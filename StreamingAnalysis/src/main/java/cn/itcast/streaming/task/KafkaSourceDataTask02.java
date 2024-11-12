package cn.itcast.streaming.task;

import cn.itcast.streaming.sink.SrcDataToHBaseSink;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import cn.itcast.utils.JsonParseUtil;
import cn.itcast.entity.ItcastDataObj;
import org.apache.flink.streaming.api.functions.sink.filesystem.OutputFileConfig;
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink;
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.DateTimeBucketAssigner;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;

import java.util.concurrent.TimeUnit;

/**
 * 需求：原始数据ETL操作
 * flink消费kafka数据，将消费出来的数据进行转换、清洗、过滤以后，正常的数据需要写入到hbase和hdfs，异常的数据写入到hdfs中
 * 1）正常数据写入hdfs和hbase
 * 2）异常数据写入到hbase
 */
public class KafkaSourceDataTask02 extends BaseTask {
    /**
     * 入口方法
     * @param args
     */
    public static void main(String[] args) throws Exception {
        /**
         * 实现步骤：
         * 1）初始化flink流式处理的开发环境
         * 2）设置按照事件时间处理数据（划分窗口或者添加水印都需要事件时间）
         * 3）开启checkpoint
         *      3.1：设置每隔30秒钟开启checkpoint
         *      3.2：设置检查点的model，exactly-once，保证数据消费一次，数据不重复消费
         *      3.3：设置两次checkpoint时间间隔，避免两次间隔太近导致频繁checkpoint而出现业务处理能力下降
         *      3.4：设置checkpoint的超时时间
         *      3.5：设置checkpoint最大的尝试次数，同一个时间有几个checkpoint并行执行
         *      3.6：设置checkpoint取消的时候，是否保留checkpoint，checkpoint默认会在job取消的时候删除
         *      3.7：设置执行job过程中，保存检查点错误时，job不失败
         *      3.8：设置检查点存储的位置，使用rocksDBStateBackend，存储到本地+hdfs分布式文件，增量检查点
         * 4）设置任务的重启策略（固定延迟重启策略、失败率重启策略、无重启策略）
         * 5）创建flink消费kafka数据的对象，指定kafka的参数信息
         *      5.1：设置kafka集群地址
         *      5.2：设置消费者组id
         *      5.3：设置kafka的分区感知（动态监测）
         *      5.4：设置key和value的反序列化
         *      5.5：设置自动递交offset位置策略
         *      5.6：创建kafka的消费者实例
         *      5.7：设置自动递交offset到保存到检查点
         * 6）将kafka消费者对象添加到环境中
         * 7）将json字符串解析成对象
         * 8）获取到异常的数据
         * 9）获取到正常的数据
         * 10）将异常的数据写入到hdfs中（StreamingFileSink、BucketingSink）
         *      StreamingFileSink是flink1.10的新特性，而flink1.8.1版本，是没有这个功能的，因此只能BucketingSink
         * 11）将正常的数据写入到hdfs中
         * 12）将正常的数据写入到hbase中
         * 13）启动作业，运行任务
         */

        //TODO 1）初始化flink流式处理的开发环境(使用basetask的方法
        StreamExecutionEnvironment env = getEnv(KafkaSourceDataTask02.class.getSimpleName());

        //TODO 6）将kafka消费者对象添加到环境中   使用BaseTask的静态方法，将kafka的消费者加入到数据源
        DataStream<String> dataStreamSource= createKafkaStream(SimpleStringSchema.class);
        //打印输出测试
        dataStreamSource.print();

        //TODO 7）将json字符串解析成对象
        SingleOutputStreamOperator<ItcastDataObj> itcastDataObjStream = dataStreamSource.map(JsonParseUtil::parseJsonToObject);
        itcastDataObjStream.printToErr("解析后的数据>>>");      // 打印输出

        //TODO 8）获取到异常的数据
        SingleOutputStreamOperator<ItcastDataObj> errorDataStream = itcastDataObjStream.filter(itcastDataObj -> !StringUtils.isEmpty(itcastDataObj.getErrorData()));
        errorDataStream.printToErr("异常数据>>>");

        //指定写入的文件名称和格式
        OutputFileConfig config = OutputFileConfig.builder().withPartPrefix("prefix").withPartSuffix(".txt").build();
        //TODO 9）将异常的数据写入到hdfs中
        StreamingFileSink errorFileSink = StreamingFileSink.forRowFormat(new Path(parameterTool.getRequired("hdfsUri")+"/apps/hive/warehouse/ods.db/itcast_error"),
                        new SimpleStringEncoder<>("utf-8"))
                .withBucketAssigner(new DateTimeBucketAssigner("yyyyMMdd"))
                .withRollingPolicy(
                        DefaultRollingPolicy.builder()
                                .withRolloverInterval(TimeUnit.SECONDS.toMillis(5)) //设置滚动时间间隔，5秒钟产生一个文件
                                .withInactivityInterval(TimeUnit.SECONDS.toMillis(2)) //设置不活动的时间间隔，未写入数据处于不活动状态时滚动文件
                                .withMaxPartSize(128*1024*1024)//文件大小，默认是128M滚动一次
                                .build()
                ).withOutputFileConfig(config).build();
        errorDataStream.map(ItcastDataObj::toHiveString).addSink(errorFileSink);

        //TODO 10）获取到正常的数据
        // 数据流操作
        SingleOutputStreamOperator<ItcastDataObj> srcDataStream = itcastDataObjStream.filter(itcastDataObj -> StringUtils.isEmpty(itcastDataObj.getErrorData()));
        srcDataStream.print("正常数据>>>");

        //TODO 11）将正常的数据写入到hdfs中（StreamingFileSink、BucketingSink）StreamingFileSink是flink1.10的新特性，而flink1.8.1版本，是没有这个功能的，因此只能BucketingSink
        /**
         * 离线数据的写入是每天加载一次（离线数据是T+1的数据）
         * 异常数据落地到hive的方案：
         *   1.直接通过jdbc的方式将数据写入到hive中(效率比较低)
         *   2.将数据流式的方式实时的写入到hdfs中，然后使用hive加载hdfs的数据
         * hive的表结构与hdfs数据的格式要相互匹配（首先在hive中创建表）
         *   1：hive中创建表
         *   2：将数据写入到hive读取的hdfs的数据路径
         **/
        //指定写入的文件名称和格式
        StreamingFileSink srcFileSink = StreamingFileSink.forRowFormat(new Path(parameterTool.getRequired("hdfsUri")+"/hive/warehouse/ods.db/itcast_src"),
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
                        //使用默认的滚动策略（设置各个参数
                        DefaultRollingPolicy.builder()
                                .withRolloverInterval(TimeUnit.SECONDS.toMillis(5)) //设置滚动时间间隔，5秒钟产生一个文件
                                .withInactivityInterval(TimeUnit.SECONDS.toMillis(2)) //设置不活动的时间间隔，未写入数据处于不活动状态时滚动文件
                                .withMaxPartSize(128*1024*1024)//文件大小，默认是128M滚动一次
                                .build()
                ).withOutputFileConfig(config).build();
        srcDataStream.map(ItcastDataObj::toHiveString).addSink(srcFileSink);


        // TODO 12）将正常的数据写入到hbase中
        srcDataStream.addSink(new SrcDataToHBaseSink("itcast_src"));     // 输入操作的表名
        //TODO 13）启动作业，运行任务
        env.execute();
    }
}
