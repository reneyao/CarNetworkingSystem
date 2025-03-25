package cn.itcast.streaming.task;

import cn.itcast.entity.ItcastDataObj;
import cn.itcast.streaming.sink.SrcDataToHBaseSink;
import cn.itcast.utils.FlinkUtil;
import cn.itcast.utils.JsonParseUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.contrib.streaming.state.RocksDBStateBackend;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.OutputFileConfig;
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink;
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.DateTimeBucketAssigner;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;
import org.apache.flink.streaming.connectors.fs.bucketing.BucketingSink;
import org.apache.flink.streaming.connectors.fs.bucketing.DateTimeBucketer;


import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * 需求：原始数据ETL操作
 * flink消费kafka数据，将消费出来的数据进行转换、清洗、过滤以后，正常的数据需要写入到hbase和hdfs，异常的数据写入到hdfs中
 * 1）正常数据写入hdfs和hbase
 * 2）异常数据写入到hbase
 */
public class KafkaSourceDataTask extends BaseTask {
    public static void main(String[] args) throws Exception {
        // 1）初始化flink流式处理的开发环境  getSimpleName()  获得类名 返回的是字符串格式
        StreamExecutionEnvironment env = getEnv(KafkaSourceDataTask.class.getSimpleName());

        //6）将kafka消费者对象添加到环境中
        DataStream<String> dataStreamSource= createKafkaStream(SimpleStringSchema.class);  // 简单的string对象的序列化，反序列化对象类

        //打印输出测试
//        dataStreamSource.print();
        //7）将json字符串解析成对象  借助map一一对应转化
        SingleOutputStreamOperator<ItcastDataObj> itcastDataObjStream = dataStreamSource.map(JsonParseUtil::parseJsonToObject);
        itcastDataObjStream.printToErr("解析后的数据>>>");
        //8）获取到异常的数据   如果数据为空判断为异常数据
        SingleOutputStreamOperator<ItcastDataObj> errorDataStream = itcastDataObjStream.filter(
                itcastDataObj -> !StringUtils.isEmpty(itcastDataObj.getErrorData()));
        errorDataStream.printToErr("异常数据>>>");
        //指定写入的文件名称和格式
        OutputFileConfig config = OutputFileConfig.builder().withPartPrefix("prefix").withPartSuffix(".txt").build();

        // 9）将异常的数据写入到hdfs中
        // 在flink1.2以后可以直接使用  FileSink操作，而不用使用StreamingFileSink，操作更加便利
        StreamingFileSink errorFileSink = StreamingFileSink.forRowFormat(new Path(parameterTool.getRequired("hdfsUri")+"/apps/hive/warehouse/ods.db/itcast_error"),
                        new SimpleStringEncoder<>("utf-8"))
                // 按照数据进行桶分区
                .withBucketAssigner(new DateTimeBucketAssigner("yyyyMMdd"))
                // 设置滚动策略
                .withRollingPolicy(
                        DefaultRollingPolicy.builder()
                                // 设置文件滚动的时间间隔为5秒钟，表示每5秒钟生成一个新文件。
                                .withRolloverInterval(TimeUnit.SECONDS.toMillis(5)) //设置滚动时间间隔，5秒钟产生一个文件
                                // 设置不活动的时间间隔为2秒钟，即2秒没有数据进入，则生成一个新的文件
                                .withInactivityInterval(TimeUnit.SECONDS.toMillis(2)) //设置不活动的时间间隔，未写入数据处于不活动状态时滚动文件
                                // 设置文件的最大大小为128MB，当文件大小达到128MB时滚动生成一个新文件。
                                .withMaxPartSize(128*1024*1024)//文件大小，默认是128M滚动一次
                                .build()
                ).withOutputFileConfig(config).build();
        errorDataStream.map(ItcastDataObj::toHiveString).addSink(errorFileSink);

        //10）获取到正常的数据  （区分在与异常数据会加 “！”号 ，即是不等于号
        SingleOutputStreamOperator<ItcastDataObj> srcDataStream = itcastDataObjStream.filter(itcastDataObj -> StringUtils.isEmpty(itcastDataObj.getErrorData()));
        srcDataStream.print("正常数据>>>");

        //11）将正常的数据写入到hdfs中（StreamingFileSink、BucketingSink）StreamingFileSink是flink1.10的新特性，而flink1.8.1版本，是没有这个功能的，因此只能BucketingSink
        /**
         * 离线数据的写入是每天加载一次（离线数据是T+1的数据）
         * 异常数据落地到hive的方案：
         *   1.直接通过jdbc的方式将数据写入到hive中(效率比较低)
         *   2.将数据流式的方式实时的写入到hdfs中，然后使用hive加载hdfs的数据
         * hive的表结构与hdfs数据的格式要相互匹配（首先在hive中创建表）
         *   1：hive中创建表
         *   2：将数据写入到hive读取的hdfs的数据路径
         **/
        //指定写入的文件名称和格式  文件在hdfs中存储的位置"/apps/hive/warehouse/ods.db/itcast_src"
        StreamingFileSink srcFileSink = StreamingFileSink.forRowFormat(new Path(parameterTool.getRequired("hdfsUri")+"/apps/hive/warehouse/ods.db/itcast_src"),
                        new SimpleStringEncoder<>("utf-8"))
                /**
                 * 指定分桶的策略
                 * DateTimeBucketAssigner：默认的桶分配策略，默认基于时间的分配器，每小时产生一个桶，指定时间格式：yyyy-MM-dd-HH
                 * BasePathBucketAssigner：将所有的文件存在基本路径的分配器（全局桶）
                 */
                .withBucketAssigner(new DateTimeBucketAssigner("yyyyMMdd"))
                /**
                 * 指定滚动策略：
                 * DefaultRollingPolicy
                 * CheckpointRollingPolicy
                 * OnCheckpointRollingPolicy
                 */
                .withRollingPolicy(
                        //使用默认的滚动策略
                        DefaultRollingPolicy.builder()
                                .withRolloverInterval(TimeUnit.SECONDS.toMillis(5)) //设置滚动时间间隔，5秒钟产生一个文件
                                .withInactivityInterval(TimeUnit.SECONDS.toMillis(2)) //设置不活动的时间间隔，未写入数据处于不活动状态时滚动文件
                                .withMaxPartSize(128*1024*1024)//文件大小，默认是128M滚动一次
                                .build()
                ).withOutputFileConfig(config).build();
        srcDataStream.map(ItcastDataObj::toHiveString).addSink(srcFileSink);

        //  12) 将数据落入hbase
        // 定义hbaseSink
        SrcDataToHBaseSink hbaseSink = new SrcDataToHBaseSink("itcast_src");
        // 保存原始数据存入 hbase   srcDataStream 是正常数据，并经过处理后的数据源
        srcDataStream.addSink(hbaseSink);

        // 13）启动作业，运行任务
        env.execute();
    }
}
