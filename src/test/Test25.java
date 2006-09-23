package test;

import org.sqlite.Function;
import java.sql.*;

public class Test25 implements Test.Case
{
    private static final Date d1 = new Date(987654321);

    private String error;
    private Exception ex;

    public boolean run() throws Exception {

        Class.forName("org.sqlite.JDBC");

        PreparedStatement prep;
        ResultSet rs;

        Connection conn = DriverManager.getConnection("jdbc:sqlite:");
        Statement stat = conn.createStatement();

        stat.execute("create table test1 (c1);");
        stat.execute("create table test2 (c1);");

        prep = conn.prepareStatement("insert into test1 values(?);");
        prep.setDate(1, d1);
        prep.executeUpdate();

        rs = stat.executeQuery("select c1 from test1;");
        rs.next();
        if (rs.getLong(1) != d1.getTime()) {
            error="bad unix date"; return false; }
        if (!rs.getDate(1).equals(d1)) {
            error="bad java date"; return false; }
        rs.close();

        prep = conn.prepareStatement(
                "insert into test2 values (datetime(?, 'unixepoch'));");
        prep.setDate(1, d1);
        prep.executeUpdate();

        rs = stat.executeQuery("select strftime('%s', c1) from test2;");
        rs.next();
        if (rs.getLong(1) != d1.getTime()) {
            error = "bad transform unix date"; return false; }
        if (!rs.getDate(1).equals(d1)) {
            error = "bad transform java date"; return false; }

        conn.close();
        return true;
    }

    public String name() { return "DateAndTimes"; }
    public String error() { return error; }
    public Exception ex() { return ex; }
}
