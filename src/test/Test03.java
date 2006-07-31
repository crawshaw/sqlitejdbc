package test;

import java.sql.*;

public class Test03 implements Test.Case
{
    private String error;
    private Exception ex;

    public boolean run() throws Exception {
        int res;
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection(
            "jdbc:sqlite:build/test/test.db");

        ResultSet rs;
        Statement stat = conn.createStatement();

        // test empty ResultSets
        rs = stat.executeQuery("SELECT * FROM People WHERE pid = -100;");
        if (rs.next()) {
            error = "results found when none expected";
            return false;
        }
        rs.close();

        // test single-row ResultSets
        rs = stat.executeQuery(
            "SELECT pid, firstname FROM People WHERE pid = 1;");
        if (!rs.next()) {
            error = "no results found when one expected"; return false;
        }
        if (rs.getInt(1) != 1) {
            error = "bad getInt(1): " + rs.getInt(1); return false;
        } else if (!"Mohandas".equals(rs.getString(2))) {
            error = "bad getString(2): " + rs.getString(2); return false;
        }
        if (rs.next()) {
            error = "more data than expected"; return false;
        }
        rs.close();

        // test multi-row ResultSets
        rs = stat.executeQuery(
            "SELECT pid, firstname FROM People "
            + "WHERE pid = 1 OR pid = 2 ORDER BY pid ASC;");
        if (!rs.next()) {
            error = "no results found when two rows expected"; return false;
        }
        if (rs.getInt(1) != 1) {
            error = "bad row 1 getInt(1): " + rs.getInt(1); return false;
        } else if (!"Mohandas".equals(rs.getString(2))) {
            error = "bad getString(2): " + rs.getString(2); return false;
        }
        if (!rs.next()) {
            error = "one row found when two rows expected"; return false;
        }
        if (rs.getInt(1) != 2) {
            error = "bad getInt(1): " + rs.getInt(1); return false;
        } else if (!"Winston".equals(rs.getString(2))) {
            error = "bad getString(2): " + rs.getString(2); return false;
        }
        rs.close();

        stat.close();
        conn.close();
        return true;
    }
    public String name() { return "BasicExecuteQuery"; }
    public String error() { return error; }
    public Exception ex() { return ex; }
}
