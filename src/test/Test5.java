package test;

import java.sql.*;

public class Test5 implements Test.Case
{
    private String error;
    private Exception ex;

    public boolean run() throws Exception {
        int res;
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection(
            "jdbc:sqlite:build/test/test.db");

        ResultSet rs;
        PreparedStatement prepDrop = conn.prepareStatement(
            "DROP TABLE IF EXISTS Test5;");
        if (prepDrop.execute()) {
            error = "expected drop to return true";
            return false;
        }
        prepDrop.close();

        PreparedStatement prepCreate = conn.prepareStatement( 
            "CREATE TABLE Test5 (id);");
        if (prepCreate.execute()) {
            error = "expected create to return true";
            return false;
        }
        prepCreate.close();


        PreparedStatement prepInsert = conn.prepareStatement(
            "INSERT INTO Test5 VALUES (?);");
        PreparedStatement prepSelect = conn.prepareStatement(
            "SELECT id FROM Test5 WHERE id = ?;");

        prepInsert.setInt(1, 2);
        if ((res = prepInsert.executeUpdate()) != 1) {
            error = "expected result of 1 to update, got " + res;
            return false;
        }

        prepInsert.clearParameters();
        prepInsert.setString(1, "Hello World, this is a string.");
        if ((res = prepInsert.executeUpdate()) != 1) {
            error = "expected result of 1 to update, got " + res;
            return false;
        }

        prepInsert.clearParameters();
        prepInsert.setString(1, "Testing euro sign: \u20ac");
        if ((res = prepInsert.executeUpdate()) != 1) {
            error = "expected result of 1 to update, got " + res;
            return false;
        }

        prepInsert.clearParameters();
        prepInsert.setInt(1, 3);
        if ((res = prepInsert.executeUpdate()) != 1) {
            error = "expected result of 1 to update, got " + res;
            return false;
        }
        prepInsert.close();

        prepSelect.setInt(1, 2);
        rs = prepSelect.executeQuery();
        if (!rs.next() || rs.getInt(1) != 2) {
            error = "expected result with id=2";
            return false;
        }
        prepSelect.clearParameters();

        prepSelect.setInt(1, 3);
        rs = prepSelect.executeQuery();
        if (!rs.next() || rs.getInt(1) != 3) {
            error = "expected result with id=3";
            return false;
        }
        prepSelect.close();

        conn.close();
        return true;
    }

    public String name() { return "BasicPreparedUpdate"; }
    public String error() { return error; }
    public Exception ex() { return ex; }
}
