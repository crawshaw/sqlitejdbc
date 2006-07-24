package org.sqlite;

import java.io.Reader;
import java.io.InputStream;
import java.net.URL;
import java.sql.*;
import java.math.BigDecimal;
import java.util.Calendar;

class PrepStmt extends Stmt implements PreparedStatement, Codes
{
    /** Used as implementation of ResultSetMetaData. */
    private RS rsMetaData;

    PrepStmt(Conn conn, DB db, String sql) throws SQLException {
        super(conn, db);

        pointer = db.prepare(sql);
        rsMetaData = new RS(this);
    }

    protected RS createResultSet() throws SQLException {
        return new RS(this, rsMetaData.cols, rsMetaData.meta);
    }

    public void addBatch() throws SQLException { throw new SQLException("NYI"); }
    public void clearParameters() throws SQLException {
        db.clear_bindings(pointer); // TODO: use return result?
        if (rs != null) rs.close();
        db.reset(pointer);
    }
    public boolean execute() throws SQLException { return exec(); }
    public ResultSet executeQuery() throws SQLException {
        if (!execute()) throw new SQLException("query does not return results");
        return getResultSet();
    }
    public int executeUpdate() throws SQLException {
        if (execute()) throw new SQLException("query returns results");
        int changes = db.changes(pointer);
        db.reset(pointer);
        return changes;
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        checkOpen(); return rsMetaData; }


    // TODO
    public ParameterMetaData getParameterMetaData() throws SQLException {
        throw new SQLException("NYI"); }



    // PARAMETER FUNCTIONS //////////////////////////////////////////

    public void setArray(int i, Array x) throws SQLException { throw new SQLException("NYI"); }
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException { throw new SQLException("NYI"); }
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException { throw new SQLException("NYI"); }
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException { throw new SQLException("NYI"); }
    public void setBlob(int i, Blob x) throws SQLException { throw new SQLException("NYI"); }
    public void setBoolean(int parameterIndex, boolean x) throws SQLException { throw new SQLException("NYI"); }
    public void setByte(int parameterIndex, byte x) throws SQLException { throw new SQLException("NYI"); }
    public void setBytes(int parameterIndex, byte[] x) throws SQLException { throw new SQLException("NYI"); }
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException { throw new SQLException("NYI"); }
    public void setClob(int i, Clob x) throws SQLException { throw new SQLException("NYI"); }
    public void setDate(int parameterIndex, Date x) throws SQLException { throw new SQLException("NYI"); }
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException { throw new SQLException("NYI"); }
    public void setDouble(int pos, double value) throws SQLException {
        if (db.bind_double(pointer, pos, value) != SQLITE_OK) throw db.ex();
    }
    public void setFloat(int pos, float value) throws SQLException {
        if (db.bind_text(pointer, pos, Float.toString(value)) != SQLITE_OK)
            throw db.ex();
    }
    public void setInt(int pos, int value) throws SQLException {
        if (db.bind_int(pointer, pos, value) != SQLITE_OK) throw db.ex();
    }
    public void setLong(int pos, long value) throws SQLException {
        if (db.bind_long(pointer, pos, value) != SQLITE_OK) throw db.ex();
    }
    public void setNull(int pos, int u1) throws SQLException {
        setNull(pos, u1, null);
    }
    public void setNull(int pos, int u1, String u2) throws SQLException {
        if (db.bind_null(pointer, pos) != SQLITE_OK) throw db.ex();
    }
    public void setObject(int pos , Object value) throws SQLException {
        if (value == null) {
            setNull(pos, 0);
        } else {
            if (db.bind_text(pointer, pos, value.toString()) != SQLITE_OK)
                throw db.ex();
        }
    }
    public void setObject(int pos, Object value, int type) throws SQLException {
        // FIXME: more complicated
        throw new SQLException("NYI");
    }
    public void setObject(int pos, Object value, int type, int scale)
            throws SQLException {
        // FIXME
        throw new SQLException("NYI");
    }
    public void setRef(int i, Ref x) throws SQLException { throw new SQLException("NYI"); }
    public void setShort(int parameterIndex, short x) throws SQLException { throw new SQLException("NYI"); }
    public void setString(int pos, String value) throws SQLException {
        if (db.bind_text(pointer, pos, value) != SQLITE_OK) throw db.ex();
    }
    public void setTime(int parameterIndex, Time x) throws SQLException { throw new SQLException("NYI"); }
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException { throw new SQLException("NYI"); }
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException { throw new SQLException("NYI"); }
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException { throw new SQLException("NYI"); }
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException { throw new SQLException("NYI"); }
    public void setURL(int parameterIndex, URL x) throws SQLException { throw new SQLException("NYI"); }



    // UNUSED FUNCTIONS /////////////////////////////////////////////

    public boolean execute(String sql) throws SQLException {
        throw new SQLException("cannot exec with params on PreparedStatement"); }
    public boolean execute(String sql, int autoKeys)throws SQLException {
        throw new SQLException("cannot exec with params on PreparedStatement"); }
    public boolean execute(String sql, int[] colinds) throws SQLException {
        throw new SQLException("cannot exec with params on PreparedStatement"); }
    public boolean execute(String sql, String[] colnames) throws SQLException {
        throw new SQLException("cannot exec with params on PreparedStatement"); }
    public int[] executeBatch() throws SQLException { throw new SQLException("NYI");}
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
