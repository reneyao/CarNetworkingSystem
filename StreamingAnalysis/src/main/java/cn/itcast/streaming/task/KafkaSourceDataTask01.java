package cn.itcast.streaming.task;

// 这个程序是写到hdfs中
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.contrib.streaming.state.RocksDBStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
// 导包
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;

import java.io.IOException;
import java.util.Properties;

/**
 * 完成从kafka在集群中读取数据车辆的json数据转化为ItCastDataObj
 * 需求：原始数据ETL操作
 * flink消费kafka数据，将消费出来的数据进行转换、清洗、过滤以后，正常的数据需要写入到hbase和hdfs，异常的数据写入到hdfs中
 * 1）正常数据写入hdfs和hbase
 * 2）异常数据写入到hbase
 */
public class KafkaSourceDataTask01 {
    /**
     * 入口方法
     * @param args
     */
    public static void main(String[] args) throws Exception {
        //todo 1.创建流执行环境
        //todo 2.设置并行度 ①配置文件并行度设置 ②客户端设置 flink run -p 2 ③在程序中 env.setParallel(2) ④算子上并行度（级别最高）
        // 设置checkpoint
        //todo 3.开启checkpoint及相应的配置，最大容忍次数，最大并行checkpoint个数，checkpoint间最短间隔时间，checkpoint的最大
        //todo 容忍的超时时间，checkpoint如果取消是否删除checkpoint 等
        //todo 4.开启重启策略
        //todo 5. 读取kafka中的数据
        //todo 5.1 设置 FlinkKafkaConsumer
        //todo 5.2 配置参数
        //todo 5.3 消费 kafka 的offset 提交给 flink 来管理
        //todo 6 env.addSource
        //todo 7 打印输出
        //todo 8 将读取出来的 json 字符串转换成 ItcastDataObj
        //todo 9 将数据拆分成正确的数据和异常的数据
        //todo 10 将正确的数据保存到 hdfs
        //todo 11 将错误的数据保存到 hdfs 上
        //todo 12 将正确的数据写入到 hbase 中
        //todo 13 执行flink程序
        //加载conf.properties配置文件，返回ParameterTool工具类对象（读取配置文件
        ParameterTool parameterTool = ParameterTool.fromPropertiesFile(KafkaSourceDataTask01.class.getClassLoader().getResourceAsStream("conf.properties"));

        //TODO 1）初始化flink流式处理的开发环境
        System.setProperty("HADOOP_USER_NAME", "rene");  // 用户名
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        //设置全局的参数
        env.getConfig().setGlobalJobParameters(parameterTool);          // 让其在任意的文件都可以读取到
        //TODO 2）设置按照事件时间处理数据（划分窗口或者添加水印都需要事件时间）
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);   // 事件时间
        //TODO 3）开启checkpoint
        //TODO 3.1：设置每隔30秒钟开启checkpoint
        env.enableCheckpointing(30*1000);
        //TODO 3.2：设置检查点的model，exactly-once，保证数据消费一次，数据不重复消费
        env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
        //TODO 3.3：设置两次checkpoint时间间隔，避免两次间隔太近导致频繁checkpoint而出现业务处理能力下降
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(20000);
        //TODO 3.4：设置checkpoint的超时时间
        env.getCheckpointConfig().setCheckpointTimeout(20000);
        //TODO 3.5：设置checkpoint最大的尝试次数，同一个时间有几个checkpoint并行执行
        env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);    // 最大的checkpoint的并发度
        //TODO 3.6：设置checkpoint取消的时候，是否保留checkpoint，checkpoint默认会在job取消的时候删除
        // 设置停止任务后，依旧保留checkpoint
        env.getCheckpointConfig().enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
        //TODO 3.7：设置执行job过程中，保存检查点错误时，job不失败
        env.getCheckpointConfig().setFailOnCheckpointingErrors(false);
        //TODO 3.8：设置检查点存储的位置，使用rocksDBStateBackend，存储到本地+hdfs分布式文件，增量检查点（注意，我使用的是高可用集群）
        String bashHdfsUri = parameterTool.getRequired("hdfsUri");     // 得到hdfs的基础路径
        try {
            // 给出具体的路径，而getSimpleName()是获得当前程序的名字——区分不同程序的目录
            env.setStateBackend(new RocksDBStateBackend(bashHdfsUri+"/flink/checkpoint/"+KafkaSourceDataTask01.class.getSimpleName()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //TODO 4）设置任务的重启策略（固定延迟重启策略、失败率重启策略、无重启策略）
        env.setRestartStrategy(RestartStrategies.noRestart()); // 设置的无重启策略

        //TODO 5）创建flink消费kafka数据的对象，指定kafka的参数信息
        Properties props = new Properties();
        //TODO     5.1：设置kafka集群地址
        props.setProperty("bootstrap.servers", parameterTool.getRequired("bootstrap.servers"));
        //TODO     5.2：设置消费者组id
        // 设置消费者组id
        props.setProperty("group.id", "KafkaSourceDataTask01");         // 组id
        //TODO     5.3：设置kafka的分区感知（动态监测）
        props.setProperty("flink.partition-discovery.interval-millis", "30000"); // 设置分区发现的时间
        //TODO     5.5：设置自动递交offset位置策略
        props.setProperty("auto.offset.reset", parameterTool.get("auto.offset.reset", "earliest"));
        //5.不自动提交偏移量，交给flink的checkpoint处理哦
        props.setProperty("enable.auto.commit", parameterTool.get("enable.auto.commit", "false"));

        // 开始消费数据
        //TODO     5.6：创建kafka的消费者实例
        // 需要创建kafka消费数据
        FlinkKafkaConsumer<String> kafkaConsumer = new FlinkKafkaConsumer<String>(
                parameterTool.getRequired("kafka.topic"),
                new SimpleStringSchema(), props
        );
        //TODO     5.7：设置自动递交offset保存到检查点
        kafkaConsumer.setCommitOffsetsOnCheckpoints(true);

        //TODO 6）将kafka消费者对象添加到环境中
        DataStream<String> dataStreamSource = env.addSource(kafkaConsumer);       // addSource

        //打印输出测试(能正确消费到数据）
        dataStreamSource.print();

        //TODO 13）启动作业，运行任务
        env.execute();
    }
}