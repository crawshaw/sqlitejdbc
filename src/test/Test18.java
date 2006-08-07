package test;

import java.sql.*;

public class Test18 implements Test.Case
{
    private String error;
    private Exception ex;

    public boolean run() throws Exception {
        int res;
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:");
        Statement stat = conn.createStatement();
        stat.executeUpdate("create table test18 (c1, c2, c3);");
        stat.executeUpdate("insert into test18 values (1, 1, 1);");
        stat.close();

        PreparedStatement prep = conn.prepareStatement(
            "select c1, c2, c3 from test18;");

        if (prep.getMetaData().getColumnCount() != 3) {
            error = "bad prep column count"; return false;
        }

        ResultSet rs = prep.executeQuery();
        if (rs.getMetaData().getColumnCount() != 3) {
            error = "bad rs column count"; return false;
        }
        rs.close();

        if (prep.getMetaData().getColumnCount() != 3) {
            error = "bad prep 2 column count"; return false;
        }

        prep.close();
        conn.close();

        return true;
    }

    public String name() { return "PrepMetaData"; }
    public String error() { return error; }
    public Exception ex() { return ex; }
}
