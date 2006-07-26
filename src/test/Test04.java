package test;

import java.sql.*;

public class Test04 implements Test.Case
{
    private String error;
    private Exception ex;

    public boolean run() throws Exception {
        int res;
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection(
            "jdbc:sqlite:build/test/test.db");

        ResultSet rs;
        PreparedStatement prep = conn.prepareStatement( 
            "SELECT * FROM People WHERE pid = ?;");

        // test empty ResultSets
        prep.setInt(1, -100);
        rs = prep.executeQuery(); 
        if (rs.next()) {
            error = "results found when none expected";
            return false;
        }
        rs.close();
        prep.clearParameters();

        // test single-row ResultSets
        prep.setInt(1, 1); 
        rs = prep.executeQuery();
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
        prep.clearParameters();

        conn.close();
        return true;
    }

    public String name() { return "BasicPreparedQuery"; }
    public String error() { return error; }
    public Exception ex() { return ex; }
}
