package cn.itcast.batch.task;

import cn.itcast.batch.sink.MySQLSinkTest;
import cn.itcast.bean.UserBehavior;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.connector.jdbc.JdbcStatementBuilder;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.util.Properties;

// 正确进入数据
public class MysqlSinkTest {
    public static void main(String[] args) throws Exception {
        //  读取kafka的数据源
        // 创建流处理环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 创建table环境
        EnvironmentSettings bsSettings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build();
        StreamTableEnvironment bsTableEnv = StreamTableEnvironment.create(env, bsSettings);

        // 开启checkpoint ：检查点，操作快照
        env.enableCheckpointing(30*1000);  // 30s为间隔
        env.getCheckpointConfig().

                setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);  // 保证每条数据消费一次，数据不重复消费
        // 设置两次checkpoint的时间间隔
        env.getCheckpointConfig().

                setMinPauseBetweenCheckpoints(20000);
        // 设置checkpoint的超时时间
        env.getCheckpointConfig().

                setCheckpointTimeout(120000);
        env.getCheckpointConfig().

                setMaxConcurrentCheckpoints(1);
        //设置checkpoint取消的时候，是否保留checkpoint，checkpoint默认会在job取消的时候删除
        env.getCheckpointConfig().

                enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
        //设置执行job过程中，保存检查点错误时，job不失败
        env.getCheckpointConfig().
                setTolerableCheckpointFailureNumber(1);  // 设置0，默认不容许失败

        // 设置重启策略：设置为不重启的策略
        env.setRestartStrategy(RestartStrategies.noRestart());

        // 创建flink消费Kafka数据的对象
        Properties props = new Properties();
        props.setProperty("bootstrap.servers","120.55.78.114:9092");
        props.setProperty("group.id","test5");   // 消费者的组id
        props.setProperty("auto.offset.reset","earliest");
        props.setProperty("enable.auto.commit","false");

        FlinkKafkaConsumer<String> vehicledata = new FlinkKafkaConsumer<>(
                "userBe",
                new SimpleStringSchema(),
                props
        );


        vehicledata.setStartFromEarliest();  // 从最开始消费
        //将kafka数据加入到数据源中
        DataStreamSource<String> streamSource = env.addSource(vehicledata);

        SingleOutputStreamOperator<UserBehavior> mapStream = streamSource.map(new MapFunction<String, UserBehavior>() {
            @Override
            public UserBehavior map(String value) throws Exception {
                String[] s = value.split(",");
                return new UserBehavior(s[0], s[1], s[2], s[3], Long.parseLong(s[4]) * 1000L);
            }
        });

        // 输入到mysql中
        //         写入到mysql中
//        mapStream.addSink(JdbcSink.sink(
//                "INSERT INTO user_behavior3  VALUES (?, ?, ?, ?, ?)",
//                (JdbcStatementBuilder<UserBehavior>) (ps, t) -> {
//                    ps.setString(1, t.getUserId());
//                    ps.setString(2, t.getItemId());
//                    ps.setString(3, t.getCategoryId());
//                    ps.setString(4, t.getBehavior());
//                    ps.setLong(5, t.getTs());
//                },
//                new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
//                        .withUrl("jdbc:mysql://120.55.78.114:3306/test_data?characterEncoding=utf-8&useSSL=false")
//                        .withDriverName("com.mysql.cj.jdbc.Driver")
//                        .withUsername("root")
//                        .withPassword("@Aabc939596")
//                        .build()
//        ));
        mapStream.addSink(new MySQLSinkTest("user_behavior4"));
        env.execute();

    }
}
