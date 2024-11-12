package cn.itcast.batch.sink;

import cn.itcast.bean.ItcastDataObj;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;


// 自定义 SinkFunction 将数据写入 MySQL
public  class MySQLSink extends RichSinkFunction<String[]> {

    // 日志打印
    private final static Logger logger = LoggerFactory.getLogger(MySQLSink.class);
    private static final String JDBC_URL = "jdbc:mysql://120.55.78.114:3306/test_data?characterEncoding=utf-8&useSSL=false";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "@Aabc939596";
    // "soc","mileage","speed","gps","terminalTime","processTime"
    private static final String INSERT_SQL = "INSERT INTO Trip_data (vin,eventminTime,soc,mileage,speed,gps,terminalTime) VALUES (?, ?, ?, ?,?,?,?)";


    private String tableName;

    public MySQLSink(String tableName) {
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
    public void invoke(String[] value, Context context) throws Exception {
        super.invoke(value, context);
        try {
            preparedStatement = connection.prepareStatement(INSERT_SQL);
            // set指定数据类型
            preparedStatement.setString(1, value[0]);
            preparedStatement.setString(2, value[1]);
            preparedStatement.setString(3, value[2]);
            preparedStatement.setString(4, value[3]);
            preparedStatement.setString(5, value[4]);
            preparedStatement.setString(6, value[5]);
            preparedStatement.setString(7, value[6]);

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}


