package test;

import java.sql.*;

public class Test08 implements Test.Case
{
    private String error;
    private Exception ex;

    public boolean run() throws Exception {
        int res;
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection(
            "jdbc:sqlite:build/test/test.db");
        Statement stat = conn.createStatement();

        ResultSet rs;
        String msg;

        rs = stat.executeQuery("SELECT pid, firstname, surname FROM People;");
        ResultSetMetaData meta = rs.getMetaData();

        if (meta.getColumnCount() != 3) {
            error = "bad column count: " + meta.getColumnCount(); return false;
        }
        if (!"People".equals(meta.getCatalogName(1))) {
            error = "bad catalog name: " + meta.getCatalogName(1); return false;
        }
        if (!"pid".equals(meta.getColumnName(1))) {
            error = "bad column 1 name: " + meta.getColumnName(1); return false;
        }
        if (!"firstname".equals(meta.getColumnName(2))) {
            error = "bad column 2 name: " + meta.getColumnName(2); return false;
        }
        if (!"surname".equals(meta.getColumnName(3))) {
            error = "bad column 3 name: " + meta.getColumnName(3); return false;
        }

        msg = null;
        try { meta.getCatalogName(4); }
        catch (SQLException e) { msg = e.getMessage(); }
        if (!"column 4 out of bounds [1,3]".equals(msg)) {
            error = "bad error for out of bounds catalog: " + msg; return false;
        }

        msg = null;
        try { meta.getColumnName(4); }
        catch (SQLException e) { msg = e.getMessage(); }
        if (!"column 4 out of bounds [1,3]".equals(msg)) {
            error = "bad error for out of bounds colname: " + msg; return false;
        }

        if (meta.getColumnType(1) != Types.INTEGER) {
            error = "expected type INTEGER for col 1"; return false;
        }
        if (meta.getColumnType(2) != Types.VARCHAR) {
            error = "expected type VARCHAR for col 2"; return false;
        }
        if (meta.getColumnType(3) != Types.VARCHAR) {
            error = "expected type VARCHAR for col 3"; return false;
        }

        if (!meta.isAutoIncrement(1)) {
            error = "expected col 1 to be auto-increment"; return false;
        }
        if (meta.isAutoIncrement(2)) {
            error = "expected col 2 to not be auto-increment"; return false;
        }
        if (meta.isAutoIncrement(3)) {
            error = "expected col 3 to not be auto-increment"; return false;
        }
        if (meta.isNullable(1) != meta.columnNoNulls) {
            error = "expected col 1 to take NoNulls"; return false;
        }
        if (meta.isNullable(2) != meta.columnNullable) {
            error = "expected col 2 to take nulls"; return false;
        }
        if (meta.isNullable(3) != meta.columnNullable) {
            error = "expected col 3 to take nulls"; return false;
        }

        rs.close();
        stat.close();
        conn.close();

        return true;
    }

    public String name() { return "RSMetaData"; }
    public String error() { return error; }
    public Exception ex() { return ex; }
}
