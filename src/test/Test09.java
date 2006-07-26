package test;

import java.sql.*;

public class Test09 implements Test.Case
{
    private String error;
    private Exception ex;

    public boolean run() throws Exception {
        int res;
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection(
            "jdbc:sqlite:build/test/test.db");

        Statement stat = conn.createStatement();
        stat.executeUpdate("DROP TABLE IF EXISTS Test9;");
        stat.executeUpdate("CREATE TABLE Test9 (id);");
        stat.executeUpdate("INSERT INTO Test9 VALUES (NULL);");

        ResultSet rs = stat.executeQuery("SELECT id FROM Test9;");
        rs.next();
        if (rs.getString(1) != null) {
            error = "expected getString(1) to equal null"; return false;
        }
        if (!rs.wasNull()) {
            error = "expected wasNull() to return true"; return false;
        }

        rs.close();
        stat.close();
        conn.close();
        return true;
    }
    public String name() { return "Nulls"; }
    public String error() { return error; }
    public Exception ex() { return ex; }
}
