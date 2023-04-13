package net.lz1998.pbbot.common.utills.Db;


import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DbUtils {

    private DbUtils() {
    }

    private static String url = "";
    private static String username = "";
    private static String password = "";

    public static void setUrl(String url1, String username1, String password1) {
        url = url1;
        username = username1;
        password = password1;
    }

    public static void executeUpdateSQL(String sql) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = DbUtils.getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.executeLargeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            //关闭连接
            close(preparedStatement, connection);
        }
    }

    public static List<Long> listKeyId(String sql) {
        return listLongId(sql, "key_id");
    }

    public static List<Long> listLongId(String sql, String columnName) {
        List<Long> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet;
        try {
            connection = DbUtils.getConnection();
            preparedStatement = connection.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                //获取keyID
                list.add(resultSet.getLong(columnName));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            //关闭连接
            close(preparedStatement, connection);
        }
        return list;
    }

    public static <T> List<T> listObject(String sql, String columnName, Class<T> type) {
        List<T> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet;
        try {
            connection = DbUtils.getConnection();
            preparedStatement = connection.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                //获取keyID
                list.add(resultSet.getObject(columnName, type));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            //关闭连接
            close(preparedStatement, connection);
        }
        return list;
    }

    /**
     *             Map map = new HashMap();
     *             map.put("application_code",String.class);
     *             map.put("product_id",Integer.class);
     *             map.put("phone_number",String.class);
     * @param sql
     * @param map
     * @return
     */
    public static List<Map> listMap(String sql, Map<String,Class> map) {
        List<Map> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet;
        try {
            connection = DbUtils.getConnection();
            preparedStatement = connection.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                Map hashMap = new HashMap();
                for (Map.Entry<String,Class> entry:map.entrySet()) {
                    Object object = resultSet.getObject(entry.getKey(), entry.getValue());
                    hashMap.put(entry.getKey(),object);
                }
                //获取keyID
                list.add(hashMap);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            //关闭连接
            close(preparedStatement, connection);
        }
        return list;
    }

    /**
     * 获取数据库链接
     *
     * @return
     */
    private static Connection getConnection() {
        Connection connection;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(url, username, password);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return connection;
    }

    /**
     * 关闭连接
     *
     * @param preparedStatement
     * @param con
     */
    public static void close(PreparedStatement preparedStatement, Connection con) {
        if (null != preparedStatement) {
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                if (null != con) {
                    try {
                        con.close();
                    } catch (SQLException e) {
                        System.err.println(e.getLocalizedMessage());
                    }
                }
            }
        }
    }
}
