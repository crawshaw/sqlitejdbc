package test;

import java.sql.*;

public class Test2 implements Test.Case
{
    private String error;
    private Exception ex;

    public boolean run() throws Exception {
        int res;
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection(
            "jdbc:sqlite:build/test/test.db");

        Statement stat = conn.createStatement();
        if ((res = stat.executeUpdate("DROP TABLE IF EXISTS Test2;")) != 0) {
            error = "unexpected result " + res + " from drop statement";
            return false;
        }
                
        if ((res = stat.executeUpdate("CREATE TABLE Test2 (id);")) != 0) {
            error = "unexpected result " + res + " from create statement";
            return false;
        }

        if ((res = stat.executeUpdate("INSERT INTO Test2 VALUES(0);")) != 1) {
            error = "unexpected result " + res + " from insert statement";
            return false;
        }

        if ((res = stat.executeUpdate("UPDATE Test2 SET id = 1")) != 1) {
            error = "unexpected result " + res + " from update statement";
            return false;
        }

        stat.close();
        conn.close();
        return true;
    }
    public String name() { return "BasicExecuteUpdate"; }
    public String error() { return error; }
    public Exception ex() { return ex; }
}
