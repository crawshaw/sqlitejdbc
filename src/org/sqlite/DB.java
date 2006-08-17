/* Copyright 2006 David Crawshaw, see LICENSE file for licensing [BSD]. */
package org.sqlite;

import java.io.File;
import java.sql.SQLException;

/** This class provides a thin JNI layer over the SQLite3 C API.
 *
 *  All SQLite access is synchronized against the instance of this class,
 *  so a synchronized (db) { ... } block guarantees no-one else can talk
 *  to the database. */
class DB implements Codes
{
    /** database pointer */
    long pointer = 0;

    static {
        String libpath = System.getProperty("org.sqlite.lib.path");
        String libname = System.getProperty("org.sqlite.lib.name");
        if (libname == null) libname = System.mapLibraryName("sqlitejdbc");

        if (libpath != null) {
            try {
                System.load(new File(libpath, libname).getAbsolutePath());
            } catch (UnsatisfiedLinkError e) {
                throw new RuntimeException(
                    "property org.sqlite.lib.path set but does not reference"
                    + " a path containing the SQLiteJDBC native library", e);
            }
        } else System.loadLibrary("sqlitejdbc");
    }


    // WRAPPER FUNCTIONS ////////////////////////////////////////////

    native synchronized void open(String filename) throws SQLException;
    native synchronized void close() throws SQLException;
    native synchronized void interrupt();
    native synchronized void busy_timeout(int ms);
    native synchronized void exec(String sql) throws SQLException;
    native synchronized long prepare(String sql) throws SQLException;
    native synchronized String errmsg();
    native synchronized String libversion();

    native synchronized int changes(long stmt);
    native synchronized int finalize(long stmt);
    native synchronized int step(long stmt);
    native synchronized int reset(long stmt);
    native synchronized int clear_bindings(long stmt);

    native synchronized int bind_parameter_count(long stmt);
    native synchronized int bind_null  (long stmt, int pos);
    native synchronized int bind_text  (long stmt, int pos, String value);
    native synchronized int bind_blob  (long stmt, int pos, byte[] value);
    native synchronized int bind_double(long stmt, int pos, double value);
    native synchronized int bind_long  (long stmt, int pos, long   value);
    native synchronized int bind_int   (long stmt, int pos, int    value);

    native synchronized int    column_count      (long stmt);
    native synchronized int    column_type       (long stmt, int col);
    native synchronized String column_decltype   (long stmt, int col);
    native synchronized String column_table_name (long stmt, int col);
    native synchronized String column_name       (long stmt, int col);
    native synchronized String column_text       (long stmt, int col);
    native synchronized byte[] column_blob       (long stmt, int col);
    native synchronized double column_double     (long stmt, int col);
    native synchronized long   column_long       (long stmt, int col);
    native synchronized int    column_int        (long stmt, int col);


    // COMPOUND FUNCTIONS (for optimisation) /////////////////////////

    native synchronized int executeUpdate(long stmt) throws SQLException;

    native synchronized String[] column_names(long stmt);

    /** Provides metadata for the columns of a statement. Returns:
     *   res[col][0] = true if column constrained NOT NULL
     *   res[col][1] = true if column is part of the primary key
     *   res[col][2] = true if column is auto-increment
     */
    native synchronized boolean[][] column_metadata(long stmt, String[] names);
    synchronized boolean[][] column_metadata(long stmt) {
        return column_metadata(stmt, column_names(stmt));
    }


    // HELPER FUNCTIONS /////////////////////////////////////////////

    void throwex() throws SQLException { throw ex(); }
    void throwex(String msg) throws SQLException { throw new SQLException(msg);}
    SQLException ex() { return new SQLException("SQLite error: " + errmsg()); }
}
