package com.sox.api.utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 主要用于构建数据库连接池
 */
public class ConnectionUtils {
    public int p_id; // 连接池标识，若为-1表示用完即闭连接
    public Long time;
    public Connection conn;

    public ConnectionUtils(Connection connection, int... id) {
        p_id = id.length > 0 ? id[0] : -1;
        time = System.currentTimeMillis() / 1000L;
        conn = connection;
    }

    public void setAutoCommit(boolean bool) throws SQLException {
        conn.setAutoCommit(bool);
    }

    public void close() throws SQLException {
        conn.close();
    }

    public Statement createStatement() throws SQLException {
        return conn.createStatement();
    }

    public void commit() throws SQLException {
        conn.commit();
    }
}
