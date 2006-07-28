package test;

import java.sql.*;

public class Test13 implements Test.Case
{
    private String error;
    private Exception ex;

    public boolean run() throws Exception {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");

        Exception expect = null;
        try { conn.commit(); } catch (SQLException e) { expect = e; }
        if (expect == null) { error = "expected exception 1"; return false; }
        try { conn.rollback(); } catch (SQLException e) { expect = e; }
        if (expect == null) { error = "expected exception 2"; return false; }

        conn.setAutoCommit(false);
        try {
            conn.rollback();
        } catch (SQLException e) {
            error = "unexpected exception on rollback()"; return false;
        }
        try {
            conn.commit();
        } catch (SQLException e) {
            error = "unexpected exception on commit()"; return false;
        }

        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TEMP TABLE Test13 (c1);");
        conn.commit();
        stat.executeUpdate("INSERT INTO Test13 VALUES (1);");
        conn.rollback();
        stat.executeUpdate("INSERT INTO Test13 VALUES (2);");
        conn.commit();

        ResultSet rs = stat.executeQuery("SELECT COUNT(c1) FROM Test13;");
        if (!rs.next()) { error = "no results"; return false; }
        if (rs.getInt(1) != 1) {
            error = "wrong count: " + rs.getInt(1); return false; }
        rs.close();

        conn.rollback();
        stat.executeUpdate("INSERT INTO Test13 VALUES (3);");
        conn.setAutoCommit(true);

        rs = stat.executeQuery("SELECT COUNT(c1) FROM Test13;");
        if (!rs.next()) { error = "no results"; return false; }
        if (rs.getInt(1) != 2) { error = "bad state change"; return false; }
        rs.close();

        stat.close();
        conn.close();
        return true;
    }
    public String name() { return "Transactions"; }
    public String error() { return error; }
    public Exception ex() { return ex; }
}
