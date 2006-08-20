package test;

import java.sql.*;

public class Test05 implements Test.Case
{
    private String error;
    private Exception ex;

    public boolean run() throws Exception {
        int res;
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:");

        PreparedStatement prepCreate, prepInsert, prepSelect, prep;
        ResultSet rs;

        prepCreate = conn.prepareStatement("create table Test5 (id);");
        if (prepCreate.execute()) { error = "expected create"; return false; }
        prepCreate.close();

        prepInsert = conn.prepareStatement("insert into Test5 values (?);");
        prepSelect = conn.prepareStatement("select id from Test5 where id=?;");

        prepInsert.setInt(1, 2);
        if ((res = prepInsert.executeUpdate()) != 1) {
            error = "expected res 1 to update, got " + res; return false; }

        prepInsert.setString(1, "Hello World, this is a string.");
        if ((res = prepInsert.executeUpdate()) != 1) {
            error = "expected res 1 (b) to update, got " + res; return false; }

        prepInsert.clearParameters();
        prepInsert.setString(1, "Testing euro sign: \u20ac");
        if ((res = prepInsert.executeUpdate()) != 1) {
            error = "expected res 1 (c) to update, got " + res; return false; }

        prepInsert.clearParameters();
        prepInsert.setInt(1, 3);
        if ((res = prepInsert.executeUpdate()) != 1) {
            error = "expected res 1 (d) to update, got " + res; return false; }
        prepInsert.close();


        prepSelect.setInt(1, 2);
        rs = prepSelect.executeQuery();
        if (!rs.next() || rs.getInt(1) != 2) {
            error = "bad res with id=2"; return false; }
        rs.close();

        prepSelect.setInt(1, 3);
        rs = prepSelect.executeQuery();
        if (!rs.next() || rs.getInt(1) != 3) {
            error = "bad res with id=3"; return false; }
        prepSelect.close();


        prep = conn.prepareStatement("select ?, ? ,?;");
        prep.setString(1, "one");
        prep.setString(2, "two");
        prep.setString(3, null);
        rs = prep.executeQuery();
        if (!rs.next()) { error = "bad rs next"; return false; }
        if (!"one".equals(rs.getString(1))) { error="bad rs 1"; return false; }
        if (!"two".equals(rs.getString(2))) { error="bad rs 2"; return false; }
        if (rs.getString(3) != null) { error = "bad rs 3"; return false; }

        conn.close();
        return true;
    }

    public String name() { return "BasicPreparedUpdate"; }
    public String error() { return error; }
    public Exception ex() { return ex; }
}
