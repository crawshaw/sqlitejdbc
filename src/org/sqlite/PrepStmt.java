/* Copyright 2006 David Crawshaw, see LICENSE file for licensing [BSD]. */
package org.sqlite;

import java.io.Reader;
import java.io.InputStream;
import java.net.URL;
import java.sql.*;
import java.math.BigDecimal;
import java.util.Calendar;

/** See comment in RS.java to explain the strange inheritance hierarchy. */
final class PrepStmt extends Stmt
        implements PreparedStatement, ParameterMetaData, Codes
{
    PrepStmt(Conn conn, DB db, String sql) throws SQLException {
        super(conn, db);

        pointer = db.prepare(sql);
        colsMeta = db.column_names(pointer);
    }

    /** Weaker close to support object overriding. */
    public void close() throws SQLException {
        if (pointer == 0) return;
        clearParameters();
    }

    public void clearParameters() throws SQLException {
        checkOpen();
        db.clear_bindings(pointer); // TODO: use return result?
        clear();
        db.reset(pointer);
    }

    public boolean execute() throws SQLException { return exec(); }
    public ResultSet executeQuery() throws SQLException {
        if (!execute()) throw new SQLException("query does not return results");
        return getResultSet();
    }
    public int executeUpdate() throws SQLException {
        return db.executeUpdate(pointer);
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        checkOpen(); return (ResultSetMetaData)this; }


    // ParameterMetaData FUNCTIONS //////////////////////////////////

    public ParameterMetaData getParameterMetaData() { return this; }

    public int getParameterCount() throws SQLException {
        checkOpen(); return db.bind_parameter_count(pointer); }
    public String getParameterClassName(int param) throws SQLException {
        checkOpen(); return "java.lang.String"; }
    public String getParameterTypeName(int pos) { return "VARCHAR"; }
    public int getParameterType(int pos) { return Types.VARCHAR; }
    public int getParameterMode(int pos) { return parameterModeIn; }
    public int getPrecision(int pos) { return 0; }
    public int getScale(int pos) { return 0; }
    public int isNullable(int pos) { return parameterNullable; }
    public boolean isSigned(int pos) { return true; }


    // PARAMETER FUNCTIONS //////////////////////////////////////////

    public void setBoolean(int pos, boolean value) throws SQLException {
        setInt(pos, value ? 1 : 0); }
    public void setByte(int pos, byte value) throws SQLException {
        setInt(pos, (int)value); }
    public void setBytes(int pos, byte[] value) throws SQLException {
        checkOpen();
        if (db.bind_blob(pointer, pos, value) != SQLITE_OK) throw db.ex(); }
    public void setDouble(int pos, double value) throws SQLException {
        checkOpen();
        if (db.bind_double(pointer, pos, value) != SQLITE_OK) throw db.ex();
    }
    public void setFloat(int pos, float value) throws SQLException {
        setDouble(pos, value);
    }
    public void setInt(int pos, int value) throws SQLException {
        checkOpen();
        if (db.bind_int(pointer, pos, value) != SQLITE_OK) throw db.ex();
    }
    public void setLong(int pos, long value) throws SQLException {
        checkOpen();
        if (db.bind_long(pointer, pos, value) != SQLITE_OK) throw db.ex();
    }
    public void setNull(int pos, int u1) throws SQLException {
        setNull(pos, u1, null);
    }
    public void setNull(int pos, int u1, String u2) throws SQLException {
        checkOpen();
        if (db.bind_null(pointer, pos) != SQLITE_OK) throw db.ex();
    }
    public void setObject(int pos , Object value) throws SQLException {
        checkOpen();
        if (value == null) { setNull(pos, 0); return; }
        if (db.bind_text(pointer, pos, value.toString()) != SQLITE_OK)
            throw db.ex();
    }
    public void setObject(int p, Object v, int t) throws SQLException {
        setObject(p, v); }
    public void setObject(int p, Object v, int t, int s) throws SQLException {
        setObject(p, v); }
    public void setShort(int pos, short value) throws SQLException {
        setInt(pos, (int)value); }
    public void setString(int pos, String value) throws SQLException {
        checkOpen();
        if (db.bind_text(pointer, pos, value) != SQLITE_OK) throw db.ex();
    }


    // TODO

    public void setDate(int pos, Date x)
        throws SQLException { throw new SQLException("NYI"); }
    public void setDate(int pos, Date x, Calendar cal)
        throws SQLException { throw new SQLException("NYI"); }
    public void setTime(int pos, Time x)
        throws SQLException { throw new SQLException("NYI"); }
    public void setTime(int pos, Time x, Calendar cal)
        throws SQLException { throw new SQLException("NYI"); }
    public void setTimestamp(int pos, Timestamp x)
        throws SQLException { throw new SQLException("NYI"); }
    public void setTimestamp(int pos, Timestamp x, Calendar cal)
        throws SQLException { throw new SQLException("NYI"); }

    public void addBatch() throws SQLException {
        throw new SQLException("NYI"); }
    public void clearBatch() throws SQLException {
        throw new SQLException("NYI"); }
    public int[] executeBatch() throws SQLException {
        throw new SQLException("NYI"); }
}
