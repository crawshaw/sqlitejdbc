package test;

import org.sqlite.Function;
import java.sql.*;

public class Test23 implements Test.Case
{
    private String error;
    private Exception ex;

    public boolean run() throws Exception {
        Class.forName("org.sqlite.JDBC");

        Connection conn = DriverManager.getConnection("jdbc:sqlite:");
        conn.createStatement().execute("create table test(id);");
        PreparedStatement prep = conn.prepareStatement(
                "insert into test values(?);");

        for (int i=0; i < 10; i++) {
            prep.setInt(1, 1);
            prep.executeUpdate();
            prep.execute();
        }

        conn.close();
        return true;
    }

    public String name() { return "MultirunPrep"; }
    public String error() { return error; }
    public Exception ex() { return ex; }
}
