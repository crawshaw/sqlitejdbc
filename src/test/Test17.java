package test;

import java.sql.*;
import java.util.*;

public class Test17 implements Test.Case
{
    private String error;
    private Exception ex;

    public boolean run() throws Exception {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:");
        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TABLE Test17 (c1,c2,c3,c4,c5,c6,c7,c8);");

        PreparedStatement prep = conn.prepareStatement(
            "INSERT INTO Test17 VALUES (?,?,?,?,?,?,?,?);");

        ParameterMetaData meta = prep.getParameterMetaData();
        if (meta.getParameterCount() != 8) {
            error = "param count != 8"; return false;
        }

        conn.close();
        return true;
    }
    public String name() { return "ParameterMetaData"; }
    public String error() { return error; }
    public Exception ex() { return ex; }
}
