package com.reneyao.realtime.streaming.task;

import com.reneyao.realtime.entity.ItcastDataObj;
import com.reneyao.realtime.streaming.sink.VehicleDetailSinkOptimizer;
import com.reneyao.realtime.utils.JsonParseUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

// 从kafka源数据获取到车辆明细数据VehicleDetai，写入到hbase
public class SourceToVehicleDetail extends BaseTask{

    public static void main(String[] args) throws Exception {
        System.setProperty("HADOOP_USER_NAME", "rene");
        // 使用basetask的getenv方法
        StreamExecutionEnvironment env = getEnv(SourceToVehicleDetail.class.getSimpleName());

        // 获取kafka数据源   序列化使用基础的字符串序列化
        DataStream<String> kafkaStream = createKafkaStream(SimpleStringSchema.class);

        // 解析数据：map算子
        SingleOutputStreamOperator<ItcastDataObj> itcastDataObjStream = kafkaStream.map(JsonParseUtil::parseJsonToObject);

        // 获取正常的数据：即使没有异常数据，itcastDataObj.getErrorData()没有返回数据
        SingleOutputStreamOperator<ItcastDataObj> srcDataStream = itcastDataObjStream.
                filter(itcastDataObj -> StringUtils.isEmpty(itcastDataObj.getErrorData()));

        // 正常数据，写入到hbase中：只取目标表的字段  itcastsrc_vehicle_detail已经在phoenix创建好
        VehicleDetailSinkOptimizer hbaseSink = new VehicleDetailSinkOptimizer("itcastsrc_vehicle_detail_test");
//        // 使用自定义的sink
        srcDataStream.addSink(hbaseSink);

        // 执行
        env.execute();

    }
}
