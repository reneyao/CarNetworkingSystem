package com.reneyao.realtime.streaming.task;

import com.reneyao.realtime.entity.ItcastDataObj;
import com.reneyao.realtime.entity.OnlineDataObj;
import com.reneyao.realtime.entity.VehicleModel;
import com.reneyao.realtime.streaming.sink.OnlineStatisticsMysqlSink;
import com.reneyao.realtime.streaming.source.VehicleInfoMysqlSource;
import com.reneyao.realtime.utils.JsonParseUtil;
import com.reneyao.realtime.window.function.AsyncHttpQueryFunction;
import com.reneyao.realtime.window.function.OnlineStatisticsWindowFunction;
import com.reneyao.realtime.window.function.VehicleInfoRedisFunction;
import com.reneyao.realtime.window.function.VehicleMapMysqlFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.datastream.WindowedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * 在线故障分析业务开发
 * 消费kafka数据进行实时故障分析，分析结果写入到mysql数据库中
 */
public class OnlineStatisticsTask extends BaseTask {
    public static void main(String[] args) throws Exception {
        /**
         * 开发步骤：
         * 1：创建流式环境，设置流式环境相关参数
         * 2：加载kafka数据源，过滤掉异常数据，过滤出来正常数据
         * 3：创建原始数据的30s的滚动窗口，根据vin进行分组
         * 4：对原始数据的窗口流数据进行实时故障分析核心逻辑的实现（区分出来告警数据以及非告警数据）
         * 5：加载业务中间表（7张表）数据，合并为车型、车系、车辆销售信息的DataStream，然后广播
         * 6：将第四步产生的窗口流数据和第五步产生的广播流数据进行合并
         * 7：加载地理位置信息表数据，进行广播
         * 8：对第六步的窗口流数据和第七步产生的地理位置信息广播流数据进行合并
         * 9：最终第八步产生的结果数据落地到mysql表中
         * 10：执行任务，查看分析结果
         */
        //1：创建流式环境，设置流式环境相关参数
        StreamExecutionEnvironment env = getEnv(OnlineStatisticsTask.class.getSimpleName());

        DataStream<String> dataStreamSource = createKafkaStream(SimpleStringSchema.class);
        dataStreamSource.print("原始数据>>>");


        //2.6：将字符串转换成javaBean
        SingleOutputStreamOperator<ItcastDataObj> itcastPartDataStream = dataStreamSource.map((MapFunction<String, ItcastDataObj>) obj -> {
            JsonParseUtil jsonParsePartUtil = new JsonParseUtil();
            return jsonParsePartUtil.parseJsonToObject(obj);
            //过滤出来异常数据，查询到正常数据返回
        }).filter(itcastPartObj -> itcastPartObj.getErrorData().isEmpty());
        dataStreamSource.print("原始数据过滤以后>>>");

        //3：创建原始数据的30s的滚动窗口，根据vin进行分组
        WindowedStream<ItcastDataObj, String, TimeWindow> itcastWindowDataStream =
                itcastPartDataStream.keyBy(itcastPartObj -> itcastPartObj.getVin()).timeWindow(Time.seconds(30));

        //4：对原始数据的窗口流数据进行实时故障分析核心逻辑的实现（告警数据及非告警数据的区分）
        SingleOutputStreamOperator<OnlineDataObj> onlineDataObjDataStream = itcastWindowDataStream.apply(
                new OnlineStatisticsWindowFunction());
        onlineDataObjDataStream.print("自定义窗口数据>>>");

        //5：加载业务中间表（7张表）数据，合并为车型、车系、车辆销售信息的DataStream，然后广播
        DataStream<HashMap<String, VehicleModel>> vehicalInfoDataStream =
                env.addSource(new VehicleInfoMysqlSource()).broadcast();
        vehicalInfoDataStream.printToErr("车辆基础数据>>>");

        // 6：将第四步产生的窗口流数据和第五步产生的广播流数据进行合并
        SingleOutputStreamOperator<OnlineDataObj> connectVehicalDataStream =
                onlineDataObjDataStream.connect(vehicalInfoDataStream).flatMap(new VehicleMapMysqlFunction());

        //7：根据经纬度获取地理位置信息
        SingleOutputStreamOperator<OnlineDataObj> vehicalLocationDataStream = connectVehicalDataStream.map(
                new VehicleInfoRedisFunction());

        //获取到拉宽成功的地理位置信息数据
        SingleOutputStreamOperator<OnlineDataObj> vehicalWithLocationDataStream = vehicalLocationDataStream.filter(onlineDataObj -> onlineDataObj.getProvince() != null);
        SingleOutputStreamOperator<OnlineDataObj> vehicalNoWithLocationDataStream = vehicalLocationDataStream.filter(onlineDataObj -> onlineDataObj.getProvince() == null);

        //8：异步请求高德地图根据经纬度获取到地理位置信息
        SingleOutputStreamOperator<OnlineDataObj> asyncVehicalWithLocationDataStream = AsyncDataStream.unorderedWait(
                vehicalNoWithLocationDataStream,                          //输入的数据流
                new AsyncHttpQueryFunction(),   //异步查询的Function实例
                2000,                   //超时时间
                TimeUnit.MILLISECONDS,         //时间单位
                10                     //最大异步并发请求数量（并发的线程队列数）
        );

        // 对窗口流数据和产生的地理位置信息广播流数据进行合并
        DataStream<OnlineDataObj> resultDataStream = vehicalWithLocationDataStream.union(asyncVehicalWithLocationDataStream);
        resultDataStream.printToErr("带有地理位置信息的数据>>>");


        resultDataStream.addSink(new OnlineStatisticsMysqlSink());

        //10：执行任务，查看分析结果
        env.execute();
    }
}


