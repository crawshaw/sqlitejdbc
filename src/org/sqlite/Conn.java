package org.sqlite;

import java.sql.*;
import java.util.*;

class Conn implements Connection
{
    private final DB db = new DB();
    private boolean open = false;

    public Conn(String filename) throws SQLException {
        db.open(filename);
        open = true;
    }

    private void checkCursor(int rst, int rsc, int rsh) throws SQLException {
        if (rst != ResultSet.TYPE_FORWARD_ONLY) throw new SQLException(
            "SQLite only supports TYPE_FORWARD_ONLY cursors");
        if (rsc != ResultSet.CONCUR_READ_ONLY) throw new SQLException(
            "SQLite only supports CONCUR_READ_ONLY cursors");
        if (rsh != ResultSet.CLOSE_CURSORS_AT_COMMIT) throw new SQLException(
            "SQLite only supports closing cursors at commit");
    }

    public void clearWarnings() throws SQLException { throw new SQLException("NYI");}
    public void close() throws SQLException {
        if (open) { db.close(); open = false; }
    }
    public void commit() throws SQLException { throw new SQLException("NYI");}

    public String getCatalog() throws SQLException { throw new SQLException("NYI");}
    public void setCatalog(String catalog) throws SQLException { throw new SQLException("NYI");}
    public int getHoldability() throws SQLException { throw new SQLException("NYI");}
    public void setHoldability(int holdable) throws SQLException { throw new SQLException("NYI");}
    public DatabaseMetaData getMetaData() throws SQLException { throw new SQLException("NYI");}
    public int getTransactionIsolation() throws SQLException { throw new SQLException("NYI");}
    public void setTransactionIsolation(int level) throws SQLException { throw new SQLException("NYI");}
    public Map getTypeMap() throws SQLException { throw new SQLException("NYI");}
    public void setTypeMap(Map map) throws SQLException { throw new SQLException("NYI");}
    public SQLWarning getWarnings() throws SQLException { throw new SQLException("NYI");}
    public boolean isClosed() throws SQLException { return !open; }
    public boolean isReadOnly() throws SQLException { return false; } // FIXME
    public void setReadOnly(boolean ro) throws SQLException { throw new SQLException("NYI"); }
    public String nativeSQL(String sql) throws SQLException { return sql; }
    public boolean getAutoCommit() throws SQLException { return true; }
    public void setAutoCommit(boolean autocom) throws SQLException { throw new SQLException("NYI"); }

    public void rollback() throws SQLException { throw new SQLException("NYI"); }
    public Statement createStatement() throws SQLException {
        return createStatement(ResultSet.TYPE_FORWARD_ONLY,
                               ResultSet.CONCUR_READ_ONLY,
                               ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }
    public Statement createStatement(int rsType, int rsConcurr)
        throws SQLException { return createStatement(rsType, rsConcurr,
                                          ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }
    public Statement createStatement(int rst, int rsc, int rsh)
        throws SQLException {
        checkCursor(rst, rsc, rsh);
        return new Stmt(this, db);
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        return prepareCall(sql, ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_READ_ONLY,
                                ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }
    public CallableStatement prepareCall(String sql, int rst, int rsc)
                                throws SQLException {
        return prepareCall(sql, rst, rsc, ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }
    public CallableStatement prepareCall(String sql, int rst, int rsc, int rsh)
                                throws SQLException {
        throw new SQLException("NYI");
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY,
                                     ResultSet.CONCUR_READ_ONLY);
    }
    public PreparedStatement prepareStatement(String sql, int autoC)
        throws SQLException { throw new SQLException("NYI"); }
    public PreparedStatement prepareStatement(String sql, int[] colInds)
        throws SQLException { throw new SQLException("NYI"); }
    public PreparedStatement prepareStatement(String sql, String[] colNames)
        throws SQLException { throw new SQLException("NYI"); }
    public PreparedStatement prepareStatement(String sql, int rst, int rsc) 
                                throws SQLException {
        return prepareStatement(sql, rst, rsc,
                                ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }

    public PreparedStatement prepareStatement(String sql, int rst, int rsc,
                                int rsh) throws SQLException {
        checkCursor(rst, rsc, rsh);
        return new PrepStmt(this, db, sql);
    }


    // UNUSED FUNCTIONS /////////////////////////////////////////////

    public Savepoint setSavepoint() throws SQLException {
        throw new SQLException("unsupported by SQLite: savepoints"); }
    public Savepoint setSavepoint(String name) throws SQLException {
        throw new SQLException("unsupported by SQLite: savepoints"); }
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        throw new SQLException("unsupported by SQLite: savepoints"); }
    public void rollback(Savepoint savepoint) throws SQLException {
        throw new SQLException("unsupported by SQLite: savepoints"); }

}
