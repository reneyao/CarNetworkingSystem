package cn.itcast.offline.task;

import cn.itcast.offline.window.function.DriveSampleWindowFunction;
import cn.itcast.offline.window.udfWatermark.TripDriveWatermark;
import cn.itcast.bean.ItcastDataObj;
import cn.itcast.utils.JsonParseUtil;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.EventTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.util.Properties;


// 排错思路：事件时间赋值问题，自定义窗口函数逻辑，mysql的类型对应问题
public class TripDriveBatchTask {

    public static void main(String[] args) throws Exception {
        // 读取kafka的数据源
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

                setMinPauseBetweenCheckpoints(120000);
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
                "vehiclejsondata",
                new SimpleStringSchema(),
                props
        );


     vehicledata.setStartFromEarliest();  // 从最开始消费
         //将kafka数据加入到数据源中
        DataStreamSource<String> streamSource = env.addSource(vehicledata);
        // 输出测试
       streamSource.print();  // 消费是没有问题的

        SingleOutputStreamOperator<ItcastDataObj> map = streamSource.map(JsonParseUtil::parseJsonToObject);// 没有问题
        SingleOutputStreamOperator<ItcastDataObj> filter = map.filter(r -> 2 == r.getChargeStatus() || 3 == r.getChargeStatus());
        filter.assignTimestampsAndWatermarks(new TripDriveWatermark())
                // window 和apply这一块有问题----> 自定义的windowFunction函数有问题
                .keyBy(r -> r.getVin()).window(EventTimeSessionWindows.withGap(Time.minutes(15)))
                .apply(new DriveSampleWindowFunction()).print();
        //        SingleOutputStreamOperator<String[]> processedStream = streamSource.map(JsonParseUtil::parseJsonToObject).
//                // ChargeStatus  充电状态
//                        filter(r-> 2 == r.getChargeStatus() || 3 == r.getChargeStatus())
//                // 最大的乱序时间是15分钟  此处加泛型，确保数据的统一
//                .assignTimestampsAndWatermarks(WatermarkStrategy.<ItcastDataObj>forBoundedOutOfOrderness(Duration.ofSeconds(30))
//                        .withTimestampAssigner(new SerializableTimestampAssigner<ItcastDataObj>() {
//                            @Override
//                            public long extractTimestamp(ItcastDataObj element, long recordTimestamp) {
//                                Long currentMaxTimestamp = 0L;
//                                System.out.println(element.getTerminalTimeStamp().toString());
//                                currentMaxTimestamp = Math.max(element.getTerminalTimeStamp(), currentMaxTimestamp);
//                                return currentMaxTimestamp;       // 防止有小于0的时间戳的异常数据
//
//                            }
//                        })
//                ).keyBy(ItcastDataObj::getVin).window(EventTimeSessionWindows.withGap(Time.minutes(15)))
//                // 窗口函数（也可以使用process来达到计算的目的
//                // 研究一下apply等窗口函数   apply 是旧版的
//                // 自定义的函数的结果是String[]类型
//                .apply(new DriveSampleWindowFunction());

//         写入到mysql中
//        processedStream.addSink(JdbcSink.sink(
//                "INSERT INTO vehicle_data (vin, timestamp) VALUES (?, ?, ?, ?, ?)",
//                (JdbcStatementBuilder<ItcastDataObj>) (ps, t) -> {
//                    ps.setString(1, t.getVin());
//                    ps.setLong(2, t.getTerminalTimeStamp());
//                },
//                new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
//                        .withUrl("jdbc:mysql://120.55.78.114:3306/test_data")
//                        .withDriverName("com.mysql.cj.jdbc.Driver")
//                        .withUsername("root")
//                        .withPassword("@Aabc939596")
//                        .build()
//        ));
//        processedStream.print();
        //使用mysql sink
//      processedStream.addSink(new MySQLSink("aa"));
        // 执行
        env.execute();


    }
}
