/* Copyright 2006 David Crawshaw, see LICENSE file for licensing [BSD]. */
package org.sqlite;

import java.io.File;
import java.sql.*;

abstract class DB implements Codes
{
    /** database pointer */
    long pointer = 0;

    // WRAPPER FUNCTIONS ////////////////////////////////////////////

    abstract void open(String filename) throws SQLException;
    abstract void close() throws SQLException;
    abstract void interrupt() throws SQLException;
    abstract void busy_timeout(int ms) throws SQLException;
    abstract void exec(String sql) throws SQLException;
    abstract long prepare(String sql) throws SQLException;
    abstract String errmsg() throws SQLException;
    abstract String libversion() throws SQLException;
    abstract int changes() throws SQLException;

    abstract int finalize(long stmt) throws SQLException;
    abstract int step(long stmt) throws SQLException;
    abstract int reset(long stmt) throws SQLException;
    abstract int clear_bindings(long stmt) throws SQLException;

    abstract int bind_parameter_count(long stmt) throws SQLException;

    abstract int    column_count      (long stmt) throws SQLException;
    abstract int    column_type       (long stmt, int col) throws SQLException;
    abstract String column_decltype   (long stmt, int col) throws SQLException;
    abstract String column_table_name (long stmt, int col) throws SQLException;
    abstract String column_name       (long stmt, int col) throws SQLException;
    abstract String column_text       (long stmt, int col) throws SQLException;
    abstract byte[] column_blob       (long stmt, int col) throws SQLException;
    abstract double column_double     (long stmt, int col) throws SQLException;
    abstract long   column_long       (long stmt, int col) throws SQLException;
    abstract int    column_int        (long stmt, int col) throws SQLException;

    abstract int bind_null  (long stmt, int pos) throws SQLException;
    abstract int bind_int   (long stmt, int pos, int    v) throws SQLException;
    abstract int bind_long  (long stmt, int pos, long   v) throws SQLException;
    abstract int bind_double(long stmt, int pos, double v) throws SQLException;
    abstract int bind_text  (long stmt, int pos, String v) throws SQLException;
    abstract int bind_blob  (long stmt, int pos, byte[] v) throws SQLException;

    abstract void result_null  (long context) throws SQLException;
    abstract void result_text  (long context, String val) throws SQLException;
    abstract void result_blob  (long context, byte[] val) throws SQLException;
    abstract void result_double(long context, double val) throws SQLException;
    abstract void result_long  (long context, long   val) throws SQLException;
    abstract void result_int   (long context, int    val) throws SQLException;
    abstract void result_error (long context, String err) throws SQLException;

    abstract int    value_bytes (Function f, int arg) throws SQLException;
    abstract String value_text  (Function f, int arg) throws SQLException;
    abstract byte[] value_blob  (Function f, int arg) throws SQLException;
    abstract double value_double(Function f, int arg) throws SQLException;
    abstract long   value_long  (Function f, int arg) throws SQLException;
    abstract int    value_int   (Function f, int arg) throws SQLException;
    abstract int    value_type  (Function f, int arg) throws SQLException;

    abstract int create_function(String name, Function f) throws SQLException;
    abstract int destroy_function(String name) throws SQLException;
    abstract void free_functions() throws SQLException;

    /** Provides metadata for the columns of a statement. Returns:
     *   res[col][0] = true if column constrained NOT NULL
     *   res[col][1] = true if column is part of the primary key
     *   res[col][2] = true if column is auto-increment
     */
    abstract boolean[][] column_metadata(long stmt) throws SQLException;


    // COMPOUND FUNCTIONS ////////////////////////////////////////////

    final synchronized String[] column_names(long stmt) throws SQLException {
        String[] names = new String[column_count(stmt)];
        for (int i=0; i < names.length; i++)
            names[i] = column_name(stmt, i);
        return names;
    }

    final synchronized int sqlbind(long stmt, int pos, Object v)
            throws SQLException {
        pos++;
        if (v == null) {
            return bind_null(stmt, pos);
        } else if (v instanceof Integer) {
            return bind_int(stmt, pos, ((Integer)v).intValue());
        } else if (v instanceof Long) {
            return bind_long(stmt, pos, ((Long)v).longValue());
        } else if (v instanceof Double) {
            return bind_double(stmt, pos, ((Double)v).doubleValue());
        } else if (v instanceof String) {
            return bind_text(stmt, pos, (String)v);
        } else if (v instanceof byte[]) {
            return bind_blob(stmt, pos, (byte[])v);
        } else {
            throw new SQLException("unexpected param type: "+v.getClass());
        }
    }

    final int[] executeBatch(long stmt, int count, Object[] vals)
            throws SQLException {
        if (count < 1) throw new SQLException("count (" + count + ") < 1");

        final int params = bind_parameter_count(stmt);

        int rc;
        int[] changes = new int[count];

        BATCH: for (int i=0; i < count; i++) {
            reset(stmt);
            for (int j=0; j < params; j++)
                if (sqlbind(stmt, j, vals[(i * params) + j]) != SQLITE_OK)
                    throwex();

            rc = step(stmt);
            // TODO: handle SQLITE_SCHEMA
            if (rc != SQLITE_DONE) {
                reset(stmt);
                if (rc == SQLITE_ROW) throw new BatchUpdateException(
                    "batch entry "+i+": query returns results", changes);
                throwex();
            }

            changes[i] = changes();
        }

        reset(stmt);
        return changes;
    }

    final synchronized boolean execute(long stmt, Object[] vals)
            throws SQLException {
        if (vals != null) {
            final int params = bind_parameter_count(stmt);
            if (params != vals.length)
                throw new SQLException("assertion failure: param count ("
                        + params + ") != value count (" + vals.length + ")");

            for (int i=0; i < params; i++)
                if (sqlbind(stmt, i, vals[i]) != SQLITE_OK) throwex();
        }

        int rc = step(stmt);
        if (rc == SQLITE_ERROR)
            rc = reset(stmt);

        switch (rc) {
            case SQLITE_DONE: return false;
            case SQLITE_ROW: return true;
            case SQLITE_BUSY: throw new SQLException("database locked");
            case SQLITE_MISUSE:
                throw new SQLException("jdbc internal consistency error");
            case SQLITE_SCHEMA: // TODO
                /*sqlite3_transfer_bindings(dbstmt, newdbstmt);
                return Java_org_sqlite_DB_execute(
                        env, this, fromref(newdbstmt), values);*/
            case SQLITE_INTERNAL: // TODO: be specific
            case SQLITE_PERM: case SQLITE_ABORT: case SQLITE_NOMEM:
            case SQLITE_READONLY: case SQLITE_INTERRUPT: case SQLITE_IOERR:
            case SQLITE_CORRUPT: case SQLITE_ERROR:
            default:
                throwex();
                return false;
        }
    }

    final synchronized int executeUpdate(long stmt, Object[] vals)
            throws SQLException {
        int changes = 0;
        if (execute(stmt, vals))
            throw new SQLException("query returns results");
        reset(stmt);
        return changes();
    }


    // HELPER FUNCTIONS /////////////////////////////////////////////

    final void throwex() throws SQLException {
        throw new SQLException(errmsg());
    }
}
