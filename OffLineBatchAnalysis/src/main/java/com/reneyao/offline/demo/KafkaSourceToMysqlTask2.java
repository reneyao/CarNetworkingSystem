package com.reneyao.offline.demo;


import com.reneyao.offline.utils.JsonParseUtil;
import com.reneyao.offline.bean.ItcastDataObj;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.util.Properties;

// 成功进数据了
public class KafkaSourceToMysqlTask2 {

    public static void main(String[] args) throws Exception {
        // 创建流处理环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
// 创建Table环境
        EnvironmentSettings bsSettings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build();
        StreamTableEnvironment bsTableEnv = StreamTableEnvironment.create(env, bsSettings);

// Kafka 配置
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", "120.55.78.114:9092");
        props.setProperty("group.id", "test4");
        props.setProperty("auto.offset.reset", "earliest");
        props.setProperty("enable.auto.commit", "false");

        FlinkKafkaConsumer<String> vehicledata = new FlinkKafkaConsumer<>(
                "vehiclejsondata",
                new SimpleStringSchema(),
                props
        );

        DataStreamSource<String> streamSource = env.addSource(vehicledata);
        SingleOutputStreamOperator<ItcastDataObj> itcastDataObjStream = streamSource.map(JsonParseUtil::parseJsonToObject);

        itcastDataObjStream.print("解析后的数据>>>");

// 直接使用JDBC连接写入MySQL  addSink
        itcastDataObjStream.addSink(JdbcSink.sink(
                "INSERT INTO vehicle_data VALUES (?, ?, ?)",
                // 让其去对应（依旧要对应每一个列
                (ps, t) -> {
                    ps.setString(1, t.getBattCoolActv());
                    ps.setString(2, t.getAbsActiveStatus());
                    ps.setString(3, t.getAccPedalValid());
                },
                JdbcExecutionOptions.builder()
                        .withBatchSize(10)
                        .build(),
                new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                        .withUrl("jdbc:mysql://120.55.78.114:3306/test_data?characterEncoding=utf-8&useSSL=false")
                        .withDriverName("com.mysql.cj.jdbc.Driver")
                        .withUsername("root")
                        .withPassword("@Aabc939596")
                        .build()
        ));

// 执行任务
        env.execute("Kafka to MySQL");

    }

}
