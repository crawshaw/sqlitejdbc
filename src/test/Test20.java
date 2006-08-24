package test;

import org.sqlite.Function;
import java.sql.*;

public class Test20 implements Test.Case
{
    private static int val = 0;
    private static byte[] b1 = new byte[] { 2, 5, -4, 8, -1, 3, -5 };
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

        // test calling
        Function.create(conn, "f1", new Function() {
           public void xFunc() throws SQLException { val = 4; }
        });
        stat.executeQuery("select f1();").close();
        if (val != 4) { error = "f1 not called"; return false; }

        // test returning
        Function.create(conn, "f2", new Function() {
            public void xFunc() throws SQLException { result(4); }
        });
        rs = stat.executeQuery("select f2();");
        if (!rs.next() || rs.getInt(1) != 4) { error ="f2 bad"; return false; }
        rs.close();

        for (int i=0; i < 20; i++) {
            rs = stat.executeQuery("select f2();");
            if (!rs.next() || rs.getInt(1) != 4) {
                error ="f2 bad in loop"; return false;
            }
            rs.close();
        }

        // test accessing arguments
        Function.create(conn, "f3", new Function() {
            public void xFunc() throws SQLException { result(value_int(0)); }
        });
        rs = stat.executeQuery("select f3(7);");
        if (!rs.next() || rs.getInt(1) != 7) { error ="f3 bad"; return false; }

        // test multiple arguments
        Function.create(conn, "f4", new Function() {
            public void xFunc() throws SQLException {
                int ret = 0;
                for (int i=0; i < args(); i++) ret += value_int(i);
                result(ret);
            }
        });
        rs.close(); rs = stat.executeQuery("select f4(2, 3, 9, -5);");
        if (!rs.next() || rs.getInt(1) != 9) { error ="f4 bad1"; return false; }
        rs.close(); rs = stat.executeQuery("select f4(2);");
        if (!rs.next() || rs.getInt(1) != 2) { error ="f4 bad2"; return false; }
        rs.close(); rs = stat.executeQuery("select f4(-3, -4, -5);");
        if (!rs.next() || rs.getInt(1)!=-12) { error ="f4 bad3"; return false; }


        // test different return types
        Function.create(conn, "f5", new Function() {
            public void xFunc() throws SQLException { result("Hello World"); }
        });
        rs.close(); rs = stat.executeQuery("select f5();");
        if (!rs.next() || !"Hello World".equals(rs.getString(1))) {
            error = "f5 bad"; return false; }

        Function.create(conn, "f6", new Function() {
            public void xFunc() throws SQLException { result(Long.MAX_VALUE); }
        });
        rs.close(); rs = stat.executeQuery("select f6();");
        if (!rs.next() || rs.getLong(1) != Long.MAX_VALUE) {
            error = "f6 bad"; return false; }

        Function.create(conn, "f7", new Function() {
            public void xFunc() throws SQLException {result(Double.MAX_VALUE);}
        });
        rs.close(); rs = stat.executeQuery("select f7();");
        if (!rs.next() || rs.getDouble(1) != Double.MAX_VALUE) {
            error = "f7 bad"; return false; }

        Function.create(conn, "f8", new Function() {
            public void xFunc() throws SQLException { result(b1); }
        });
        rs.close(); rs = stat.executeQuery("select f8();");
        if (!rs.next() || !same(b1, rs.getBytes(1))) {
            error = "f8 bad"; return false; }

        // TODO test different argument types
        Function.create(conn, "farg_int", new Function() {
            public void xFunc() throws SQLException { result(value_int(0)); }
        });
        PreparedStatement prep;

        prep = conn.prepareStatement("select farg_int(?);");
        prep.setInt(1, Integer.MAX_VALUE);
        rs = prep.executeQuery();
        if (!rs.next() || Integer.MAX_VALUE != rs.getInt(1)) {
            error = "bad int arg"; return false; }
        prep.close();

        Function.create(conn, "farg_long", new Function() {
            public void xFunc() throws SQLException { result(value_long(0)); }
        });
        prep = conn.prepareStatement("select farg_long(?);");
        prep.setLong(1, Long.MAX_VALUE);
        rs = prep.executeQuery();
        if (!rs.next() || Long.MAX_VALUE != rs.getLong(1)) {
            error = "bad long arg"; return false; }
        prep.close();

        Function.create(conn, "farg_double", new Function() {
            public void xFunc() throws SQLException { result(value_double(0));}
        });
        prep = conn.prepareStatement("select farg_double(?);");
        prep.setDouble(1, Double.MAX_VALUE);
        rs = prep.executeQuery();
        if (!rs.next() || Double.MAX_VALUE != rs.getDouble(1)) {
            error = "bad double arg"; return false; }
        prep.close();

        Function.create(conn, "farg_blob", new Function() {
            public void xFunc() throws SQLException { result(value_blob(0));}
        });
        prep = conn.prepareStatement("select farg_blob(?);");
        prep.setBytes(1, b1);
        rs = prep.executeQuery();
        if (!rs.next() || !same(b1, rs.getBytes(1))) {
            error = "bad bytes arg"; return false; }
        prep.close();

        Function.create(conn, "farg_string", new Function() {
            public void xFunc() throws SQLException { result(value_text(0));}
        });
        prep = conn.prepareStatement("select farg_string(?);");
        prep.setString(1, "Hello World");
        rs = prep.executeQuery();
        if (!rs.next() || !"Hello World".equals(rs.getString(1))) {
            error = "bad string arg"; return false; }
        prep.close();


        // test errors
        rs.close();
        Function.create(conn, "f9", new Function() {
            public void xFunc() throws SQLException {
                throw new SQLException("myErr"); }
        });
        String msg = null;
        try { stat.executeQuery("select f9();"); }
        catch (SQLException e) { msg = e.getMessage(); }
        if (!"java.sql.SQLException: myErr".equals(msg)) {
            System.out.println(msg);
            error = "bad error"; return false;
        }


        // test triggers
        Function.create(conn, "inform", new Function() {
            protected void xFunc() throws SQLException {
                gotTrigger = value_int(0); }
        });
        stat.executeUpdate("create table trigtest (c1);");
        stat.executeUpdate(
            "create trigger trigt after insert on trigtest"
            + " begin select inform(new.c1); end;"
        );
        stat.executeUpdate("insert into trigtest values (5);");
        if (gotTrigger != 5) { error = "bad trigger"; return false; }


        // test aggregate functions
        Function.create(conn, "mySum", new Function.Aggregate() {
            private int val = 0;
            protected void xStep() throws SQLException {
                for (int i=0; i < args(); i++) val += value_int(i);
            }
            protected void xFinal() throws SQLException {
                result(val);
            }
        });
        stat.executeUpdate("create table tobesummed (c1);");
        stat.executeUpdate("insert into tobesummed values (5);");
        stat.executeUpdate("insert into tobesummed values (3);");
        stat.executeUpdate("insert into tobesummed values (8);");
        stat.executeUpdate("insert into tobesummed values (2);");
        stat.executeUpdate("insert into tobesummed values (7);");
        rs = stat.executeQuery("select mySum(c1), sum(c1) from tobesummed;");
        if (!rs.next() || rs.getInt(1) != rs.getInt(2)) {
            error = "bad aggregate"; return false; }


        conn.close();
        return true;
    }

    public String name() { return "UserDefinedFunction"; }
    public String error() { return error; }
    public Exception ex() { return ex; }
}
