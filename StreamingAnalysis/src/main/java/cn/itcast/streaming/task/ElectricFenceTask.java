package cn.itcast.streaming.task;

import cn.itcast.entity.ElectricFenceModel;
import cn.itcast.entity.ItcastDataObj;
import cn.itcast.streaming.source.ElectricFenceResultTmp;
import cn.itcast.streaming.source.MysqlElectricFenceSouce;
import cn.itcast.utils.DateUtil;
import cn.itcast.utils.JsonParseUtil;
import cn.itcast.window.function.ElectricFenceRulesFunction;
import cn.itcast.window.function.ElectricFenceWindowFunction;
import cn.itcast.window.udfWatermark.ElectricFenceWatermark;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.api.common.serialization.SimpleStringSchema;

import org.apache.flink.streaming.api.datastream.ConnectedStreams;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.connector.jdbc.JdbcStatementBuilder;
import java.util.HashMap;

// 总结，别用flink1.10版本  太老了
// 成功进数据拿下
public class ElectricFenceTask extends BaseTask {
    public static void main(String[] args) throws Exception {
        /**
         * 实现步骤：
         * 1.电子围栏分析任务设置、原始数据json解析、过滤异常数据
         * 2.读取已存在电子围栏中的车辆与电子围栏信息(广播流临时结果数据)
         * 3.原始车辆数据与电子围栏广播流进行合并，生成电子围栏规则模型流数据（DStream<ElectricFenceModel>）
         * 4.创建90秒翻滚窗口，计算电子围栏信息(ElectricFenceModel中的值根据车辆是否在围栏内进行设置)
         * 5.读取电子围栏分析结果表数据并广播
         * 6.90秒翻滚窗口电子围栏对象模型流数据与电子围栏分析结果数据广播流进行connect
         * 7.对电子围栏对象模型，添加uuid和inMysql(车辆是否已存在mysql表中)
         * 8.电子围栏分析结果数据落地mysql|也可以选择落地mongo
         */
        //TODO 1.电子围栏分析任务设置、原始数据json解析、过滤异常数据
        StreamExecutionEnvironment env = getEnv(ElectricFenceTask.class.getSimpleName());

        //1.1：将kafka消费者实例添加到环境中
        DataStream<String> dataStreamSource = createKafkaStream(SimpleStringSchema.class);

        //1.2：将字符串转换成对象，过滤出来正常的数据
        SingleOutputStreamOperator<ItcastDataObj> itcastJsonDataStream = dataStreamSource.map(JsonParseUtil::parseJsonToObject)
                .filter(itcastDataObj -> StringUtils.isEmpty(itcastDataObj.getErrorData()));
//        itcastJsonDataStream.print("原始数据>>>");

        //TODO 2.读取已存在电子围栏中的车辆与电子围栏信息(广播流临时结果数据)
        // broadcast()方法，将广播DStream中的元素到每一个slot中
        DataStream<HashMap<String, ElectricFenceResultTmp>> electricFenceVinsStream = env.addSource(new MysqlElectricFenceSouce()).broadcast();
//        electricFenceVinsStream.print("电子围栏数据>>>");
        // 合并广播流数据和原有数据

        //TODO 3.原始车辆数据与电子围栏广播流进行合并，生成电子围栏规则模型流数据（DStream<ElectricFenceModel>）
        ConnectedStreams<ItcastDataObj, HashMap<String, ElectricFenceResultTmp>> electricFenceVinsConnectStream =
                // 合并kafka读取出来的原始数据和mysql读取出来的电子围栏数据
                itcastJsonDataStream.connect(electricFenceVinsStream);
//        electricFenceVinsStream.print("合并的数据>>>");
        // 对数据进行处理（通过自定义的函数，生成ElectricFenceModel
        // TODO 自定义函数1：用于处理合并数据
        // 判断经度、维度、gpsTime判断，是否为空，如不为空才可以进行数据处理: 数据满足条件，会返回验证通过
        SingleOutputStreamOperator<ElectricFenceModel> efModelDataStream = electricFenceVinsConnectStream.flatMap(
                new ElectricFenceRulesFunction());
//        efModelDataStream.print("电子围栏车辆规则与原始数据关联后的数据>>>");

        //TODO 4.创建90秒翻滚窗口，计算电子围栏信息(ElectricFenceModel中的值根据车辆是否在围栏内进行设置)
        DataStream<ElectricFenceModel> electricFenceDataStream = efModelDataStream
                .assignTimestampsAndWatermarks(new ElectricFenceWatermark())
                .keyBy(ElectricFenceModel::getVin)
                // TumblingEventTimeWindows.of(Time.hours(1)) 窗口大小
                .window(TumblingEventTimeWindows.of(Time.seconds(90)))
                // TODO 自定义函数2
                .apply(new ElectricFenceWindowFunction());  // 是否关联上，会打印不同的日志信息
        // LS6A2E0E5KA000668=ElectricFenceResultTmp{id=37, name='北京天安门', address='天安门', radius=15.0, longitude=116.397454,
        // latitude=39.909178, startTime=Fri Dec 07 00:00:00 CST 2018, endTime=Mon Dec 09 00:00:00 CST 2019}
        electricFenceDataStream.print("电子围栏结果>>>");

        // 落地mysql  表 electric_fence  使用jdbc sink，或者 mysql sink
        electricFenceDataStream
                .addSink(JdbcSink.sink(
                        // 注：processTime由mysql操作插入数据
                "INSERT INTO electric_fence(vin,inTime,outTime,gpsTime,lat,lng,eleId,eleName,address,latitude,longitude,radius,terminalTime) " +
                        " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,? )",
                (JdbcStatementBuilder<ElectricFenceModel>) (ps, t) -> {
//                    ps.setLong(1, t.getEleId());   // 自增id
                    ps.setString(1, t.getVin());
                    ps.setString(2, t.getInEleTime());
                    ps.setString(3, t.getOutEleTime());
                    ps.setString(4, t.getGpsTime());
                    ps.setDouble(5, t.getLat());
                    ps.setDouble(6, t.getLng());
                    ps.setLong(7, t.getEleId());
                    ps.setString(8, t.getEleName());
                    ps.setString(9, t.getAddress());
                    ps.setDouble(10, t.getLatitude());
                    ps.setDouble(11, t.getLongitude());
                    ps.setFloat(12,t.getRadius());
                    ps.setString(13,t.getTerminalTime());
//                    ps.setString(14,DateUtil.getCurrentDateTime());
                },
                new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                        .withUrl("jdbc:mysql://120.55.78.114:3306/test_data?characterEncoding=utf-8&useSSL=false")
                        .withDriverName("com.mysql.cj.jdbc.Driver")
                        .withUsername("rene")
                        .withPassword("@Aabc939596")
                        .build()
                        )
                );

        // 目前依赖有点问题
        env.execute();
    }


}