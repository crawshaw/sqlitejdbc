package test;

import java.sql.*;

public class Speed01 implements Speed.Case
{
    public void run(String dbfile, boolean autoCommit) throws Exception {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:"+dbfile);

        Statement stat = conn.createStatement();
        stat.executeUpdate("create table speed01 (id);");

        PreparedStatement prep = conn.prepareStatement(
            "insert into speed01 values (?);");

        conn.setAutoCommit(autoCommit);
        for (int i=0; i < 1000; i++) {
            prep.setString(1, "Hello World");
            prep.executeUpdate();
        }
        if (!autoCommit) conn.commit();
        prep.close();

        conn.setAutoCommit(true);
        stat.executeUpdate("drop table speed01;");

        stat.close();
        conn.close();
    }
    public String name() { return "PrepInsert1000"; }
}
