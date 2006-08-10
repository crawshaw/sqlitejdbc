package test;

import java.sql.*;

public class Speed02 implements Speed.Case
{
    public void run(String dbfile, boolean autoCommit) throws Exception {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:"+dbfile);

        Statement stat = conn.createStatement();
        stat.executeUpdate("create table speed02 (id);");

        conn.setAutoCommit(autoCommit);
        for (int i=0; i < 1000; i++)
            stat.executeUpdate("insert into speed02 values ('Hello world');");
        if (!autoCommit) conn.commit();

        conn.setAutoCommit(true);
        stat.executeUpdate("drop table speed02;");

        stat.close();
        conn.close();
    }
    public String name() { return "Insert1000"; }
}
