package test;

import java.sql.*;

public class Test12 implements Test.Case
{
    private String error;
    private Exception ex;

    public boolean run() throws Exception {
        int res;
        byte[] origbytes = new byte[] {1,2,3, 1,2,3, 4,5,6, 4,5,6, 8,8,8};

        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");

        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TEMP TABLE Test12 (c1,c2,c3,c4,c5,c6,c7);");

        PreparedStatement prep = conn.prepareStatement(
            "INSERT INTO Test12 VALUES (?,?,?,?,?,?,?);");

        prep.setInt    (1, 99);
        prep.setFloat  (2, 54.77F);
        prep.setDouble (3, 43.44D);
        prep.setLong   (4, Long.MAX_VALUE);
        prep.setBoolean(5, false);
        prep.setByte   (6, (byte)7);
        prep.setBytes  (7, origbytes);
        prep.executeUpdate();

        ResultSet rs = stat.executeQuery(
            "SELECT c1,c2,c3,c4,c5,c6,c7 FROM Test12;");
        if (!rs.next()) { error = "expected result"; return false; }
        if (rs.getInt(1) != 99) { error = "bad int"; return false; }
        if (rs.getFloat(2) != 54.77F) { error = "bad float"; return false; }
        if (rs.getDouble(3) != 43.44D) { error = "bad double"; return false; }
        if (rs.getLong(4) != Long.MAX_VALUE) { error="bad long"; return false; }
        if (rs.getBoolean(5)) { error = "bad boolean"; return false; }
        if (rs.getByte(6) != (byte)7) { error = "bad byte"; return false; }
        byte[] getbytes = rs.getBytes(7);
        if (origbytes.length != getbytes.length) {
            error = "bad bytes length, got " + getbytes.length; return false;
        }
        for (int i=0; i < origbytes.length; i++) {
            if (origbytes[i] != getbytes[i]) {
                error = "bad bytes["+i+"]"; return false;
            }
        }

        rs.close();
        stat.close();
        conn.close();
        return true;
    }
    public String name() { return "GetAndSetTypes"; }
    public String error() { return error; }
    public Exception ex() { return ex; }
}
