/* Copyright 2006 David Crawshaw, see LICENSE file for licensing [BSD]. */
package org.sqlite;

import java.sql.*;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Calendar;
import java.util.Map;

/** Implements a JDBC ResultSet.
 *
 * As only one ResultSet can exist per statement, this implementation
 * takes the odd step of making the ResultSet and Statement the same
 * object. This means:
 *     ResultSet rs = statement.executeQuery("SELECT ...");
 *
 * Generates no temporary ResultSet object, it just returns itself.
 * When a great many ResultSets are used (e.g. in a loop), this can
 * help reduce the load on the Garbage collector.
 *
 * As a result of this odd arrangement, Stmt and PrepStmt must
 * extend RS:
 *     Object -- Unused -- RS -- Stmt -- PrepStmt
 *
 * Such inheritance requires careful checking of the object state,
 * for which the check...() functions and isRS() function handle.
 */
abstract class RS extends Unused implements ResultSet, ResultSetMetaData, Codes
{
    DB db;

    long pointer = 0;
    boolean isAfterLast = false;
    int maxRows;              // max. number of rows as set by a Statement
    String[] cols = null;     // if null, the RS is closed()
    String[] colsMeta = null; // same as cols, but used by Meta interface
    boolean[][] meta = null;

    private int limitRows; // 0 means no limit, must check against maxRows
    private int row = 1;   // number of current row, starts at 1
    private int lastCol;   // last column accessed, for wasNull(). -1 if none

    RS(DB db) { this.db = db; }


    // INTERNAL FUNCTIONS ///////////////////////////////////////////

    protected final void checkOpen() throws SQLException {
        if (db == null) throw new SQLException("statement is closed");
    }
    protected final void checkExec() throws SQLException {
        if (pointer == 0) throw new SQLException("statement is not executing");
    }
    protected final void checkRS() throws SQLException {
        if (db == null || !isRS()) throw new SQLException("ResultSet closed");
    }
    /** Returns true if this Statement is an currently an active ResultSet. */
    protected final boolean isRS() { return cols != null; }

    // takes col in [1,x] form, returns in [0,x-1] form
    private int checkCol(int col) throws SQLException {
        checkOpen();
        if (colsMeta == null) throw new IllegalStateException(
            "SQLite JDBC: inconsistent internal state");
        if (col < 1 || col > colsMeta.length) throw new SQLException(
            "column " + col + " out of bounds [1," + colsMeta.length + "]");
        return --col;
    }

    // takes col in [1,x] form, marks it as last accessed and returns [0,x-1]
    private int markCol(int col) throws SQLException {
        checkRS(); checkCol(col); lastCol = col; return --col;
    }

    private void checkMeta() throws SQLException {
        checkCol(1);
        if (meta == null) meta = db.column_metadata(pointer, cols);
    }


    // ResultSet Functions //////////////////////////////////////////

    // returns col in [1,x] form
    public int findColumn(String col) throws SQLException {
        checkRS();
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
        if (maxRows != 0 && row > maxRows) return false;
        if (limitRows != 0 && row >= limitRows) return false;

        // do the real work
        switch (db.step(pointer)) {
            case SQLITE_BUSY:
                throw new SQLException("database locked");
            case SQLITE_DONE:
                isAfterLast = true;
                close();      // agressive closing to avoid writer starvation
                return false;
            case SQLITE_ROW: row++; return true;
            case SQLITE_MISUSE:
                 throw new SQLException("JDBC internal consistency error");
            case SQLITE_ERROR:
            default:
                throw db.ex();
        }
    }

    public int getType() throws SQLException { return TYPE_FORWARD_ONLY; }

    public int getFetchSize() throws SQLException { return limitRows; }
    public void setFetchSize(int rows) throws SQLException {
        if (0 > rows || (maxRows != 0 && rows > maxRows))
            throw new SQLException("fetch size " + rows
                                   + " out of bounds " + maxRows);
        limitRows = rows; 
    }

    public int getFetchDirection() throws SQLException {
        checkOpen(); return ResultSet.FETCH_FORWARD; }
    public void setFetchDirection(int d) throws SQLException {
        checkOpen();
        if (d != ResultSet.FETCH_FORWARD)
            throw new SQLException("only FETCH_FORWARD direction supported");
    }

    public boolean isAfterLast() throws SQLException { return isAfterLast; }
    public boolean isBeforeFirst() throws SQLException { return row == 0; }
    public boolean isFirst() throws SQLException { return row == 1; }
    public boolean isLast() throws SQLException { // FIXME
        throw new SQLException("function not yet implemented for SQLite"); }

    /** Resets the RS in a way safe for both Stmt and PrepStmt.
     *  Full reset happens in Stmt.close(). */
    void clear() throws SQLException {
        cols = null;
        isAfterLast = true;
        limitRows = 0;
        row = 1;
        lastCol = -1;
    }
    protected void finalize() throws SQLException { clear(); }

    public String getCursorName() throws SQLException { return null; }

    public Statement getStatement() throws SQLException { return (Stmt)this; }
    public ResultSetMetaData getMetaData() throws SQLException { return this; }
    public int getRow() throws SQLException { return row; }

    public SQLWarning getWarnings() throws SQLException {
        checkOpen(); return null; }
    public void clearWarnings() throws SQLException {}

    public boolean wasNull() throws SQLException {
        // TODO: optimise
        return db.column_text(pointer, markCol(lastCol)) == null;
    }


    // DATA ACCESS FUNCTIONS ////////////////////////////////////////

    public boolean getBoolean(int col) throws SQLException {
        return getInt(col) == 0 ? false : true; }
    public boolean getBoolean(String col) throws SQLException {
        return getBoolean(findColumn(col)); }
    public byte getByte(int col) throws SQLException {
        return (byte)getInt(col); }
    public byte getByte(String col) throws SQLException {
        return getByte(findColumn(col)); }
    public byte[] getBytes(int col) throws SQLException {
        return db.column_blob(pointer, markCol(col)); }
    public byte[] getBytes(String col) throws SQLException {
        return getBytes(findColumn(col)); }
    public Date getDate(int col) throws SQLException {
        return getDate(col, Calendar.getInstance()); }
    public Date getDate(int col, Calendar cal) throws SQLException {
        throw new SQLException("NYI"); } // TODO
    public Date getDate(String col) throws SQLException {
        return getDate(findColumn(col), Calendar.getInstance()); }
    public Date getDate(String col, Calendar cal) throws SQLException {
        return getDate(findColumn(col), cal); }
    public double getDouble(int col) throws SQLException {
        return db.column_double(pointer, markCol(col)); }
    public double getDouble(String col) throws SQLException {
        return getDouble(findColumn(col)); }
    public float getFloat(int col) throws SQLException {
        return Float.parseFloat(db.column_text(pointer, markCol(col))); }
    public float getFloat(String col) throws SQLException {
        return getFloat(findColumn(col)); }
    public int getInt(int col) throws SQLException {
        return db.column_int(pointer, markCol(col)); }
    public int getInt(String col) throws SQLException {
        return getInt(findColumn(col)); }
    public long getLong(int col) throws SQLException {
        return db.column_long(pointer, markCol(col)); }
    public long getLong(String col) throws SQLException {
        return getLong(findColumn(col)); }
    public short getShort(int col) throws SQLException {
        return (short)getInt(col); }
    public short getShort(String col) throws SQLException {
        return getShort(findColumn(col)); }

    public String getString(int col) throws SQLException {
        return db.column_text(pointer, markCol(col)); }
    public String getString(String col) throws SQLException {
        return getString(findColumn(col)); }

    public Time getTime(int col) throws SQLException {
        return getTime(col, Calendar.getInstance()); }
    public Time getTime(int col, Calendar cal) throws SQLException {
        throw new SQLException("NYI"); } // TODO
    public Time getTime(String col) throws SQLException {
        return getTime(findColumn(col)); }
    public Time getTime(String col, Calendar cal) throws SQLException {
        return getTime(findColumn(col), cal); }
    public Timestamp getTimestamp(int col) throws SQLException {
        return getTimestamp(col, Calendar.getInstance()); }
    public Timestamp getTimestamp(int col, Calendar cal) throws SQLException {
        throw new SQLException("NYI"); } // TODO
    public Timestamp getTimestamp(String col) throws SQLException {
        return getTimestamp(findColumn(col), Calendar.getInstance()); }
    public Timestamp getTimestamp(String c, Calendar ca) throws SQLException {
        return getTimestamp(findColumn(c), ca); }


    // ResultSetMetaData Functions //////////////////////////////////

    // we do not need to check the RS is open, only that colsMeta
    // is not null, done with checkCol(int).

    public String getCatalogName(int col) throws SQLException {
        return db.column_table_name(pointer, checkCol(col)); }
    public String getColumnClassName(int col) throws SQLException {
        checkCol(col); throw new SQLException("NYI"); }
    public int getColumnCount() throws SQLException {
        checkCol(1); return colsMeta.length;
    }
    public int getColumnDisplaySize(int col) throws SQLException {
        return Integer.MAX_VALUE; }
    public String getColumnLabel(int col) throws SQLException {
        return getColumnName(col); }
    public String getColumnName(int col) throws SQLException {
        return db.column_name(pointer, checkCol(col)); }
    public int getColumnType(int col) throws SQLException {
        switch (db.column_type(pointer, checkCol(col))) {
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
        return db.column_decltype(pointer, checkCol(col));
    }
    public int getPrecision(int col) throws SQLException { return 0; } // FIXME
    public int getScale(int col) throws SQLException { return 0; }
    public String getSchemaName(int col) throws SQLException {
        throw new SQLException("NYI"); } 
    public String getTableName(int col) throws SQLException {
        return db.column_table_name(pointer, checkCol(col)); }
    public int isNullable(int col) throws SQLException {
        checkMeta();
        return meta[checkCol(col)][1] ? columnNoNulls: columnNullable;
    }
    public boolean isAutoIncrement(int col) throws SQLException {
        checkMeta(); return meta[checkCol(col)][2]; }
    public boolean isCaseSensitive(int col) throws SQLException { return true; }
    public boolean isCurrency(int col) throws SQLException { return false; }
    public boolean isDefinitelyWritable(int col) throws SQLException {
        return true; } // FIXME: check db file constraints?
    public boolean isReadOnly(int col) throws SQLException { return false; }
    public boolean isSearchable(int col) throws SQLException { return true; }
    public boolean isSigned(int col) throws SQLException { return false; }
    public boolean isWritable(int col) throws SQLException { return true; }

    public int getConcurrency() throws SQLException { return CONCUR_READ_ONLY; }


    public boolean rowDeleted()  throws SQLException { return false; }
    public boolean rowInserted() throws SQLException { return false; }
    public boolean rowUpdated()  throws SQLException { return false; }
}
