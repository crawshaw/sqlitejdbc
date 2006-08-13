package test;

import java.sql.*;

public class Test12 implements Test.Case
{
    private String error;
    private Exception ex;

    private boolean same(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) return false;
        for (int i=0; i < a.length; i++) if (a[i] != b[i]) return false;
        return true;
    }

    public boolean run() throws Exception {
        int res;
        byte[] origbytes = new byte[] {1,2,3, 1,2,3, 4,5,6, 4,5,6, 8,8,8};

        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:");

        Statement stat = conn.createStatement();
        stat.executeUpdate(
            "create temp table test12 ("
            + "c1 integer, c2 float, c3, c4 varchar, c5 bit, c6, c7);");

        PreparedStatement prep = conn.prepareStatement(
            "insert into test12 values (?,?,?,?,?,?,?);");

        prep.setInt    (1, Integer.MAX_VALUE);
        prep.setFloat  (2, Float.MAX_VALUE);
        prep.setDouble (3, Double.MAX_VALUE);
        prep.setLong   (4, Long.MAX_VALUE);
        prep.setBoolean(5, false);
        prep.setByte   (6, (byte)7);
        prep.setBytes  (7, origbytes);
        prep.executeUpdate();

        ResultSet rs = stat.executeQuery(
            "SELECT c1,c2,c3,c4,c5,c6,c7 FROM Test12;");
        if (!rs.next()) { error = "expected result"; return false; }

        if (rs.getInt(1) != Integer.MAX_VALUE) {
            error = "bad int"; return false; }
        if (rs.getFloat(2) != Float.MAX_VALUE) {
            error = "bad float"; return false; }
        if (rs.getDouble(3) != Double.MAX_VALUE) {
            error = "bad double"; return false; }
        if (rs.getLong(4) != Long.MAX_VALUE) {
            error="bad long"; return false; }
        if (rs.getBoolean(5)) {
            error = "bad boolean"; return false; }
        if (rs.getByte(6) != (byte)7) {
            error = "bad byte"; return false; }
        if (!same(rs.getBytes(7), origbytes)) {
            error = "bad bytes"; return false; }

        Object o;

        o = rs.getObject(1);
        if (o == null || !(o instanceof Integer)
                || ((Integer)o).intValue() != Integer.MAX_VALUE) {
            error = "bad object int"; return false;
        }
        o = rs.getObject(2);
        if (o == null || !(o instanceof Double)
                || ((Double)o).floatValue() != Float.MAX_VALUE) {
            error = "bad object float"; return false;
        }
        o = rs.getObject(3);
        if (o == null || !(o instanceof Double)
                || ((Double)o).doubleValue() != Double.MAX_VALUE) {
            error = "bad object double"; return false;
        }
        o = rs.getObject(4);
        if (o == null || !(o instanceof String)
                || Long.parseLong((String)o) != Long.MAX_VALUE) {
            error = "bad object long"; return false;
        }
        o = rs.getObject(5);
        if (o == null || !(o instanceof Integer)
                || ((Integer)o).intValue() != 0) {
            error = "bad object boolean"; return false;
        }
        o = rs.getObject(6);
        if (o == null || !(o instanceof Integer)
                || ((Integer)o).byteValue() != (byte)7) {
            error = "bad object byte"; return false;
        }
        o = rs.getObject(7);
        if (o == null || !(o instanceof byte[])
                || !same(origbytes, (byte[])o)) {
            error = "bad object bytes"; return false;
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
