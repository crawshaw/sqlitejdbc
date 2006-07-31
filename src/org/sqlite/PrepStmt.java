package org.sqlite;

import java.io.Reader;
import java.io.InputStream;
import java.net.URL;
import java.sql.*;
import java.math.BigDecimal;
import java.util.Calendar;

/** See comment in RS.java to explain the strange inheritance hierarchy. */
final class PrepStmt extends Stmt implements PreparedStatement, Codes
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
    protected void finalize() throws SQLException { super.close(); }

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


    // TODO
    public ParameterMetaData getParameterMetaData() throws SQLException {
        throw new SQLException("NYI"); }



    // PARAMETER FUNCTIONS //////////////////////////////////////////

    public void setArray(int i, Array x) throws SQLException { throw new SQLException("NYI"); }
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException { throw new SQLException("NYI"); }
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException { throw new SQLException("NYI"); }
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException { throw new SQLException("NYI"); }
    public void setBlob(int i, Blob x) throws SQLException { throw new SQLException("NYI"); }
    public void setBoolean(int pos, boolean value) throws SQLException {
        setInt(pos, value ? 1 : 0); }
    public void setByte(int pos, byte value) throws SQLException {
        setInt(pos, (int)value); }
    public void setBytes(int pos, byte[] value) throws SQLException {
        checkOpen();
        if (db.bind_blob(pointer, pos, value) != SQLITE_OK) throw db.ex(); }
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException { throw new SQLException("NYI"); }
    public void setClob(int i, Clob x) throws SQLException { throw new SQLException("NYI"); }
    public void setDate(int parameterIndex, Date x) throws SQLException { throw new SQLException("NYI"); }
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException { throw new SQLException("NYI"); }
    public void setDouble(int pos, double value) throws SQLException {
        checkOpen();
        if (db.bind_double(pointer, pos, value) != SQLITE_OK) throw db.ex();
    }
    public void setFloat(int pos, float value) throws SQLException {
        checkOpen();
        if (db.bind_text(pointer, pos, Float.toString(value)) != SQLITE_OK)
            throw db.ex();
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
    public void setObject(int pos, Object value, int type) throws SQLException {
        setObject(pos, value);
    }
    public void setObject(int pos, Object v, int t, int s) throws SQLException {
        setObject(pos, v);
    }
    public void setRef(int i, Ref x) throws SQLException { throw new SQLException("NYI"); }
    public void setShort(int pos, short value) throws SQLException {
        setInt(pos, (int)value); }
    public void setString(int pos, String value) throws SQLException {
        checkOpen();
        if (db.bind_text(pointer, pos, value) != SQLITE_OK) throw db.ex();
    }
    public void setTime(int parameterIndex, Time x) throws SQLException { throw new SQLException("NYI"); }
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException { throw new SQLException("NYI"); }
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException { throw new SQLException("NYI"); }
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException { throw new SQLException("NYI"); }
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException { throw new SQLException("NYI"); }
    public void setURL(int parameterIndex, URL x) throws SQLException { throw new SQLException("NYI"); }


    public void addBatch() throws SQLException {
        throw new SQLException("NYI"); }
    public void clearBatch() throws SQLException {
        throw new SQLException("NYI"); }
    public int[] executeBatch() throws SQLException {
        throw new SQLException("NYI"); }


    // UNUSED FUNCTIONS /////////////////////////////////////////////

    public void addBatch(String sql) throws SQLException {
        throw new SQLException("cannot exec batch on PreparedStatement"); }
    public boolean execute(String sql) throws SQLException {
        throw new SQLException("cannot exec with params on PreparedStatement"); }
    public boolean execute(String sql, int autoKeys)throws SQLException {
        throw new SQLException("cannot exec with params on PreparedStatement"); }
    public boolean execute(String sql, int[] colinds) throws SQLException {
        throw new SQLException("cannot exec with params on PreparedStatement"); }
    public boolean execute(String sql, String[] colnames) throws SQLException {
        throw new SQLException("cannot exec with params on PreparedStatement"); }
    public ResultSet executeQuery(String sql) throws SQLException {
        throw new SQLException("cannot exec with params on PreparedStatement"); }
    public int executeUpdate(String sql) throws SQLException {
        throw new SQLException("cannot exec with params on PreparedStatement"); }
    public int executeUpdate(String sql, int autoKeys) throws SQLException {
        throw new SQLException("cannot exec with params on PreparedStatement"); }
    public int executeUpdate(String sql, int[] colinds) throws SQLException {
        throw new SQLException("cannot exec with params on PreparedStatement"); }
    public int executeUpdate(String sql, String[] cols) throws SQLException {
        throw new SQLException("cannot exec with params on PreparedStatement"); }
}
