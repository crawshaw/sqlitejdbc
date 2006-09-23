package test;

import org.sqlite.Function;
import java.sql.*;

public class Test24 implements Test.Case
{
    private int rows = 500;

    private String error;
    private Exception ex;

    public boolean run() throws Exception {
        Class.forName("org.sqlite.JDBC");

        Connection conn = DriverManager.getConnection("jdbc:sqlite:");
        conn.createStatement().execute("create table test(c1, c2, c3, c4);");
        PreparedStatement prep = conn.prepareStatement(
                "insert into test values(?, ?, ?, ?);");

        for (int i=0; i < rows; i++) {
            prep.setInt(1, Integer.MIN_VALUE);
            prep.setFloat(2, Float.MIN_VALUE);
            prep.setString(3, "Hello World");
            prep.setDouble(4, Double.MAX_VALUE);

            prep.addBatch();
        }

        int[] changes = prep.executeBatch();

        if (changes == null || changes.length != rows) {
            error = "bad int[] on exec" + changes.length; return false; }
        for (int i=0; i < rows; i++) {
            if (changes[i] != 1) { error = "bad change "+i; return false; }
        }

        ResultSet rs = conn.createStatement().executeQuery(
                "select * from test;");
        for (int i=0; i < rows; i++) {
            if (!rs.next()) { error = "expected next()"; return false; }
            if (rs.getInt(1) != Integer.MIN_VALUE) {
                error = "bad int"; return false; }
            if (rs.getFloat(2) != Float.MIN_VALUE) {
                error = "bad float"; return false; }
            if (!"Hello World".equals(rs.getString(3))) {
                error = "bad string"; return false; }
            if (rs.getDouble(4) != Double.MAX_VALUE) {
                error = "bad double"; return false; }
        }

        rs.close();

        conn.close();
        return true;
    }

    public String name() { return "PrepBatch"; }
    public String error() { return error; }
    public Exception ex() { return ex; }
}
