package test;

import java.sql.*;

public class Test14 implements Test.Case
{
    private String error;
    private Exception ex;

    public boolean run() throws Exception {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        Statement stat = conn.createStatement();

        stat.addBatch("CREATE TABLE Test14 (id);");
        stat.addBatch("INSERT INTO Test14 VALUES (1);");
        stat.addBatch("INSERT INTO Test14 VALUES (2);");
        stat.addBatch("INSERT INTO Test14 VALUES (3);");
        stat.addBatch("INSERT INTO Test14 VALUES (4);");

        int[] r = stat.executeBatch();
        if (r[1] != 1 || r[2] != 1 || r[3] != 1 || r[4] != 1) {
            error = "unexpected return " + r[1]+","+r[2]+","+r[3]+","+r[4];
            return false;
        }
        stat.clearBatch();

        ResultSet rs = stat.executeQuery("SELECT COUNT(*) FROM Test14;");
        rs.next();
        if (rs.getInt(1) != 4) {
            error = "unexpected num of rows: " + rs.getInt(1); return false;
        }
        rs.close();

        stat.close();
        conn.close();
        return true;
    }
    public String name() { return "StatementBatch"; }
    public String error() { return error; }
    public Exception ex() { return ex; }
}
