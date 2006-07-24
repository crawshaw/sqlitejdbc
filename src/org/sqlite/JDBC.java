package org.sqlite;

import java.sql.*;
import java.util.*;

public class JDBC implements Driver
{
    static {
        try {
            DriverManager.registerDriver(new JDBC());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getMajorVersion() { return 1; }
    public int getMinorVersion() { return 1; }
    public boolean jdbcCompliant() { return false; }
    public boolean acceptsURL(String url) {
        return url.startsWith("jdbc:sqlite:");
    }
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info)
        throws SQLException {
        return new DriverPropertyInfo[] {};
    }

    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) return null;
        return new Conn(url.substring(12));
    }
}

