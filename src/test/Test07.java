package test;

import java.sql.*;

public class Test07 implements Test.Case
{
    private String error;
    private Exception ex;

    public boolean run() throws Exception {
        int res;
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection(
            "jdbc:sqlite:build/test/test.db");
        Statement stat = conn.createStatement();

        ResultSet rs;
        String msg;

        msg = null;
        try { rs = stat.executeQuery("SELECT * FROM NotThereTable;"); }
        catch (SQLException e) { msg = e.getMessage(); }
        if (!"SQLite error: no such table: NotThereTable".equals(msg)) {
            error = "incorrect error for missing table";
            return false;
        }

        msg = null;
        try { rs = stat.executeQuery("SELECT NotAColumn FROM People;"); }
        catch (SQLException e) { msg = e.getMessage(); }
        if (!"SQLite error: no such column: NotAColumn".equals(msg)) {
            error = "incorrect error for missing column";
            return false;
        }

        rs = stat.executeQuery("SELECT surname FROM People WHERE pid = 4;");

        msg = null;
        try { rs.getInt("notaname"); }
        catch (SQLException e) { msg = e.getMessage(); }
        if (!"no such column: 'notaname'".equals(msg)) {
            error = "incorrect error for getInt(notaname)";
            return false;
        }

        rs.close();
        stat.close();
        conn.close();

        return true;
    }

    public String name() { return "NonexistentNames"; }
    public String error() { return error; }
    public Exception ex() { return ex; }
}
