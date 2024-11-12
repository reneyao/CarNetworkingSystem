package cn.itcast.utils;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class FlinkUtil {

    private static StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

    public static <T> DataStream<T> createKafkaStream(ParameterTool parameters, Class<? extends DeserializationSchema<T>> clazz) throws Exception {

        //1.设置全局的参数
        env.getConfig().setGlobalJobParameters(parameters);            // 全局参数

        //2.checkpoint配置
        env.enableCheckpointing(parameters.getLong("checkpoint.interval", 5000L), CheckpointingMode.EXACTLY_ONCE);

        //3.取消checkpoint任务不删除
        env.getCheckpointConfig().enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);

        //4.kafka配置
        Properties prop = new Properties();
        prop.setProperty("bootstrap.servers", parameters.getRequired("bootstrap.servers"));
        prop.setProperty("group.id", parameters.getRequired("group.id"));
        prop.setProperty("auto.offset.reset", parameters.get("auto.offset.reset", "earliest"));
        //5.不自动提交偏移量，交给flink的checkpoint处理哦
        prop.setProperty("enable.auto.commit", parameters.get("enable.auto.commit", "false"));
        String topics = parameters.getRequired("topics");
        List<String> topicList = Arrays.asList(topics.split(","));

        FlinkKafkaConsumer<T> kafkaConsumer = new FlinkKafkaConsumer<T>(
                topicList,
                clazz.newInstance(),
                prop);

        return env.addSource(kafkaConsumer);
    }

    //获取执行环境
    public static StreamExecutionEnvironment getEnv() {
        return env;
    }
}

