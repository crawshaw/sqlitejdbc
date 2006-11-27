/* Copyright 2006 David Crawshaw, see LICENSE file for licensing [BSD]. */
package org.sqlite;

import org.ibex.nestedvm.Runtime;

import java.io.File;
import java.io.PrintWriter;
import java.sql.*;

// FEATURE: strdup is wasteful, SQLite interface will take unterminated char*

/** Communicates with the Java version of SQLite provided by NestedVM. */
final class NestedDB extends DB
{
    /** database pointer */
    int handle = 0;

    /** sqlite binary embedded in nestedvm */
    final Runtime rt;

    {
        Runtime run = null;
        try {
            run = (Runtime)Class.forName("org.sqlite.SQLite").newInstance();
            run.start();
        } catch (Exception e) {
            System.err.println("SQLiteJDBC load ERROR");
            e.printStackTrace();
        } finally {
            rt = run;
        }
    }


    // WRAPPER FUNCTIONS ////////////////////////////////////////////

    synchronized void open(String filename) throws SQLException {
        if (handle != 0) throw new SQLException("DB already open");
        int passback = rt.xmalloc(1);
        int str = rt.strdup(filename);
        int ret = call("sqlite3_open", str, passback);
        if (ret != 0) {
            System.out.println("ERR on open"); throwex(); // TODO
        }
        handle = deref(passback);
        rt.free(str);
        rt.free(passback);
    }
    synchronized void close() throws SQLException {
        if (handle == 0) return;
        if (call("sqlite3_close", handle) != SQLITE_OK) {
            handle = 0;
            throwex();
        }
        handle = 0;
    }
    synchronized void interrupt() throws SQLException {
        call("sqlite3_interrupt", handle);
    }
    synchronized void busy_timeout(int ms) throws SQLException {
        call("sqlite3_busy_timeout", handle, ms);
    }
    synchronized void exec(String sql) throws SQLException {
        int pointer = (int)prepare(sql);
        try { execute(pointer, null); } finally { finalize(pointer); }
    }
    // TODO: store sqlite3_stmt against PhantomReference for org.sqlite.RS
    synchronized long prepare(String sql) throws SQLException {
        int passback = rt.xmalloc(1);
        int str = rt.strdup(sql);
        int ret = call("sqlite3_prepare", handle, str, -1, passback, 0);
        rt.free(str);
        if (ret != SQLITE_OK) {
            rt.free(passback);
            throwex();
        }
        int pointer = deref(passback);
        rt.free(passback);
        return pointer;
    }
    synchronized String errmsg() throws SQLException {
        return cstring(call("sqlite3_errmsg", handle)); }
    synchronized String libversion() throws SQLException {
        return cstring(call("sqlite3_libversion", handle)); }
    synchronized int changes() throws SQLException {
        return call("sqlite3_changes", handle); }

    // TODO: clear sqlite3_stmt from PhantomReference
    synchronized int finalize(long stmt) throws SQLException {
        return call("sqlite3_finalize", (int)stmt); }
    synchronized int step(long stmt) throws SQLException {
        int rc = call("sqlite3_step", (int)stmt);
        if (rc == SQLITE_ERROR)
            rc = reset(stmt);
        return rc;
    }
    synchronized int reset(long stmt) throws SQLException {
        return call("sqlite3_reset", (int)stmt); }
    synchronized int clear_bindings(long stmt) throws SQLException {
        return call("sqlite3_clear_bindings", (int)stmt); }

    synchronized int bind_parameter_count(long stmt) throws SQLException {
        return call("sqlite3_bind_parameter_count", (int)stmt); }

    synchronized int column_count(long stmt) throws SQLException {
        return call("sqlite3_column_count", (int)stmt); }
    synchronized int column_type(long stmt, int col) throws SQLException {
        return call("sqlite3_column_type", (int)stmt, col); }
    synchronized String column_name(long stmt, int col) throws SQLException {
        return utfstring(call("sqlite3_column_name", (int)stmt, col)); }
    synchronized String column_text(long stmt, int col) throws SQLException {
        return utfstring(call("sqlite3_column_text", (int)stmt, col)); }
    synchronized byte[] column_blob(long stmt, int col) throws SQLException {
        byte[] blob = new byte[call("sqlite3_column_bytes", (int)stmt, col)];
        int addr = call("sqlite3_column_blob", (int)stmt, col);
        copyin(addr, blob, blob.length);
        return blob;
    }
    synchronized double column_double(long stmt, int col) throws SQLException {
        try { return Double.parseDouble(column_text(stmt, col)); }
        catch (NumberFormatException e) { return Double.NaN; } // TODO
    }
    synchronized long column_long(long stmt, int col) throws SQLException {
        try { return Long.parseLong(column_text(stmt, col)); }
        catch (NumberFormatException e) { return 0; } // TODO
    }
    synchronized int column_int(long stmt, int col) throws SQLException {
        return call("sqlite3_column_int", (int)stmt, col); }
    synchronized String column_decltype(long stmt, int col)
            throws SQLException {
        return utfstring(call("sqlite3_column_decltype", (int)stmt, col)); }
    synchronized String column_table_name(long stmt, int col)
            throws SQLException {
        return utfstring(call("sqlite3_column_table_name", (int)stmt, col));
    }

    synchronized int bind_null(long stmt, int pos) throws SQLException {
        return call("sqlite3_bind_null", (int)stmt, pos);
    }
    synchronized int bind_int(long stmt, int pos, int v) throws SQLException {
        return call("sqlite3_bind_int", (int)stmt, pos, v);
    }
    synchronized int bind_long(long stmt, int pos, long v) throws SQLException {
        return bind_text(stmt, pos, Long.toString(v)); // TODO
    }
    synchronized int bind_double(long stmt, int pos, double v)
            throws SQLException {
        return bind_text(stmt, pos, Double.toString(v)); // TODO
    }
    synchronized int bind_text(long stmt, int pos, String v)
            throws SQLException {
        if (v == null) return bind_null(stmt, pos);
        return call("sqlite3_bind_text", (int)stmt, pos, rt.strdup(v),
                    rt.lookupSymbol("free"));
    }
    synchronized int bind_blob(long stmt, int pos, byte[] buf)
            throws SQLException {
        if (buf == null || buf.length < 1) return bind_null(stmt, pos);
        int len = buf.length;
        int blob = rt.xmalloc(len); // free()ed by sqlite3_bind_blob
        copyout(buf, blob, len);
        return call("sqlite3_bind_blob", (int)stmt, pos, blob, len,
                    rt.lookupSymbol("free"));
    }

    synchronized void result_null  (long cxt) throws SQLException {
        call("sqlite3_result_null", (int)cxt); }
    synchronized void result_text  (long cxt, String val) throws SQLException {
        int str = rt.strdup(val);
        call("sqlite3_result_text", (int)cxt, str);
        rt.free(str);
    }
    synchronized void result_blob  (long cxt, byte[] val) throws SQLException {
        // TODO
        call("sqlite3_result_blob", (int)cxt);
    }
    synchronized void result_double(long cxt, double val) throws SQLException {
        call("sqlite3_result_double", (int)cxt); } // TODO
    synchronized void result_long  (long cxt, long   val) throws SQLException {
        call("sqlite3_result_long", (int)cxt); } // TODO
    synchronized void result_int   (long cxt, int    val) throws SQLException {
        call("sqlite3_result_int", (int)cxt, val); }
    synchronized void result_error (long cxt, String err) throws SQLException {
        int str = rt.strdup(err);
        call("sqlite3_result_error", (int)cxt, str);
        rt.free(str);
    }

    native synchronized int    value_bytes (Function f, int arg);
    native synchronized String value_text  (Function f, int arg);
    native synchronized byte[] value_blob  (Function f, int arg);
    native synchronized double value_double(Function f, int arg);
    native synchronized long   value_long  (Function f, int arg);
    native synchronized int    value_int   (Function f, int arg);
    native synchronized int    value_type  (Function f, int arg);

    native synchronized int create_function(String name, Function func);
    native synchronized int destroy_function(String name);
    synchronized void free_functions() {}

    /** Calls support function found in upstream/sqlite-metadata.patch */
    synchronized boolean[][] column_metadata(long stmt) throws SQLException {
        int colCount = call("sqlite3_column_count", (int)stmt);
        boolean[][] meta = new boolean[colCount][3];
        int pass = rt.xmalloc(12); // struct metadata

        for (int i=0; i < colCount; i++) {
            call("column_metadata_helper", handle, (int)stmt, i, pass);
            meta[i][0] = deref(pass) == 1;
            meta[i][1] = deref(pass + 4) == 1;
            meta[i][2] = deref(pass + 8) == 1;
        }

        rt.free(pass);
        return meta;
    }


    // HELPER FUNCTIONS /////////////////////////////////////////////

    /** safe to reuse parameter arrays as all functions are syncrhonized */
    private final int[]
        p0 = new int[] {},
        p1 = new int[] { 0 },
        p2 = new int[] { 0, 0 },
        p3 = new int[] { 0, 0, 0 },
        p4 = new int[] { 0, 0, 0, 0 },
        p5 = new int[] { 0, 0, 0, 0, 0 };

    private int call(String addr, int a0) throws SQLException {
        p1[0] = a0; return call(addr, p1); }
    private int call(String addr, int a0, int a1) throws SQLException {
        p2[0] = a0; p2[1] = a1; return call(addr, p2); }
    private int call(String addr, int a0, int a1, int a2) throws SQLException {
        p3[0] = a0; p3[1] = a1; p3[2] = a2; return call(addr, p3); }
    private int call(String addr, int a0, int a1, int a2, int a3)
            throws SQLException {
        p4[0] = a0; p4[1] = a1; p4[2] = a2; p4[3] = a3;
        return call(addr, p4);
    }
    private int call(String addr, int a0, int a1, int a2, int a3, int a4)
            throws SQLException {
        p5[0] = a0; p5[1] = a1; p5[2] = a2; p5[3] = a3; p5[4] = a4;
        return call(addr, p5);
    }

    private int call(String func, int[] args) throws SQLException {
        try {
            return rt.call(func, args);
        } catch (Runtime.CallException e) { throw new CausedSQLException(e); }
    }
    private int deref(int pointer) throws SQLException {
        try { return rt.memRead(pointer); }
        catch (Runtime.ReadFaultException e) { throw new CausedSQLException(e);}
    }
    private String utfstring(int str) throws SQLException {
        try { return rt.utfstring(str); }
        catch (Runtime.ReadFaultException e) { throw new CausedSQLException(e);}
    }
    private String cstring(int str) throws SQLException {
        try { return rt.cstring(str); }
        catch (Runtime.ReadFaultException e) { throw new CausedSQLException(e);}
    }
    private void copyin(int addr, byte[] buf, int count) throws SQLException {
        try { rt.copyin(addr, buf, count); }
        catch (Runtime.ReadFaultException e) { throw new CausedSQLException(e);}
    }
    private void copyout(byte[] buf, int addr, int count) throws SQLException {
        try { rt.copyout(buf, addr, count); }
        catch (Runtime.FaultException e) { throw new CausedSQLException(e);}
    }

    /** Maps any exception onto an SQLException. */
    private static final class CausedSQLException extends SQLException {
        private final Exception cause;
        CausedSQLException(Exception e) {
            if (e == null) throw new RuntimeException("null exception cause");
            cause = e;
        }
        public Throwable getCause() { return cause; }
        public void printStackTrace() { cause.printStackTrace(); }
        public void printStackTrace(PrintWriter s) { cause.printStackTrace(s); }
        public Throwable fillInStackTrace() { return cause.fillInStackTrace(); }
        public StackTraceElement[] getStackTrace() {
            return cause.getStackTrace(); }
        public String getMessage() { return cause.getMessage(); }
    }
}
