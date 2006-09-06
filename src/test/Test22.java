package test;

import org.sqlite.Function;
import java.sql.*;

public class Test22 implements Test.Case
{
    private String error;
    private Exception ex;

    public boolean run() throws Exception {
        Class.forName("org.sqlite.JDBC");

        Connection conn = DriverManager.getConnection("jdbc:sqlite:");
        //Statement stat = conn.createStatement();

        conn.createStatement().executeUpdate("pragma auto_vacuum=0;");
        conn.createStatement().executeQuery("select null;");
        conn.prepareStatement("select ?;").setString(1, "Hello World");
        conn.prepareStatement("select null;").close();
        conn.prepareStatement("select null;");
        conn.prepareStatement("select null;");
        conn.close();
        conn.close();
        return true;
    }

    public String name() { return "ClosePrepStatement"; }
    public String error() { return error; }
    public Exception ex() { return ex; }
}
