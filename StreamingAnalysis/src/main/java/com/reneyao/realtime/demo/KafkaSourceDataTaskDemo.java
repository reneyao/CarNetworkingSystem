package com.reneyao.realtime.demo;


import com.reneyao.realtime.streaming.task.KafkaSourceDataTask;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.contrib.streaming.state.RocksDBStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;


import java.io.IOException;
import java.util.Properties;


public class KafkaSourceDataTaskDemo {

    public static void main(String[] args) throws Exception {

        // 加载conf.properties配置文件
        ParameterTool parameterTool = ParameterTool.fromPropertiesFile(KafkaSourceDataTaskDemo.class.getClassLoader().getResourceAsStream("conf.properties"));
        // 设置hadooop的操作用户
        System.setProperty("HADOOP_USER_NAME","rene");
        // 创建流处理环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 将parameterTool设置未全局参数
        env.getConfig().setGlobalJobParameters(parameterTool);
        // 设置按照事件时间处理数据
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        // 开启checkpoint ：检查点，操作快照
        // Flink 会在每 30 秒尝试创建一个检查点，但如果在两个检查点之间的时间小于 20 秒，则会等待达到 20 秒的最小间隔再创建下一个检查点。
        env.enableCheckpointing(30 * 1000);  // 30s为间隔
        env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);  // 保证每条数据消费一次，数据不重复消费
        // 设置两次checkpoint的时间间隔
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(20000);
        // 设置checkpoint的超时时间
        env.getCheckpointConfig().setCheckpointTimeout(20000);
        env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
        //设置checkpoint取消的时候，是否保留checkpoint，checkpoint默认会在job取消的时候删除
        env.getCheckpointConfig().enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
        //设置执行job过程中，保存检查点错误时，job不失败
        env.getCheckpointConfig().setFailOnCheckpointingErrors(false);
        //设置检查点存储的位置，使用rocksDBStateBackend，存储到本地+hdfs分布式文件，增量检查点
        String bashHdfsUri = parameterTool.getRequired("hdfsUri");  // 在配置文件中获得hadoop的uri
        try {
            // 将检查点数据报错到指定的hdfs的目录上
            env.setStateBackend(new RocksDBStateBackend(bashHdfsUri+"/flink/checkpoint/"+ KafkaSourceDataTask.class.getSimpleName()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 设置重启策略：设置为不重启的策略
        env.setRestartStrategy(RestartStrategies.noRestart());

        // 创建flink消费Kafka数据的对象
        // 设置各种配置
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", parameterTool.getRequired("bootstrap.servers"));
        props.setProperty("group.id", "test1" );   // 消费者的组id
        props.setProperty("flink.partition-discovery.interval-millis", "30000");
        props.setProperty("auto.offset.reset", parameterTool.get("auto.offset.reset", "earliest"));
        //不自动提交偏移量，交给flink的checkpoint处理哦
        props.setProperty("enable.auto.commit", parameterTool.get("enable.auto.commit", "false"));

        // 创建消费者实例 FlinkKafkaConsumer011使用于kafka0.11版本
        FlinkKafkaConsumer<String> vehicledata = new FlinkKafkaConsumer<>(
                // 读取配置文件中的topic parameterTool.getRequired("kafka.topic")
                "vehicledata",
                new SimpleStringSchema()   // 可以对简单的字符串进行序列化和反序列化，还可以自定义序列化和反序列化
                , props
        );
        // 设置自动提交offset到检查点
        vehicledata.setCommitOffsetsOnCheckpoints(true);
        // 加kafka消费者对象加入到数据源环境
        DataStreamSource<String> streamSource = env.addSource(vehicledata);
        // 打印输出
        streamSource.print();
        // 执行
        env.execute();

    }
}
