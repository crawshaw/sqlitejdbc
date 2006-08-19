package test;

import java.sql.*;

public class Test19 implements Test.Case
{
    private String error;
    private Exception ex;

    public boolean run() throws Exception {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:");
        Statement stat = conn.createStatement();
        stat.executeUpdate(
            "create table test19 (id integer primary key, fn, sn);");
        stat.executeUpdate("create view test19view as select * from test19;");
        stat.close();

        DatabaseMetaData meta = conn.getMetaData();
        if (meta == null) { error = "expected meta data"; return false; }

        // check getTables
        ResultSet rs = meta.getTables(null, null, null, null);
        if (rs == null) { error = "expected getTables RS"; return false; }
        if (!rs.next()) { error = "expected table"; return false; }
        if (!"test19".equals(rs.getString("TABLE_NAME"))) {
            error = "bad table name"; return false; }
        if (!"test19".equals(rs.getString(3))) {
            error = "bad table name (ordered)"; return false; }
        if (!"TABLE".equals(rs.getString("TABLE_TYPE"))) {
            error = "bad table type"; return false; }
        if (!"TABLE".equals(rs.getString(4))) {
            error = "bad table type (ordered)"; return false; }

        if (!rs.next()) { error = "expected view"; return false; }
        if (!"test19view".equals(rs.getString("TABLE_NAME"))) {
            error = "bad view name"; return false; }
        if (!"VIEW".equals(rs.getString("TABLE_TYPE"))) {
            error = "bad view type"; return false; }

        conn.close();

        return true;
    }

    public String name() { return "DatabaseMetaData"; }
    public String error() { return error; }
    public Exception ex() { return ex; }
}
