package org.sqlite;

import java.sql.*;

class Stmt implements Statement, Codes
{
    protected Conn conn;
    protected DB db;

    protected boolean results = false;
    protected RS rs = null;

    long pointer = 0;
    int  maxRows = 0;
    int  timeout = 0;

    Stmt(Conn conn, DB db) {
        this.conn = conn;
        this.db = db;
    }

    protected void checkOpen() throws SQLException {
        if (pointer == 0) throw new SQLException("statement is closed");
    }

    public void cancel() throws SQLException { checkOpen(); db.interrupt(); }

    public Connection getConnection() throws SQLException { return conn; }

    /** Calls SQLite finalize() on current pointer and clears it. */
    private void finalizeStmt() throws SQLException {
        if (pointer == 0) return;
        int resp = db.finalize(pointer);
        if (resp != SQLITE_OK && resp != SQLITE_MISUSE) throw db.ex();
        pointer = 0;
    }

    /** Removes all connection references and makes Statement unusable. */
    public void close() throws SQLException {
        if (pointer == 0) return; // API says function returns if already closed
        if (rs != null) rs.clear();
        finalizeStmt();
        conn = null;
        db = null;
    }



    public void addBatch(String sql) throws SQLException { throw new SQLException("NYI");}
    public void clearBatch() throws SQLException { throw new SQLException("NYI");}
    public void clearWarnings() throws SQLException { throw new SQLException("NYI");}

    public ResultSet getGeneratedKeys() throws SQLException { throw new SQLException("NYI");}
    public boolean getMoreResults() throws SQLException { throw new SQLException("NYI");}
    public boolean getMoreResults(int current) throws SQLException { throw new SQLException("NYI");}
    public int getResultSetConcurrency() throws SQLException { throw new SQLException("NYI");}


    public int getResultSetHoldability() throws SQLException {
        return ResultSet.CLOSE_CURSORS_AT_COMMIT; }
    public int getResultSetType() throws SQLException {
        return ResultSet.TYPE_FORWARD_ONLY; }

    public int getFetchDirection() { return ResultSet.FETCH_FORWARD; }
    public void setFetchDirection(int d) throws SQLException {
        if (d != ResultSet.FETCH_FORWARD)
            throw new SQLException("only FETCH_FORWARD direction supported");
    }

    public int getQueryTimeout() { return timeout; }
    public void setQueryTimeout(int seconds) throws SQLException {
        if (seconds < 0) throw new SQLException("query timeout must be >= 0");
        timeout = seconds;
    }

    public int getMaxRows() { return maxRows; }
    public void setMaxRows(int max) throws SQLException {
        if (max < 0) throw new SQLException("max row count must be >= 0");
        maxRows = max;
    }

    /** Overridden by PrepStmt to reduce RS instantiation work. */
    protected RS createResultSet() throws SQLException {
        return new RS(this);
    }

    public ResultSet getResultSet() throws SQLException {
        checkOpen();
        if (rs != null) throw new SQLException("ResultSet already requested");
        if (db.column_count(pointer) == 0) throw new SQLException(
            "no ResultSet available");
        rs = createResultSet();
        if (results) results = false; else rs.isAfterLast = true;
        return rs;
    }
    public int getUpdateCount() throws SQLException {
        if (pointer == 0 || results) return -1;
        return db.changes(pointer);
    }

    protected boolean exec() throws SQLException {
        if (rs != null) throw new SQLException(
                "SQLite JDBC internal consistency error: rs != null on exec");
        results = false;
        switch (db.step(pointer)) {
            case SQLITE_DONE:   break;
            case SQLITE_ROW:    results = true; break;
            case SQLITE_BUSY:   throw new SQLException("db locked");//FIXME loop?
            case SQLITE_MISUSE: throw new SQLException("cannot execute, misuse");
            case SQLITE_ERROR:
            default:
                throw db.ex();
        }

        return db.column_count(pointer) != 0;
    }

    public boolean execute(String sql) throws SQLException {
        pointer = db.prepare(sql); return exec();
    }
    public ResultSet executeQuery(String sql) throws SQLException {
        pointer = db.prepare(sql);
        if (!exec()) {
            finalizeStmt();
            throw new SQLException("query does not return ResultSet");
        }
        return getResultSet();
    }
    public int executeUpdate(String sql) throws SQLException {
        // FIXME: check current state
        pointer = db.prepare(sql);
        int changes = 0;
        try { changes = db.executeUpdate(pointer); } finally { pointer = 0; }
        return changes;
    }


    // TODO
    public void setEscapeProcessing(boolean enable) throws SQLException {
        throw new SQLException("NYI"); }

    public int getMaxFieldSize() throws SQLException {
        throw new SQLException("NYI"); }
    public void setMaxFieldSize(int max) throws SQLException {
        throw new SQLException("NYI"); }

    public int getFetchSize() throws SQLException {
        throw new SQLException("NYI"); }
    public void setFetchSize(int rows) throws SQLException {
        throw new SQLException("NYI"); }

    public int[] executeBatch() throws SQLException {
        throw new SQLException("NYI");}
    public boolean execute(String sql, int[] colinds) throws SQLException {
        throw new SQLException("NYI: auto-generated keys"); }
    public boolean execute(String sql, String[] colnames) throws SQLException {
        throw new SQLException("NYI: auto-generated keys"); }
    public int executeUpdate(String sql, int autoKeys) throws SQLException {
        throw new SQLException("NYI: auto-generated keys"); }
    public int executeUpdate(String sql, int[] colinds) throws SQLException {
        throw new SQLException("NYI: auto-generated keys"); }
    public int executeUpdate(String sql, String[] cols) throws SQLException {
        throw new SQLException("NYI: auto-generated keys"); }
    public boolean execute(String sql, int autokeys)throws SQLException {
        if (autokeys != Statement.NO_GENERATED_KEYS) throw new SQLException(
            "not yet implemented: returning auto-generated keys"); // FIXME
        return execute(sql);
    }


    // UNUSED FUNCTIONS /////////////////////////////////////////////

    public SQLWarning getWarnings() throws SQLException { return null; }
    public void setCursorName(String name) throws SQLException {}
}
