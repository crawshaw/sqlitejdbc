package org.sqlite;

import java.sql.*;

/** Provides an interface for creating SQLite user-defined functions.
 *
 * <p>A subclass of <tt>org.sqlite.Function</tt> can be registered with
 * <tt>Function.create()</tt> and called by the name it was given. All
 * functions must implement <tt>xFunc()</tt>, which is called when SQLite
 * runs the custom function.</p>
 *
 * Eg.
 *
 * <pre>
 *      Class.forName("org.sqlite.JDBC");
 *      Connection conn = DriverManager.getConnection("jdbc:sqlite:");
 *
 *      Function.create(conn, "myFunc", new Function() {
 *          protected synchronized void xFunc() {
 *              System.out.println("myFunc called!");
 *          }
 *      });
 *
 *      conn.createStatement().execute("select myFunc();");
 *  </pre>
 *
 *  <p>Arguments passed to a custom function can be accessed using the
 *  <tt>protected</tt> functions provided. <tt>args()</tt> returns
 *  the number of arguments passed, while
 *  <tt>value_&lt;type&gt;(int)</tt> returns the value of the specific
 *  argument. Similarly a function can return a value using the
 *  <tt>result(&lt;type&gt;)</tt> function.</p>
 *
 *  <p>Aggregate functions are not yet supported, but coming soon.</p>
 *
 */
public abstract class Function
{
    private Conn conn;
    private DB db;

    long context = 0;     // pointer sqlite3_context*
    long value = 0;       // pointer sqlite3_value*, first of 'args' array
    int args = 0;

    /** Registers the given function with the Connection using the
     *  provided name. */
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

    /** Removes the named function form the Connection. */
    public static final void destroy(Connection conn, String name)
            throws SQLException {
        if (conn == null || !(conn instanceof Conn))
            throw new SQLException("connection must be to an SQLite db");
        ((Conn)conn).db().destroy_function(name);
    }


    /** Called by the JDBC driver when the custom function is called
     *  by SQLite. */
    protected abstract void xFunc() throws SQLException;


    /** Returns the number of arguments passed to the function.
     *  Can only be called from <tt>xFunc()</tt>. */
    protected synchronized final int args() { return args; }

    /** Called by <tt>xFunc</tt> to return a value. */
    protected synchronized final void result(byte[] value) throws SQLException {
        db.result_blob(context, value); }

    /** Called by <tt>xFunc</tt> to return a value. */
    protected synchronized final void result(double value) throws SQLException {
        db.result_double(context, value); }

    /** Called by <tt>xFunc</tt> to return a value. */
    protected synchronized final void result(int value) throws SQLException {
        db.result_int(context, value); }

    /** Called by <tt>xFunc</tt> to return a value. */
    protected synchronized final void result(long value) throws SQLException {
        db.result_long(context, value); }

    /** Called by <tt>xFunc</tt> to return a value. */
    protected synchronized final void result() throws SQLException {
        db.result_null(context); }

    /** Called by <tt>xFunc</tt> to return a value. */
    protected synchronized final void result(String value) throws SQLException {
        db.result_text(context, value); }

    /** Called by <tt>xFunc</tt> to throw an error. */
    protected synchronized final void error(String er) throws SQLException {
        db.result_error(context, er); }

    /** Called by <tt>xFunc</tt> to access the value of an argument. */
    protected synchronized final int value_bytes(int arg) throws SQLException {
        checkValue(arg); return db.value_bytes(this, arg); }

    /** Called by <tt>xFunc</tt> to access the value of an argument. */
    protected synchronized final String value_text(int arg) throws SQLException{
        checkValue(arg); return db.value_text(this, arg); }

    /** Called by <tt>xFunc</tt> to access the value of an argument. */
    protected synchronized final byte[] value_blob(int a) throws SQLException {
        checkValue(a); return db.value_blob(this, a); }

    /** Called by <tt>xFunc</tt> to access the value of an argument. */
    protected synchronized final double value_double(int a) throws SQLException{
        checkValue(a); return db.value_double(this, a); }

    /** Called by <tt>xFunc</tt> to access the value of an argument. */
    protected synchronized final int value_int(int arg) throws SQLException {
        checkValue(arg); return db.value_int(this, arg); }

    /** Called by <tt>xFunc</tt> to access the value of an argument. */
    protected synchronized final long value_long(int arg) throws SQLException {
        checkValue(arg); return db.value_long(this, arg); }

    /** Called by <tt>xFunc</tt> to access the value of an argument. */
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
