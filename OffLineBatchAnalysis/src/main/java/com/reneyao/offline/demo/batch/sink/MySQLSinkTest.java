package com.reneyao.offline.demo.batch.sink;

import com.reneyao.offline.bean.UserBehavior;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;


// 自定义 SinkFunction 将数据写入 MySQL (两种方法数据都可以正常进入
public  class MySQLSinkTest extends RichSinkFunction<UserBehavior> {

    // 日志打印
    private final static Logger logger = LoggerFactory.getLogger(MySQLSink.class);
    private static final String JDBC_URL = "jdbc:mysql://120.55.78.114:3306/test_data?characterEncoding=utf-8&useSSL=false";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "@Aabc939596";
    // "soc","mileage","speed","gps","terminalTime","processTime"
    private static final String INSERT_SQL = "INSERT INTO user_behavior4  VALUES (?, ?, ?, ?, ?)";


    private String tableName;

    public MySQLSinkTest(String tableName) {
        this.tableName = tableName;
    }

    Connection connection = null;
    PreparedStatement preparedStatement = null;
    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        // 连接mysql
        connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);

        logger.warn("获得mysql的连接对象，{}表对象初始化成功！", tableName);
    }

    @Override
    public void close() throws Exception {
        // 关闭资源
        super.close();
        preparedStatement.close();
        connection.close();

    }

    @Override
    public void invoke(UserBehavior value, Context context) throws Exception {
        super.invoke(value, context);
        try {
            preparedStatement = connection.prepareStatement(INSERT_SQL);
            // set指定数据类型
            preparedStatement.setString(1, value.getUserId());
            preparedStatement.setString(2, value.getItemId());
            preparedStatement.setString(3, value.getCategoryId());
            preparedStatement.setString(4, value.getBehavior());
            preparedStatement.setLong(5, value.getTs());

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}


