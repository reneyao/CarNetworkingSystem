package cn.itcast;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import java.util.Properties;

// 数据采集之kafka消费数据
public class FlinkKafkaReader {
    public static void main(String[] args) throws Exception {
        //从控制台获取参数列表
        ParameterTool tool = ParameterTool.fromArgs(args);
        //设置流执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        //设置 checkpoint
        //env.enableCheckpointing(2000L);
        //读取kafka中的数据
        //配置属性
        Properties props = new Properties();
        props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,"aliecs008:9092");
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG,"test1");
        props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");
        //自动发现分区的配置
        props.setProperty("flink.partition-discovery.interval-millis","5000");
        //自动提交的间隔时间
        props.setProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG,"2000");

        FlinkKafkaConsumer<String> consumer = new FlinkKafkaConsumer<>(
                "vehiclejsondata",
                new SimpleStringSchema(),
                props
        );
        //从头开始消费，默认是 latest（消费数据）
        consumer.setStartFromEarliest();
        //提交到 checkpoint
        consumer.setCommitOffsetsOnCheckpoints(true);
        DataStreamSource<String> source = env.addSource(consumer);
        //读取 kafka 中数据
        source.print();
        //执行流环境
        env.execute();
    }
}
