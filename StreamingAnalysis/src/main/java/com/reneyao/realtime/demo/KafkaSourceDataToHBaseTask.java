package com.reneyao.realtime.demo;


import com.reneyao.realtime.entity.ItcastDataObj;
import com.reneyao.realtime.streaming.sink.SrcDataToHBaseSink;
import com.reneyao.realtime.streaming.task.BaseTask;
import com.reneyao.realtime.utils.JsonParseUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.OutputFileConfig;
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink;
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.DateTimeBucketAssigner;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;

import java.util.concurrent.TimeUnit;

// 写入到hbase
public class KafkaSourceDataToHBaseTask extends BaseTask {

    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = getEnv(KafkaSourceDataToHBaseTask.class.getSimpleName());
        System.setProperty("HADOOP_USER_NAME","rene");
        DataStream<String> dataStreamSource= createKafkaStream(SimpleStringSchema.class);


        SingleOutputStreamOperator<ItcastDataObj> itcastDataObjStream = dataStreamSource.map(JsonParseUtil::parseJsonToObject);
        itcastDataObjStream.printToErr("解析后的数据>>>");

        SingleOutputStreamOperator<ItcastDataObj> errorDataStream = itcastDataObjStream.filter(
                itcastDataObj -> !StringUtils.isEmpty(itcastDataObj.getErrorData()));
        errorDataStream.printToErr("异常数据>>>");
        //指定写入的文件名称和格式
        OutputFileConfig config = OutputFileConfig.builder().withPartPrefix("prefix").withPartSuffix(".txt").build();

        // 将异常的数据写入到hdfs中
        StreamingFileSink errorFileSink = StreamingFileSink.forRowFormat(new Path(parameterTool.getRequired("hdfsUri")+"/apps/hive/warehouse/vehicle/ods.db/itcast_error"),
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

        // 获取到正常的数据  （区分在与异常数据会加 “！”号 ，即是不等于号
        SingleOutputStreamOperator<ItcastDataObj> srcDataStream = itcastDataObjStream.filter(itcastDataObj -> StringUtils.isEmpty(itcastDataObj.getErrorData()));
        srcDataStream.print("正常数据>>>");



        // 将正常数据落入hbase
        // 定义hbaseSink
        SrcDataToHBaseSink hbaseSink = new SrcDataToHBaseSink("itcast_src");
        // 保存原始数据存入 hbase   srcDataStream 是正常数据，并经过处理后的数据源
        srcDataStream.addSink(hbaseSink);

        // 13）启动作业，运行任务
        env.execute();
    }
}

