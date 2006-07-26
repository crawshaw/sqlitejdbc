package test;

import java.sql.*;

public class Test10 implements Test.Case
{
    private String error;
    private Exception ex;

    public boolean run() throws Exception {
        int res;
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");

        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TEMP TABLE Test10 (id);");

        for (int i=0; i < 1000; i++)
            stat.executeUpdate("INSERT INTO Test10 VALUES ('Hello world');");

        ResultSet rs = stat.executeQuery("SELECT COUNT(id) FROM Test10;");
        if (!rs.next()) { error = "expected result"; return false; }
        if (rs.getInt(1) != 1000) { error = "expected entries"; return false; }

        rs.close();
        stat.close();
        conn.close();
        return true;
    }
    public String name() { return "MemInsert1000"; }
    public String error() { return error; }
    public Exception ex() { return ex; }
}
