package com.reneyao.offline.demo.batch.task;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import static org.apache.flink.table.api.Expressions.$;               // 解析符号


/**
 * demo：展示StreamTableEnvironment的使用，flink1.11之后，流/批处理的table api/sql api的调用都是用这个环境
 * 展示了table api和sql api使用的不同
 * sql api的sql执行，主要通过executeSql()来执行
 * flink纯sql要使用临时视图
 */

public class FlinkTableAndSqlDemo1 {

    public static void main(String[] args) throws Exception {
        // 1. 创建 Flink 环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        // 2. 使用 Table API 进行流数据处理
        DataStream<Tuple2<String, Integer>> stream = env.fromElements(
                Tuple2.of("Alice", 25),
                Tuple2.of("Bob", 30),
                Tuple2.of("Alice", 28)
        );

        // 将 DataStream 转换为 Table
        Table table = tableEnv.fromDataStream(stream, $("name"), $("age"));

        // 使用 Table API 进行数据处理
        Table resultTable = table
                .groupBy($("name"))
                .select($("name"), $("age").sum().as("total_age"));

        // 3. 注册临时视图，方便使用 SQL 查询
        tableEnv.createTemporaryView("name_age_table", resultTable);

        // 4. 使用 SQL API 执行查询
        String sqlQuery = "SELECT name, total_age FROM name_age_table WHERE total_age > 50";
        // 使用方法sqlQuery()/executeSql()来执行sql
        Table result = tableEnv.sqlQuery(sqlQuery);

        // 5. 执行查询并打印结果
        tableEnv.toAppendStream(result, Tuple2.class).print();

        // 6. 启动 Flink 程序
        env.execute();
    }
}

