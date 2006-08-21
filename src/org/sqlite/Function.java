package org.sqlite;

import java.sql.*;

public abstract class Function
{
    private Conn conn;
    private DB db;

    long context = 0;     // pointer sqlite3_context*
    long value = 0;       // pointer sqlite3_value*, first of 'args' array
    int args = 0;

    public static final void create(Connection conn, String name, Function f)
            throws SQLException {
        if (conn == null || !(conn instanceof Conn))
            throw new SQLException("connection must be to an SQLite db");
        f.conn = (Conn)conn;
        f.db = f.conn.db();

        if (name == null || name.length() > 255)
            throw new SQLException("");

        f.db.create_function(name, f);
    }

    public static final void destroy(Connection conn, String name)
            throws SQLException {
        if (conn == null || !(conn instanceof Conn))
            throw new SQLException("connection must be to an SQLite db");
        ((Conn)conn).db().destroy_function(name);
    }


    protected abstract void xFunc() throws SQLException;


    protected synchronized final int args() { return args; }

    protected synchronized final void result(byte[] val) throws SQLException {
        db.result_blob(context, val); }
    protected synchronized final void result(double v) throws SQLException {
        db.result_double(context, v); }
    protected synchronized final void result(int value) throws SQLException {
        db.result_int(context, value); }
    protected synchronized final void result(long val) throws SQLException {
        db.result_long(context, val); }
    protected synchronized final void result() throws SQLException {
        db.result_null(context); }
    protected synchronized final void result(String val) throws SQLException {
        db.result_text(context, val); }

    protected synchronized final void error(String er) throws SQLException {
        db.result_error(context, er); }

    protected synchronized final int value_bytes(int arg) throws SQLException {
        checkValue(arg); return db.value_bytes(this, arg); }
    protected synchronized final String value_text(int arg) throws SQLException{
        checkValue(arg); return db.value_text(this, arg); }
    protected synchronized final byte[] value_blob(int a) throws SQLException {
        checkValue(a); return db.value_blob(this, a); }
    protected synchronized final double value_double(int a) throws SQLException{
        checkValue(a); return db.value_double(this, a); }
    protected synchronized final int value_int(int arg) throws SQLException {
        checkValue(arg); return db.value_int(this, arg); }
    protected synchronized final long value_long(int arg) throws SQLException {
        checkValue(arg); return db.value_long(this, arg); }
    protected synchronized final int value_type(int arg) throws SQLException {
        checkValue(arg); return db.value_type(this, arg); }

    private void checkValue(int arg) throws SQLException {
        if (conn == null || conn.isClosed() || value == 0)
            throw new SQLException("not in value access state");
        if (arg >= args)
            throw new SQLException("arg "+arg+" out of bounds [0,"+args+")");
    }


    /*public static abstract class Aggregate extends Function
    {
        protected abstract void xStep();
        protected abstract void xFinal();
    }*/
}
