package org.sqlite;

import java.sql.SQLException;

/** This class provides a thin JNI layer over the SQLite3 C API.  */
class DB implements Codes
{

    /** database pointer */
    long pointer = 0;

    static {
        System.loadLibrary("sqlitejdbc");
        init();
    }

    private static native void init();


    // WRAPPER FUNCTIONS ////////////////////////////////////////////

    native void open(String filename) throws SQLException;
    native void close() throws SQLException;
    native void interrupt();
    native void exec(String sql) throws SQLException;
    native long prepare(String sql) throws SQLException;
    native String errmsg();

    native int changes(long stmt);
    native int finalize(long stmt);
    native int step(long stmt);
    native int reset(long stmt);
    native int clear_bindings(long stmt);

    native int bind_null  (long stmt, int pos);
    native int bind_text  (long stmt, int pos, String value);
    native int bind_blob  (long stmt, int pos, byte[] value);
    native int bind_double(long stmt, int pos, double value);
    native int bind_long  (long stmt, int pos, long   value);
    native int bind_int   (long stmt, int pos, int    value);

    native int    column_count      (long stmt);
    native int    column_type       (long stmt, int col);
    native String column_decltype   (long stmt, int col);
    native String column_table_name (long stmt, int col);
    native String column_name       (long stmt, int col);
    native String column_text       (long stmt, int col);
    native byte[] column_blob       (long stmt, int col);
    native double column_double     (long stmt, int col);
    native long   column_long       (long stmt, int col);
    native int    column_int        (long stmt, int col);


    // COMPOUND FUNCTIONS (for optimisation) /////////////////////////

    native int executeUpdate(long stmt) throws SQLException;

    native String[]    column_names    (long stmt);

    /** Provides metadata for the columns of a statement. Returns:
     *   res[col][0] = true if column constrained NOT NULL
     *   res[col][1] = true if column is part of the primary key
     *   res[col][2] = true if column is auto-increment
     */
    native boolean[][] column_metadata(long stmt, String[] colNames);
    boolean[][] column_metadata(long stmt) {
        return column_metadata(stmt, column_names(stmt));
    }


    // HELPER FUNCTIONS /////////////////////////////////////////////

    void throwex() throws SQLException { throw ex(); }
    void throwex(String msg) throws SQLException { throw new SQLException(msg);}
    SQLException ex() { return new SQLException("SQLite error: " + errmsg()); }
}
