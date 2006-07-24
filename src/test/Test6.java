package test;

import java.sql.*;

public class Test6 implements Test.Case
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
        rs = stat.executeQuery(
            "SELECT pid, firstname, surname, dob FROM People WHERE pid = 3;");
        if (!rs.next()) {
            error = "expected data for pid=3"; return false;
        }
        if (rs.getInt("pid") != 3) {
            error = "expected getInt(pid) == 3"; return false;
        }
        if (rs.getInt("pid") != 3) {
            error = "expected to be able to reuse getInt(pid)"; return false;
        }
        if (!"3".equals(rs.getString("pid"))) {
            error = "expected manifest typing to give getString(pid) \"3\"";
            return false;
        }
        if (!"Bertrand".equals(rs.getString("firstname"))) {
            error = "expected getString(firstname) to be Bertrand, got '"
                + rs.getString("firstname") + "'";
            return false;
        }
        if (!"Russell".equals(rs.getString("surname"))) {
            error = "expected getString(surname) to be Russell, got '"
                + rs.getString("surname") + "'";
            return false;
        }
        if (!"1872-05-18".equals(rs.getString("dob"))) {
            error = "expected getString(dob) to be 1872-05-18, got '"
                + rs.getString("dob") + "'";
            return false;
            // TODO: rs.getDate("dob")
        }

        rs.close();
        stat.close();
        conn.close();
        return true;
    }

    public String name() { return "NamedColumns"; }
    public String error() { return error; }
    public Exception ex() { return ex; }
}
