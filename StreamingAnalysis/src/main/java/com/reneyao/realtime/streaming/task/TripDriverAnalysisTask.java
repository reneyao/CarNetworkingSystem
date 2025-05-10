package com.reneyao.realtime.streaming.task;


import com.reneyao.realtime.entity.ItcastDataObj;
import com.reneyao.realtime.entity.TripModel;
import com.reneyao.realtime.streaming.sink.TripDivisionHBaseSink;
import com.reneyao.realtime.utils.JsonParseUtil;
import com.reneyao.realtime.window.function.DriveTripWindowFunction;
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

// 驾驶行程入库（分析）  业务需求：对车辆驾驶的里程进行分析
public class TripDriverAnalysisTask extends BaseTask{

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = getEnv(TripDriverAnalysisTask.class.getSimpleName());

        DataStream<String> dataStreamSource = createKafkaStream(SimpleStringSchema.class);

        SingleOutputStreamOperator<ItcastDataObj> itcastJsonStream = dataStreamSource.map(JsonParseUtil::parseJsonToObject)
                //过滤出来驾驶行程数据
                .filter(itcastDataObj -> 2 == itcastDataObj.getChargeStatus() || 3 == itcastDataObj.getChargeStatus());

        try {
            env.execute("TripDriveTask");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 添加水位线（允许数据延迟到达30秒钟）
        // 使用自定义的水位线对象 tripDriveWatermark
        SingleOutputStreamOperator<ItcastDataObj> tripDriveWatermark = itcastJsonStream
                .assignTimestampsAndWatermarks(new TripDriveWatermark());
        // 上面处理好的数据可以被采用数据/分析行程数据一起用
        // 后续要单独处理
        // 根据vin进行分组
        KeyedStream<ItcastDataObj, String> keyedStream = tripDriveWatermark.keyBy(ItcastDataObj::getVin);
        // 应用sessionWindow
        WindowedStream<ItcastDataObj, String, TimeWindow> driveDataStream = keyedStream.window(
                EventTimeSessionWindows.withGap(Time.minutes(15)));

        //   应用自定义的function
        SingleOutputStreamOperator<TripModel> tripdataresult = driveDataStream.apply(new DriveTripWindowFunction());


        //  驾驶行程入GMALL库TRIPDB:trip_division
        tripdataresult.addSink(new TripDivisionHBaseSink("GMALL:trip_division"));


        env.execute();
    }
}
