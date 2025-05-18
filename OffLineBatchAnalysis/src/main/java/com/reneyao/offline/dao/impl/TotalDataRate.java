package com.reneyao.offline.dao.impl;

import com.reneyao.offline.dao.JDBCFormatAbstract;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.operators.MapOperator;
import org.apache.flink.types.Row;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.io.jdbc.JDBCInputFormat;
import org.apache.flink.api.java.io.jdbc.JDBCOutputFormat;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.reneyao.offline.utils.OfflineDateUtil;

import java.util.UUID;


// 就是单纯计算一个数据的准确率和错误率
public class TotalDataRate extends JDBCFormatAbstract {
    private Logger logger = LoggerFactory.getLogger(TotalDataRate.class);

    /**
     * @return hive JDBCInputFormat
     * @desc:通过flin jdbc获得hive数据:查询
     */
    public JDBCInputFormat getHiveJdbcInput() {
        /** hive connect info*/
        String driverName = "org.apache.hive.jdbc.HiveDriver";
        // jdbc:hive2://hadoop102:10000/
        String url = "jdbc:hive2://hadoop102:10000/itcast_ods";
        String userName = "rene";
        String password = "123456";
        String hiveTableSrc = "itcast_src";
        String hiveTableError = "itcast_error";

        // 查询结果返回字段类型
        TypeInformation[] types = new TypeInformation[]{BasicTypeInfo.LONG_TYPE_INFO, BasicTypeInfo.LONG_TYPE_INFO};
        //查询结果返回字段
        String[] columName = new String[]{"srcTotalNum", "errorTotalNum"};
        RowTypeInfo rowTypeInfo = new RowTypeInfo(types, columName);
        // 每批次执行数据条数
//    String fetchSize = 10000;

        /** hive查询作为输入数据：查询itcast_src、itcast_error,再进行计算 */      // TODO 加入限定
        String sql = "SELECT (SELECT count(1) FROM " + hiveTableSrc + ") as srcTotalNum, (SELECT count(1) FROM " + hiveTableError + ") as errorTotalNum";
        logger.warn("Method:getBatchJDBCInputFormat input params is : 'inputSql:{}'\t'rowTypeInfo:{}'", sql, rowTypeInfo.toString());
//        return getBatchJDBCInputFormat(driverName, url, userName, password, sql, rowTypeInfo, fetchSize)
        return getJDBCInputFormat(driverName, url, userName, password, sql, rowTypeInfo);
    }

    /**
     * @return JDBCOutputFormat
     * @desc:获得mysql输出jdbc
     */
    public JDBCOutputFormat getMysqlJdbcOutput() {
        /** mysql连接信息*/
        String mysqlDriver = "com.mysql.cj.jdbc.Driver";
        String mysqlUrl = "jdbc:mysql://localhost:3306/mydb_test?characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true";
        String mysqlUserName = "root";
        String mysqlPassword = "123456";
        String mysqlTableDataRate = "itcast_data_rate";

//        int batchInterval = 1000;
        String sql = "insert into " + mysqlTableDataRate + "(series_no, src_total_num, error_total_num, data_accuracy, data_error_rate, process_date) values(?,?,?,?,?,?)";
        logger.warn("Method:getBatchJDBCOutputFormat output params is : 'outputSql:{}'", sql);
//        return getBatchJDBCOutputFormat(mysqlDriver,mysqlUrl,mysqlUserName,mysqlPassword, sql, batchInterval)
        return getJDBCOutputFormat(mysqlDriver, mysqlUrl, mysqlUserName, mysqlPassword, sql);
    }

    /**
     * 转换hive查询结果为最终计算结果
     *
     * @param hiveDataSet
     * @return DataSet[resultRow]
     */
    public DataSet<Row> convertHiveDataSet(DataSet<Row> hiveDataSet) {
        MapOperator<Row, Row> mapped = hiveDataSet.map(row -> {
            long srcTotalNum = Long.parseLong(row.getField(0).toString());
            long errorTotalNum = Long.parseLong(row.getField(1).toString());
            /** 计算结果:Row(随机id,srcTotalNum,errorTotalNum,srcTotalNum/(srcTotalNum+errorTotalNum),errorTotalNum/(srcTotalNum+errorTotalNum))*/
            Long dataAccuracy = srcTotalNum / (srcTotalNum + errorTotalNum);
            Row resultRow = new Row(6);        // 新建
            resultRow.setField(0, UUID.randomUUID().toString());
            resultRow.setField(1, srcTotalNum);
            resultRow.setField(2, errorTotalNum);
            resultRow.setField(3, dataAccuracy);
            resultRow.setField(4, 1 - dataAccuracy);
            resultRow.setField(5, OfflineDateUtil.getCurrentDate());
            return resultRow;
        });
        return mapped;
    }
}

