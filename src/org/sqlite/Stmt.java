package org.sqlite;

import java.sql.*;
import java.util.ArrayList;

class Stmt implements Statement, Codes
{
    protected Conn conn;
    protected DB db;

    private ArrayList batch = null;

    protected boolean results = false;
    protected RS rs = null;

    long pointer = 0;
    int  maxRows = 0;
    int  timeout = 0;

    Stmt(Conn conn, DB db) {
        this.conn = conn;
        this.db = db;
    }


    // INTERNAL FUNCTIONS ///////////////////////////////////////////

    protected final void checkOpen() throws SQLException {
        if (db == null) throw new SQLException("statement is closed");
    }
    protected final void checkExec() throws SQLException {
        if (pointer == 0) throw new SQLException("statement is not executing");
    }

    /** Calls sqlite3_step() and sets up results. Expects a clean stmt. */
    protected boolean exec() throws SQLException {
        if (pointer == 0) throw new SQLException(
            "SQLite JDBC internal error: pointer == 0 on exec.");
        if (rs != null) throw new SQLException(
            "SQLite JDBC internal error: rs != null on exec.");
        results = false;
        switch (db.step(pointer)) {
            case SQLITE_DONE:   break;
            case SQLITE_ROW:    results = true; break;
            case SQLITE_BUSY:   throw new SQLException("db locked");//FIXME
            case SQLITE_MISUSE: throw new SQLException("internal misuse");
            case SQLITE_ERROR:
            default:
                throw db.ex();
        }

        return db.column_count(pointer) != 0;
    }

    /** Overridden by PrepStmt to reduce RS instantiation work. */
    protected RS createResultSet() throws SQLException { return new RS(this); }

    /** Calls SQLite finalize() on current pointer and clears it. */
    private void finalizeStmt() throws SQLException {
        if (pointer == 0) return;
        int resp = db.finalize(pointer);
        if (resp != SQLITE_OK && resp != SQLITE_MISUSE) throw db.ex();
        pointer = 0;
    }


    // PUBLIC INTERFACE /////////////////////////////////////////////

    public Connection getConnection() throws SQLException {
        checkOpen(); return conn; }

    /** Removes all connection references and makes Statement unusable. */
    public void close() throws SQLException {
        if (db == null) return; // API says function returns if already closed
        if (rs != null) rs.clear();
        finalizeStmt();
        conn = null;
        db = null;
        batch = null;
    }

    /** SQLite does not support multiple results from execute(). */
    public boolean getMoreResults() throws SQLException {
        checkOpen();
        return false;
    }
    public boolean getMoreResults(int c) throws SQLException {
        checkOpen();
        // take this chance to clean up any open ResultSet
        if (rs != null && (c == CLOSE_CURRENT_RESULT || c == CLOSE_ALL_RESULTS))
            finalizeStmt();
        return false;
    }

    public int getResultSetConcurrency() throws SQLException {
        checkOpen(); return ResultSet.CONCUR_READ_ONLY; }
    public int getResultSetHoldability() throws SQLException {
        checkOpen(); return ResultSet.CLOSE_CURSORS_AT_COMMIT; }
    public int getResultSetType() throws SQLException {
        checkOpen(); return ResultSet.TYPE_FORWARD_ONLY; }

    public int getFetchDirection() throws SQLException {
        checkOpen(); return ResultSet.FETCH_FORWARD; }
    public void setFetchDirection(int d) throws SQLException {
        checkOpen();
        if (d != ResultSet.FETCH_FORWARD)
            throw new SQLException("only FETCH_FORWARD direction supported");
    }

    // FIXME: use sqlite3_progress_handler
    public void cancel() throws SQLException { checkExec(); db.interrupt(); }
    public int getQueryTimeout() throws SQLException {
        checkOpen(); return timeout; }
    public void setQueryTimeout(int seconds) throws SQLException {
        checkOpen();
        if (seconds < 0) throw new SQLException("query timeout must be >= 0");
        timeout = seconds;
    }


    // TODO: write test
    public int getMaxRows() throws SQLException { checkOpen(); return maxRows; }
    public void setMaxRows(int max) throws SQLException {
        checkOpen();
        if (max < 0) throw new SQLException("max row count must be >= 0");
        maxRows = max;
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
        checkOpen();
        if (pointer == 0 || results) return -1;
        return db.changes(pointer);
    }

    public boolean execute(String sql) throws SQLException {
        checkOpen(); finalizeStmt();
        pointer = db.prepare(sql); return exec();
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        checkOpen(); finalizeStmt();
        pointer = db.prepare(sql);
        if (!exec()) {
            finalizeStmt();
            throw new SQLException("query does not return ResultSet");
        }
        return getResultSet();
    }

    public int executeUpdate(String sql) throws SQLException {
        checkOpen(); finalizeStmt();
        pointer = db.prepare(sql);
        int changes = 0;
        try { changes = db.executeUpdate(pointer); } finally { pointer = 0; }
        return changes;
    }

    public void addBatch(String sql) throws SQLException {
        checkOpen();
        if (batch == null) batch = new ArrayList();
        batch.add(sql);
    }

    public void clearBatch() throws SQLException {
        checkOpen(); if (batch != null) batch.clear(); }

    public int[] executeBatch() throws SQLException {
        // FIXME: optimise
        checkOpen(); finalizeStmt();
        if (batch == null) return new int[] {};
        int[] changes = new int[batch.size()];
        for (int i=0; i < changes.length; i++) {
            pointer = db.prepare((String)batch.get(i));
            try {
                changes[i] = db.executeUpdate(pointer);
            } catch (SQLException e) {
                throw new BatchUpdateException(
                    "batch entry " + i + ": " + e.getMessage(), changes);
            } finally { pointer = 0; }
        }

        return changes;
    }


    // TODO /////////////////////////////////////////////////////////

    public int getMaxFieldSize() throws SQLException {
        throw new SQLException("NYI"); }
    public void setMaxFieldSize(int max) throws SQLException {
        throw new SQLException("NYI"); }

    public int getFetchSize() throws SQLException {
        throw new SQLException("NYI"); }
    public void setFetchSize(int rows) throws SQLException {
        throw new SQLException("NYI"); }

    public ResultSet getGeneratedKeys() throws SQLException {
        throw new SQLException("NYI");} // FIXME: stupid, return empty RS

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
    public void clearWarnings() throws SQLException { }
    public void setCursorName(String name) throws SQLException {}
    public void setEscapeProcessing(boolean enable) throws SQLException { }
}
