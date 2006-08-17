/* Copyright 2006 David Crawshaw, see LICENSE file for licensing [BSD]. */
package org.sqlite;

import java.sql.*;
import java.util.*;
import java.lang.ref.WeakReference;

class Conn implements Connection
{
    private DB db = new DB();
    private Set stmts = Collections.synchronizedSet(new HashSet());
    private MetaData meta = null;
    private boolean autoCommit = true;
    private int timeout = 0;

    public Conn(String filename) throws SQLException {
        db.open(filename);
        setTimeout(3000);
    }

    int getTimeout() { return timeout; }
    void setTimeout(int ms) {
        timeout = ms;
        db.busy_timeout(ms);
    }
    String libversion() { return db.libversion(); }

    private void checkOpen() throws SQLException {
        if (db == null)  throw new SQLException("database connection closed");
    }

    private void checkCursor(int rst, int rsc, int rsh) throws SQLException {
        if (rst != ResultSet.TYPE_FORWARD_ONLY) throw new SQLException(
            "SQLite only supports TYPE_FORWARD_ONLY cursors");
        if (rsc != ResultSet.CONCUR_READ_ONLY) throw new SQLException(
            "SQLite only supports CONCUR_READ_ONLY cursors");
        if (rsh != ResultSet.CLOSE_CURSORS_AT_COMMIT) throw new SQLException(
            "SQLite only supports closing cursors at commit");
    }

    void remove(Stmt stmt) { stmts.remove(stmt); }

    public void finalize() throws SQLException { close(); }
    public void close() throws SQLException {
        if (db == null) return;

        // must be  synchronized against both db and stmts to ensure
        // no database access occurs or a new statement is created
        // while the Connection is being closed
        synchronized (db) { synchronized (stmts) {
            for (Iterator i = stmts.iterator(); i.hasNext(); i.remove()) {
                Stmt s = (Stmt)(((WeakReference)i.next()).get());
                if (s == null) continue;
                s.close();
                if (s.pointer != 0) {
                    db.finalize(s.pointer);
                    s.pointer = 0;
                }
                s.db = null;
            }
            db.close();
            db = null;
        } }
    }

    public boolean isClosed() throws SQLException { return db == null; }

    public String getCatalog() throws SQLException { checkOpen(); return null; }
    public void setCatalog(String catalog) throws SQLException { checkOpen(); }

    public int getHoldability() throws SQLException {
        checkOpen();  return ResultSet.CLOSE_CURSORS_AT_COMMIT; }
    public void setHoldability(int h) throws SQLException {
        checkOpen();
        if (h != ResultSet.CLOSE_CURSORS_AT_COMMIT) throw new SQLException(
            "SQLite only supports CLOSE_CURSORS_AT_COMMIT");
    }

    public int getTransactionIsolation() { return TRANSACTION_SERIALIZABLE; }
    public void setTransactionIsolation(int level) throws SQLException {
        if (level != TRANSACTION_SERIALIZABLE) throw new SQLException(
            "SQLite supports only TRANSACTION_SERIALIZABLE");
    }

    public Map getTypeMap() throws SQLException
        { throw new SQLException("not yet implemented");}
    public void setTypeMap(Map map) throws SQLException
        { throw new SQLException("not yet implemented");}

    public boolean isReadOnly() throws SQLException { return false; } // FIXME
    public void setReadOnly(boolean ro) throws SQLException
        { throw new SQLException("not yet implemented"); }

    public DatabaseMetaData getMetaData() {
        if (meta == null) meta = new MetaData(this);
        return meta;
    }

    public String nativeSQL(String sql) { return sql; }

    public void clearWarnings() throws SQLException { }
    public SQLWarning getWarnings() throws SQLException { return null; }

    // TODO: optimise with direct jni calls for begin/commit/rollback
    public boolean getAutoCommit() throws SQLException {
        checkOpen(); return autoCommit; }
    public void setAutoCommit(boolean ac) throws SQLException {
        checkOpen();
        if (autoCommit == ac) return;
        autoCommit = ac;
        db.exec(autoCommit ? "COMMIT;" : "BEGIN DEFERRED;");
    }

    public void commit() throws SQLException {
        checkOpen();
        if (autoCommit) throw new SQLException("database in auto-commit mode");
        db.exec("COMMIT;");
        db.exec("BEGIN DEFERRED;");
    }

    public void rollback() throws SQLException {
        checkOpen();
        if (autoCommit) throw new SQLException("database in auto-commit mode");
        db.exec("ROLLBACK;");
        db.exec("BEGIN DEFERRED;");
    }

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
        Stmt s = new Stmt(this, db);
        stmts.add(new WeakReference(s));
        return s;
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
        throw new SQLException("SQLite does not support Stored Procedures");
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

    public PreparedStatement prepareStatement(
            String sql, int rst, int rsc, int rsh) throws SQLException {
        checkCursor(rst, rsc, rsh);
        PrepStmt prep = new PrepStmt(this, db, sql);
        stmts.add(new WeakReference(prep));
        return prep;
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
