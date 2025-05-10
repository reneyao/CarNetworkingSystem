package com.reneyao.realtime.streaming.task;

import com.reneyao.realtime.entity.ItcastDataObj;
import com.reneyao.realtime.streaming.sink.TripSampleToHBaseSink;
import com.reneyao.realtime.utils.JsonParseUtil;
import com.reneyao.realtime.window.function.DriveSampleWindowFunction;
import com.reneyao.realtime.window.udfWatermark.TripDriveWatermark;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.datastream.WindowedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.EventTimeSessionWindows;

import org.apache.flink.streaming.api.windowing.time.Time;

import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
// 驾驶行程入库（采样）
/**
 * 驾驶行程业务开发
 * 1）消费kafka数据过滤出来驾驶行程采样数据，实时的写入到hbase表中
 * 2）消费kafka数据过滤出来驾驶行程数据，实时的写入到hbase表中
 * 添加行程划分水位线
 * 根据vin进行分组
 * 指定Window为SessionWindow
 * 驾驶行程采样分析与入库
 * 驾驶行程划分与入库
 */
public class TripDriveTask extends BaseTask {
    // 继承BaseTask任务
    public static void main(String[] args) throws Exception {
        // 1）初始化flink流式处理的开发环境
        StreamExecutionEnvironment env = getEnv(TripDriveTask.class.getSimpleName());

        // 6）将kafka消费者对象添加到环境中
        DataStream<String> dataStreamSource = createKafkaStream(SimpleStringSchema.class);

        // 7）将消费出来的数据进行json解析成javaBean对象  map处理   数据
        SingleOutputStreamOperator<ItcastDataObj> itcastJsonStream = dataStreamSource.map(JsonParseUtil::parseJsonToObject)
                //过滤出来驾驶行程数据
                .filter(itcastDataObj -> 2 == itcastDataObj.getChargeStatus() || 3 == itcastDataObj.getChargeStatus());

        try {
            env.execute("TripDriveTask");
        } catch (Exception e) {
            e.printStackTrace();
        }

        //8）添加水位线（允许数据延迟到达30秒钟）
        // 使用自定义的水位线对象 tripDriveWatermark
        SingleOutputStreamOperator<ItcastDataObj> tripDriveWatermark = itcastJsonStream
                .assignTimestampsAndWatermarks(new TripDriveWatermark());
        // 上面处理好的数据可以被采用数据/分析行程数据一起用
        //9）根据vin进行分组
        KeyedStream<ItcastDataObj, String> keyedStream = tripDriveWatermark.keyBy(ItcastDataObj::getVin);

        //10）应用sessionWindow
        WindowedStream<ItcastDataObj, String, TimeWindow> driveDataStream = keyedStream.window(
                EventTimeSessionWindows.withGap(Time.minutes(15)));

        // 11) 应用自定义的function  亦可以使用Process，aggregate去计算
        // 传入ItcastDataObj类型的迭代器
        SingleOutputStreamOperator<String[]> driveSampleDataStream = driveDataStream.apply(new DriveSampleWindowFunction());

        //12） 驾驶行程采样如hbase库TRIPDB:trip_sample
        driveSampleDataStream.addSink(new TripSampleToHBaseSink("TRIPDB:trip_sample"));

//        // 第二个数据处理
//        // 13）  沿用todo10 的window处理后的数据  再应用另一个function
//        SingleOutputStreamOperator<TripModel> tripModelSingleOutputStreamOperator = driveDataStream.apply(new DriveTripWindowFunction());
//
//        //  14) 写入到hbase
//        tripModelSingleOutputStreamOperator.addSink(new TripDivisionHBaseSink("TRIPDB:trip_division"));

        env.execute();
    }


    }


