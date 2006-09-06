/* Copyright 2006 David Crawshaw, see LICENSE file for licensing [BSD]. */
package org.sqlite;

import java.sql.*;
import java.util.ArrayList;

/** See comment in RS.java to explain the strange inheritance hierarchy. */
class Stmt extends RS implements Statement, Codes
{
    Conn conn;
    protected boolean resultsWaiting = false;
    private ArrayList batch = null;

    Stmt(Conn conn, DB db) {
        super(db);
        this.conn = conn;
    }

    /** Calls sqlite3_step() and sets up results. Expects a clean stmt. */
    protected boolean exec() throws SQLException {
        if (pointer == 0) throw new SQLException(
            "SQLite JDBC internal error: pointer == 0 on exec.");
        if (isRS()) throw new SQLException(
            "SQLite JDBC internal error: isRS() on exec.");
        resultsWaiting = false;
        switch (db.step(pointer)) {
            case SQLITE_DONE:   break;
            case SQLITE_ROW:    resultsWaiting = true; break;
            case SQLITE_BUSY:   throw new SQLException("database locked");
            case SQLITE_MISUSE:
                 throw new SQLException("JDBC internal consistency error");
            case SQLITE_ERROR:
            default:
                 int ret = db.finalize(pointer);
                 pointer = 0;
                 throw db.ex();
        }

        return db.column_count(pointer) != 0;
    }


    // PUBLIC INTERFACE /////////////////////////////////////////////

    public Connection getConnection() throws SQLException {
        checkOpen(); return conn; }

    /** More lax than JDBC spec, a Statement can be reused after close().
     *  This is to support Stmt and RS sharing a heap object. */
    public void close() throws SQLException {
        if (pointer == 0) return;
        clear();
        colsMeta = null;
        meta = null;
        batch = null;
        int resp = db.finalize(pointer);
        pointer = 0;
        if (resp != SQLITE_OK && resp != SQLITE_MISUSE) throw db.ex();
    }
    protected void finalize() throws SQLException {
        Stmt.this.close();
        if (conn != null) conn.remove(this);;
    }

    /** SQLite does not support multiple results from execute(). */
    public boolean getMoreResults() throws SQLException {
        checkOpen();
        return false;
    }
    public boolean getMoreResults(int c) throws SQLException {
        checkOpen();
        // take this chance to clean up any open ResultSet
        if (isRS() && (c == CLOSE_CURRENT_RESULT || c == CLOSE_ALL_RESULTS))
            close();
        return false;
    }

    public int getResultSetConcurrency() throws SQLException {
        checkOpen(); return ResultSet.CONCUR_READ_ONLY; }
    public int getResultSetHoldability() throws SQLException {
        checkOpen(); return ResultSet.CLOSE_CURSORS_AT_COMMIT; }
    public int getResultSetType() throws SQLException {
        checkOpen(); return ResultSet.TYPE_FORWARD_ONLY; }

    public void cancel() throws SQLException { checkExec(); db.interrupt(); }
    public int getQueryTimeout() throws SQLException {
        checkOpen(); return conn.getTimeout(); }
    public void setQueryTimeout(int seconds) throws SQLException {
        checkOpen();
        if (seconds < 0) throw new SQLException("query timeout must be >= 0");
        conn.setTimeout(1000 * seconds);
    }


    // TODO: write test
    public int getMaxRows() throws SQLException { checkOpen(); return maxRows; }
    public void setMaxRows(int max) throws SQLException {
        checkOpen();
        if (max < 0) throw new SQLException("max row count must be >= 0");
        maxRows = max;
    }

    public ResultSet getResultSet() throws SQLException {
        checkExec();
        if (isRS()) throw new SQLException("ResultSet already requested");
        if (db.column_count(pointer) == 0) throw new SQLException(
            "no ResultSet available");
        if (colsMeta == null) colsMeta = db.column_names(pointer);
        cols = colsMeta;

        isAfterLast = !resultsWaiting;
        if (resultsWaiting) resultsWaiting = false;
        return this;
    }

    public int getUpdateCount() throws SQLException {
        checkOpen();
        if (pointer == 0 || resultsWaiting) return -1;
        return db.changes(pointer);
    }

    public boolean execute(String sql) throws SQLException {
        checkOpen(); close();
        pointer = db.prepare(sql);
        return exec();
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        checkOpen(); close();
        pointer = db.prepare(sql);
        if (!exec()) {
            close();
            throw new SQLException("query does not return ResultSet");
        }
        return getResultSet();
    }

    public int executeUpdate(String sql) throws SQLException {
        checkOpen(); close();
        pointer = db.prepare(sql);
        int changes = 0;
        try { changes = db.executeUpdate(pointer); } finally { close(); }
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
        // TODO: optimise
        checkOpen(); close();
        if (batch == null) return new int[] {};

        ArrayList run = batch;
        int[] changes = new int[run.size()];
        for (int i=0; i < changes.length; i++) {
            pointer = db.prepare((String)run.get(i));
            try {
                changes[i] = db.executeUpdate(pointer);
            } catch (SQLException e) {
                throw new BatchUpdateException(
                    "batch entry " + i + ": " + e.getMessage(), changes);
            } finally { close(); }
        }

        return changes;
    }


    // Silly Functions //////////////////////////////////////////////

    public SQLWarning getWarnings() { return null; }
    public void clearWarnings() {}
    public void setCursorName(String name) {}

    public int getFetchSize() throws SQLException { return 1; }
    public void setFetchSize(int rows) throws SQLException {
        if (0 >= rows || rows > getMaxRows()) throw new SQLException(
            "fetch size "+rows+" out of range [0,"+getMaxRows());
    }

    public int getMaxFieldSize() throws SQLException { return 0; }
    public void setMaxFieldSize(int max) throws SQLException {
        if (max < 0) throw new SQLException(
            "max field size "+max+" cannot be negative");
    }
}
