package test;

import org.sqlite.Function;
import java.sql.*;

public class Test21 implements Test.Case
{
    private static int val = 0;
    private static int gotTrigger = 0;

    private String error;
    private Exception ex;

    private boolean same(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) return false;
        for (int i=0; i < a.length; i++) if (a[i] != b[i]) return false;
        return true;
    }

    public boolean run() throws Exception {
        Class.forName("org.sqlite.JDBC");

        Connection conn = DriverManager.getConnection("jdbc:sqlite:");
        Statement stat = conn.createStatement();
        ResultSet rs;

        Function.create(conn, "f1", new Function() {
           public void xFunc() throws SQLException { val = 4; }
        });
        stat.executeQuery("select f1();").close();
        if (val != 4) { error = "f1 not called"; return false; }

        Function.destroy(conn, "f1");
        Function.destroy(conn, "f1");

        conn.close();

        return true;
    }

    public String name() { return "UserDefinedFunctionDestroy"; }
    public String error() { return error; }
    public Exception ex() { return ex; }
}
