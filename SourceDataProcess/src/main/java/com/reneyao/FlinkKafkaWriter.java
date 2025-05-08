package com.reneyao;

import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

import javax.annotation.Nullable;
import java.util.Properties;
// 使用flink连接kafka进行生成数据，支持并行处理
/**
 * 模拟程序
 * 生成数据
 * 因为测试环境不具备车联网的TBox环境，因此只能编写数据生成器代码将数据直接写入到kafka集群
 *
 */
public class FlinkKafkaWriter {
    public static void main(String[] args) throws Exception {
        //1）创建flink流式程序的开发环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.setParallelism(4);

        //2）设置应应用程序按照事件时间处理数据
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        //3）加载原始数据目录中的所有文件，返回dataStream对象：设置为自己的路径
        DataStreamSource<String> source = env.readTextFile("E:\\BigDataProfile\\ProjectData\\3.Java Flink车联网项目\\cardata\\sourcedata/sourcedata.txt");
        //4）创建kafka的生产者实例，将数据写入到kafka集群(无集群
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "aliecs008:9092");
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 60000);  // 增加请求超时时间到 1 分钟
        props.put(ProducerConfig.LINGER_MS_CONFIG, 20);  // 调整 linger.ms 配置以减少网络请求次数
        // 加入ack就正常了，kafka集群不稳地
        props.put("request.required.acks", "1");   // 需要ack
        props.put("producer.type", "async");
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);  // 增加缓冲区大小到 32 MB

        FlinkKafkaProducer<String> producer = new FlinkKafkaProducer<>(
                // 设置的topic
                "vehiclejsondata",
                new KafkaSerializationSchema<String>() {
                    @Override
                    public ProducerRecord<byte[], byte[]> serialize(String element, @Nullable Long timestamp) {
                        return new ProducerRecord<>(
                                "vehiclejsondata",
                                element.getBytes()
                        );
                    }
                },
                props,
                FlinkKafkaProducer.Semantic.EXACTLY_ONCE
        );
        source.print();
        source.addSink(producer);
        //5）启动运行
        env.execute();
    }
}