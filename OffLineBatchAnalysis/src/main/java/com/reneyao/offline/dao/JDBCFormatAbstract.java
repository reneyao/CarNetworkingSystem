package com.reneyao.offline.dao;

import org.apache.flink.api.java.io.jdbc.JDBCInputFormat;
import org.apache.flink.api.java.io.jdbc.JDBCOutputFormat;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.api.java.DataSet.*;



// 封装sql写入和读取
public abstract class JDBCFormatAbstract {          // 抽象类必须被继承，才能被使用

    public JDBCInputFormat getBatchJDBCInputFormat(String driverName, String url, String userName, String password, String inputSql, RowTypeInfo rowTypeInfo, int fetchSize) {
        return JDBCInputFormat.buildJDBCInputFormat()
                .setDrivername(driverName)
                .setDBUrl(url)
                .setUsername(userName)
                .setPassword(password)
                .setQuery(inputSql)
                .setRowTypeInfo(rowTypeInfo)
                .setFetchSize(fetchSize)                 // 分批处理
                .finish();
    }


    // 无分批处理
    public JDBCInputFormat getJDBCInputFormat(String driverName, String url, String userName, String password, String inputSql, RowTypeInfo rowTypeInfo) {
        return JDBCInputFormat.buildJDBCInputFormat()
                .setDrivername(driverName)
                .setDBUrl(url)
                .setUsername(userName)
                .setPassword(password)
                .setQuery(inputSql)
                .setRowTypeInfo(rowTypeInfo)
                .finish();
    }




    public JDBCOutputFormat getBatchJDBCOutputFormat(String driverName, String url, String userName, String password, String outputSql, int batchInterval) {
        return JDBCOutputFormat.buildJDBCOutputFormat()
                .setDrivername(driverName)
                .setDBUrl(url)
                .setUsername(userName)
                .setPassword(password)
                .setQuery(outputSql)
                .setBatchInterval(batchInterval)
                .finish();
    }



    /**
     * desc:获取JDBCOutputFormat
     * @param driverName
     * @param url
     * @param userName
     * @param password
     * @param outputSql
     * @return JDBCOutputFormat
     */
    public JDBCOutputFormat getJDBCOutputFormat(String driverName, String url, String userName, String password, String outputSql) {
        return JDBCOutputFormat.buildJDBCOutputFormat()
                .setDrivername(driverName)
                .setDBUrl(url)
                .setUsername(userName)
                .setPassword(password)
                .setQuery(outputSql)
                .finish();
    }





}
