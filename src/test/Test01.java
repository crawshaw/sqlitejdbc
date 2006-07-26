package test;

import java.sql.*;

public class Test01 implements Test.Case
{
    private String error;
    private Exception ex;

    public boolean run() throws Exception {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            error = "org.sqlite.JDBC not found";
            return false;
        }

        try {
            Connection conn = DriverManager.getConnection(
                "jdbc:sqlite:build/test/test.db");
            conn.close();
        } catch (SQLException e) {
            return false;
        }

        return true;
    }
    public String name() { return "Connect"; }
    public String error() { return error; }
    public Exception ex() { return ex; }
}
