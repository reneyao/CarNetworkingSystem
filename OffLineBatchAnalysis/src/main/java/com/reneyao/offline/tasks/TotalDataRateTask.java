package com.reneyao.offline.tasks;

import com.reneyao.offline.dao.impl.TotalDataRate;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.io.jdbc.JDBCInputFormat;
import org.apache.flink.api.java.io.jdbc.JDBCOutputFormat;
import org.apache.flink.types.Row;

// hive写入到mysql的任务
public class TotalDataRateTask {
    public static void main(String[] args) {
        ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        // 离线环境不能设置checkpoint
        try {
            // 查询
            TotalDataRate totalDataRate = new TotalDataRate();
            JDBCInputFormat hiveJdbcInput = totalDataRate.getHiveJdbcInput();
            DataSet<Row> hiveDataSet = env.createInput(hiveJdbcInput);   // 转换
            hiveDataSet.collect().forEach(row -> {
                System.out.println("中间结果点查：srcTotalNum: " + row.getField(0) + ", errorTotalNum: " + row.getField(1));
            });
            // 计算
            DataSet<Row> hiveResultSet = totalDataRate.convertHiveDataSet(hiveDataSet);
            // 写入
            JDBCOutputFormat mysqlOutput = totalDataRate.getMysqlJdbcOutput();
            // hive结果数据输出到mysql结果表中
            hiveResultSet.output(mysqlOutput);

            // 执行
            env.execute("TotalDataRateTask");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

