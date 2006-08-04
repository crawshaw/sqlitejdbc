package test;

import java.sql.*;
import java.util.*;

public class Test16 implements Test.Case
{
    private String error;
    private Exception ex;

    public boolean run() throws Exception {
        String utf01 = "\uD840\uDC40";
        String utf02 = "\uD840\uDC47 ";
        String utf03 = " \uD840\uDC43";
        String utf04 = " \uD840\uDC42 ";
        String utf05 = "\uD840\uDC40\uD840\uDC44";
        String utf06 = "Hello World, \uD840\uDC40 \uD880\uDC99";
        String utf07 = "\uD840\uDC41 testing \uD880\uDC99";
        String utf08 = "\uD840\uDC40\uD840\uDC44 testing";

        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TABLE Test16 (c1,c2,c3,c4,c5,c6,c7,c8);");

        PreparedStatement prep = conn.prepareStatement(
            "INSERT INTO Test16 VALUES (?,?,?,?,?,?,?,?);");
        prep.setString(1, utf01); prep.setString(2, utf02);
        prep.setString(3, utf03); prep.setString(4, utf04);
        prep.setString(5, utf05); prep.setString(6, utf06);
        prep.setString(7, utf07); prep.setString(8, utf08);
        prep.executeUpdate();

        ResultSet rs = stat.executeQuery("SELECT * FROM Test16;");
        rs.next();

        if (!utf01.equals(rs.getString(1))) { error = "utf01"; return false; }
        if (!utf02.equals(rs.getString(2))) { error = "utf02"; return false; }
        if (!utf03.equals(rs.getString(3))) { error = "utf03"; return false; }
        if (!utf04.equals(rs.getString(4))) { error = "utf04"; return false; }
        if (!utf05.equals(rs.getString(5))) { error = "utf05"; return false; }
        if (!utf06.equals(rs.getString(6))) { error = "utf06"; return false; }
        if (!utf07.equals(rs.getString(7))) { error = "utf07"; return false; }
        if (!utf08.equals(rs.getString(8))) { error = "utf08"; return false; }

        conn.close();
        return true;
    }
    public String name() { return "UTF16SurrogatePairs"; }
    public String error() { return error; }
    public Exception ex() { return ex; }
}
