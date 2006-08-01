package test;

import java.sql.*;
import java.util.*;

public class Test15 implements Test.Case
{
    private String error;
    private Exception ex;

    public boolean run() throws Exception {
        /* This test checks for a bug where a substring is read by the
         * driver as the full original string. Spotted by Oliver Randschau. */
        StringTokenizer st = new StringTokenizer("one two three");
        st.nextToken();
        String substr = st.nextToken();

        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TABLE Test15 (id);");

        PreparedStatement prep = conn.prepareStatement(
            "INSERT INTO Test15 VALUES (?);");
        prep.setString(1, substr);
        prep.executeUpdate();

        ResultSet rs = stat.executeQuery("SELECT id FROM Test15;");
        rs.next();
        String ret = rs.getString(1);

        if (!ret.equals(substr)) {
            error = "expected equality between: '"+ ret +"', '"+ substr +"'";
            return false;
        }

        conn.close();
        return true;
    }
    public String name() { return "Substrings"; }
    public String error() { return error; }
    public Exception ex() { return ex; }
}
