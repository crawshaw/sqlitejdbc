package org.sqlite;

import java.sql.*;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Calendar;
import java.util.Map;

class RS implements ResultSet, ResultSetMetaData, Codes
{
    private final DB db;

    Stmt stmt;
    boolean isAfterLast = false;
    String[] cols = null;
    boolean[][] meta = null;

    private int limitRows; // 0 means no limit, must check against stmt.maxRows
    private int row = 1;   // number of current row, starts at 1
    private int lastCol;   // last column accessed, for wasNull(). -1 if none

    RS(Stmt stmt) {
        this(stmt, stmt.db.column_names(stmt.pointer), null);
    }

    RS(Stmt stmt, String[] cols, boolean[][] meta) {
        this.stmt = stmt;
        this.db   = stmt.db;
        this.cols = cols;
        this.meta = meta;
    }

    private void checkOpen() throws SQLException {
        if (stmt == null) throw new SQLException("ResultSet is closed");
        if (cols == null) throw new SQLException("inconsistent internal state");
    }
    private void checkCol(int col) throws SQLException {
        checkOpen();
        if (col < 1 || col > cols.length) throw new SQLException(
            "column " + col + " out of bounds [1," + cols.length + "]");
    }
    private void checkMeta() throws SQLException {
        if (meta == null) meta = db.column_metadata(stmt.pointer, cols);
    }
    private void markCol(int col) throws SQLException {
        checkCol(col); lastCol = col;
    }


    public int findColumn(String col) throws SQLException {
        for (int i=0; i < cols.length; i++)
            if (col.equalsIgnoreCase(cols[i])) return i+1;
        throw new SQLException("no such column: '"+col+"'");
    }

    public boolean next() throws SQLException {
        if (isAfterLast) return false;  // finished ResultSet
        lastCol = -1;

        // first row is loaded by execute(), so do not step() again
        if (row == 1) { row++; return true; }

        // check if we are row limited by the statement or the ResultSet
        if (stmt.maxRows != 0 && row > stmt.maxRows) return false;
        if (limitRows != 0 && row >= limitRows) return false;

        // do the real work
        switch (db.step(stmt.pointer)) {
            case SQLITE_BUSY:
                try { Thread.sleep(5); } catch (Exception e) {}
                return next(); // FIXME: bad idea? use statement timeout?
            case SQLITE_DONE:
                isAfterLast = true;
                close();      // agressive closing to avoid writer starvation
                return false;
            case SQLITE_ROW: row++; return true;
            case SQLITE_ERROR:
            case SQLITE_MISUSE:
            default:
                throw db.ex();
        }
    }

    public int getType() throws SQLException { return TYPE_FORWARD_ONLY; }
    public int getFetchDirection() { return stmt.getFetchDirection(); }
    public void setFetchDirection(int d) throws SQLException {
        stmt.setFetchDirection(d);
    }

    public int getFetchSize() throws SQLException { return limitRows; }
    public void setFetchSize(int rows) throws SQLException {
        if (0 > rows || (stmt.maxRows != 0 && rows > stmt.maxRows))
            throw new SQLException("fetch size " + rows
                                   + " out of bounds " + stmt.maxRows);
        limitRows = rows; 
    }

    public boolean isAfterLast() throws SQLException { return isAfterLast; }
    public boolean isBeforeFirst() throws SQLException { return row == 0; }
    public boolean isFirst() throws SQLException { return row == 1; }
    public boolean isLast() throws SQLException { // FIXME
        throw new SQLException("function not yet implemented for SQLite"); }

    void clear() throws SQLException {
        stmt.rs = null;
        stmt = null;
        cols = null;
        meta = null;
        isAfterLast = true;
    }

    public void close() throws SQLException {
        if (stmt == null) return;

        // close all references, reset statement
        db.reset(stmt.pointer);
        clear();
    }

    public String getCursorName() throws SQLException { return null; }

    public ResultSetMetaData getMetaData() throws SQLException { return this; }
    public int getRow() throws SQLException { return row; }
    public Statement getStatement() throws SQLException { return stmt; }

    public SQLWarning getWarnings() throws SQLException {
        checkOpen(); return null; }
    public void clearWarnings() throws SQLException {}

    public boolean wasNull() throws SQLException {
        checkCol(lastCol);
        return db.column_text(stmt.pointer, lastCol) == null; }


    // DATA ACCESS FUNCTIONS ////////////////////////////////////////

    public Array getArray(int i) throws SQLException {
        throw new SQLException("NYI"); }
    public Array getArray(String col) throws SQLException {
        return getArray(findColumn(col)); }
    public InputStream getAsciiStream(int col) throws SQLException {
        throw new SQLException("NYI"); }
    public InputStream getAsciiStream(String col) throws SQLException {
        return getAsciiStream(findColumn(col)); }
    public BigDecimal getBigDecimal(int col) throws SQLException {
        return getBigDecimal(col, 10); } // FIXME scale?
    public BigDecimal getBigDecimal(int col, int scale) throws SQLException {
        throw new SQLException("NYI"); }
    public BigDecimal getBigDecimal(String col) throws SQLException {
        return getBigDecimal(findColumn(col), 10); }
    public BigDecimal getBigDecimal(String col, int scale) throws SQLException {
        return getBigDecimal(findColumn(col), 10); }
    public InputStream getBinaryStream(int col) throws SQLException {
        throw new SQLException("NYI"); }
    public InputStream getBinaryStream(String col) throws SQLException {
        return getBinaryStream(findColumn(col)); }
    public Blob getBlob(int col) throws SQLException {
        throw new SQLException("NYI"); }
    public Blob getBlob(String col) throws SQLException {
        return getBlob(findColumn(col)); }
    public boolean getBoolean(int col) throws SQLException {
        throw new SQLException("NYI"); }
    public boolean getBoolean(String col) throws SQLException {
        return getBoolean(findColumn(col)); }
    public byte getByte(int col) throws SQLException {
        throw new SQLException("NYI"); }
    public byte getByte(String col) throws SQLException {
        return getByte(findColumn(col)); }
    public byte[] getBytes(int col) throws SQLException {
        throw new SQLException("NYI"); }
    public byte[] getBytes(String col) throws SQLException {
        return getBytes(findColumn(col)); }
    public Reader getCharacterStream(int col) throws SQLException {
        throw new SQLException("NYI"); }
    public Reader getCharacterStream(String col) throws SQLException {
        return getCharacterStream(findColumn(col)); }
    public Clob getClob(int col) throws SQLException {
        throw new SQLException("NYI"); }
    public Clob getClob(String col) throws SQLException {
        return getClob(findColumn(col)); }
    public int getConcurrency() throws SQLException {
        throw new SQLException("NYI"); }
    public Date getDate(int col) throws SQLException {
        return getDate(col, Calendar.getInstance()); }
    public Date getDate(int col, Calendar cal) throws SQLException {
        throw new SQLException("NYI"); }
    public Date getDate(String col) throws SQLException {
        return getDate(findColumn(col), Calendar.getInstance()); }
    public Date getDate(String col, Calendar cal) throws SQLException {
        return getDate(findColumn(col), cal); }
    public double getDouble(int col) throws SQLException {
        markCol(col); return db.column_double(stmt.pointer, col - 1); }
    public double getDouble(String col) throws SQLException {
        return getDouble(findColumn(col)); }
    public float getFloat(int col) throws SQLException {
        markCol(col);
        return Float.parseFloat(db.column_text(stmt.pointer, col - 1)); }
    public float getFloat(String col) throws SQLException {
        return getFloat(findColumn(col)); }
    public int getInt(int col) throws SQLException {
        markCol(col); return db.column_int(stmt.pointer, col - 1); }
    public int getInt(String col) throws SQLException {
        return getInt(findColumn(col)); }
    public long getLong(int col) throws SQLException {
        markCol(col); return db.column_long(stmt.pointer, col - 1); }
    public long getLong(String col) throws SQLException {
        return getLong(findColumn(col)); }
    public Object getObject(int col) throws SQLException {
        return getObject(col, null); }
    public Object getObject(int col, Map map) throws SQLException {
        throw new SQLException("NYI"); }
    public Object getObject(String col) throws SQLException {
        return getObject(findColumn(col)); }
    public Object getObject(String col, Map map) throws SQLException {
        return getObject(findColumn(col), map); }
    public Ref getRef(int i) throws SQLException {
        throw new SQLException("NYI"); }
    public Ref getRef(String col) throws SQLException {
        return getRef(findColumn(col)); }
    public short getShort(int col) throws SQLException {
        throw new SQLException("NYI"); }
    public short getShort(String col) throws SQLException {
        return getShort(findColumn(col)); }

    public String getString(int col) throws SQLException {
        markCol(col); return db.column_text(stmt.pointer, col - 1); }
    public String getString(String col) throws SQLException {
        return getString(findColumn(col)); }
    public Time getTime(int col) throws SQLException {
        return getTime(col, Calendar.getInstance()); }
    public Time getTime(int col, Calendar cal) throws SQLException {
        throw new SQLException("NYI"); }
    public Time getTime(String col) throws SQLException {
        return getTime(findColumn(col)); }
    public Time getTime(String col, Calendar cal) throws SQLException {
        return getTime(findColumn(col), cal); }
    public Timestamp getTimestamp(int col) throws SQLException {
        return getTimestamp(col, Calendar.getInstance()); }
    public Timestamp getTimestamp(int col, Calendar cal) throws SQLException {
        throw new SQLException("NYI"); }
    public Timestamp getTimestamp(String col) throws SQLException {
        return getTimestamp(findColumn(col), Calendar.getInstance()); }
    public Timestamp getTimestamp(String col, Calendar cal) throws SQLException {
        return getTimestamp(findColumn(col), cal); }
    public InputStream getUnicodeStream(int col) throws SQLException {
        throw new SQLException("NYI"); }
    public InputStream getUnicodeStream(String col) throws SQLException {
        return getUnicodeStream(findColumn(col)); }
    public URL getURL(int col) throws SQLException {
        throw new SQLException("NYI"); }
    public URL getURL(String col) throws SQLException {
        return getURL(findColumn(col)); }


    // ResultSetMetaData Functions //////////////////////////////////

    public String getCatalogName(int col) throws SQLException {
        checkCol(col); return db.column_table_name(stmt.pointer, col - 1); }
    public String getColumnClassName(int col) throws SQLException {
        checkCol(col); throw new SQLException("NYI"); }
    public int getColumnCount() throws SQLException {
        checkOpen(); return cols.length; }
    public int getColumnDisplaySize(int col) throws SQLException {
        return Integer.MAX_VALUE; }
    public String getColumnLabel(int col) throws SQLException {
        return getColumnName(col); }
    public String getColumnName(int col) throws SQLException {
        checkCol(col); return db.column_name(stmt.pointer, col - 1); }
    public int getColumnType(int col) throws SQLException {
        checkCol(col);
        switch (db.column_type(stmt.pointer, col - 1)) {
            case SQLITE_INTEGER: return Types.INTEGER;
            case SQLITE_FLOAT:   return Types.FLOAT;
            case SQLITE_BLOB:    return Types.BLOB;
            case SQLITE_NULL:    return Types.NULL;
            case SQLITE_TEXT:
            default:
                return Types.VARCHAR;
        }
    }
    public String getColumnTypeName(int col) throws SQLException {
        return db.column_decltype(stmt.pointer, col - 1);
    }
    public int getPrecision(int col) throws SQLException { return 0; } // FIXME
    public int getScale(int col) throws SQLException { return 0; }
    public String getSchemaName(int col) throws SQLException {
        throw new SQLException("NYI"); } 
    public String getTableName(int col) throws SQLException {
        return db.column_table_name(stmt.pointer, col - 1); }
    public int isNullable(int col) throws SQLException {
        checkMeta(); return meta[col - 1][1] ? columnNoNulls: columnNullable;
    }
    public boolean isAutoIncrement(int col) throws SQLException {
        checkMeta(); return meta[col - 1][2]; }
    public boolean isCaseSensitive(int col) throws SQLException { return true; }
    public boolean isCurrency(int col) throws SQLException { return false; }
    public boolean isDefinitelyWritable(int col) throws SQLException {
        return true; } // FIXME: check db file constraints?
    public boolean isReadOnly(int col) throws SQLException { return false; }
    public boolean isSearchable(int col) throws SQLException { return true; }
    public boolean isSigned(int col) throws SQLException { return false; }
    public boolean isWritable(int col) throws SQLException { return true; }


    // UNUSED FUNCTIONS /////////////////////////////////////////////

    public boolean rowDeleted()  throws SQLException { return false; }
    public boolean rowInserted() throws SQLException { return false; }
    public boolean rowUpdated()  throws SQLException { return false; }

    public void insertRow() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }
    public void moveToCurrentRow() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }
    public void moveToInsertRow() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }
    public boolean last() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }
    public boolean previous() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }
    public boolean relative(int rows) throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }
    public boolean absolute(int row) throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }
    public void afterLast() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }
    public void beforeFirst() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }
    public void cancelRowUpdates() throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void deleteRow() throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public boolean first() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }

    public void updateArray(int col, Array x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateArray(String col, Array x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateAsciiStream(int col, InputStream x, int l)
            throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateAsciiStream(String col, InputStream x, int l)
            throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateBigDecimal(int col, BigDecimal x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateBigDecimal(String col, BigDecimal x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateBinaryStream(int c, InputStream x, int l)
            throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateBinaryStream(String c, InputStream x, int l)
            throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateBlob(int col, Blob x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateBlob(String col, Blob x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateBoolean(int col, boolean x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateBoolean(String col, boolean x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateByte(int col, byte x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateByte(String col, byte x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateBytes(int col, byte[] x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateBytes(String col, byte[] x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateCharacterStream(int c, Reader x, int l)
            throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateCharacterStream(String c, Reader r, int l)
            throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateClob(int col, Clob x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateClob(String col, Clob x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateDate(int col, Date x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateDate(String col, Date x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateDouble(int col, double x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateDouble(String col, double x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateFloat(int col, float x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateFloat(String col, float x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateInt(int col, int x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateInt(String col, int x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateLong(int col, long x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateLong(String col, long x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateNull(int col) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateNull(String col) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateObject(int c, Object x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateObject(int c, Object x, int scale) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateObject(String col, Object x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateObject(String c, Object x, int s) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateRef(int col, Ref x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateRef(String c, Ref x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateRow() throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateShort(int c, short x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateShort(String c, short x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateString(int c, String x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateString(String c, String x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateTime(int c, Time x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateTime(String c, Time x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateTimestamp(int c, Timestamp x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }
    public void updateTimestamp(String c, Timestamp x) throws SQLException {
        throw new SQLException("unsupported by SQLite: updating a ResultSet"); }

    public void refreshRow() throws SQLException {
        throw new SQLException("unsupported by SQLite: refreshing rows"); }
}
