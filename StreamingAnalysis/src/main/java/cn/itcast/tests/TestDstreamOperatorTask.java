package cn.itcast.tests;

import cn.itcast.window.function.DstreamOperatorFlatMap;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple5;
import org.apache.flink.api.java.tuple.Tuple7;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class TestDstreamOperatorTask {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        env.setParallelism(1);
        // 流1：添加车辆测试数据 vin、terminalTime、soc   80D：理解为电量为百分之80
        Tuple3<String, String, Double> tuple3_1 = Tuple3.of("LSDKCAS872ADS", "2020-06-08 13:00:02", 80D);
        Tuple3<String, String, Double> tuple3_2 = Tuple3.of("LS5A3AJC8JB004567", "2020-06-08 13:01:03", 57D);
        Tuple3<String, String, Double> tuple3_3 = Tuple3.of("LS5A3AJC3JB009480", "2020-06-08 14:01:25", 68D);
        Tuple3<String, String, Double> tuple3_4 = Tuple3.of("LS5A3AJC1JB007195", "2020-06-08 13:32:52", 100D);
        Tuple3<String, String, Double> tuple3_5 = Tuple3.of("LS5A3CJC2JF810117", "2020-06-05 17:55:36", 35D);
        // 流1：根据测试数据创建data stream
        DataStream<Tuple3<String, String, Double>> dataStream = env.fromElements(tuple3_1, tuple3_2, tuple3_3, tuple3_4, tuple3_5);

        // 流2：广播流测试数据 vin、vehicleSpeed、satNum、lat、lng
        Tuple5<String, Float, Integer, Double, Double> tuple5_1 = Tuple5.of("LSDKCAS872ADS", 55.5f, 3, 114.21523, 34.21242);
        Tuple5<String, Float, Integer, Double, Double> tuple5_2 = Tuple5.of("LS5A3AJC3JB009480", 25.0f,  2, 115.11123, 33.5311);
        Tuple5<String, Float, Integer, Double, Double> tuple5_3 = Tuple5.of("LS5A3AJC8JB004567", 35.2f,  3, 115.2323, 36.2312);
        Tuple5<String, Float, Integer, Double, Double> tuple5_4 = Tuple5.of("LS5A3AJC1JB007195", 65.3f,  1, 116.1423, 35.13232);
        Tuple5<String, Float, Integer, Double, Double> tuple5_5 = Tuple5.of("LS5A3CJC2JF810117", 95.1f,  5, 116.2315, 32.25611);
        // 根据测试数据创建data stream并广播给下一个task
        // broadcast()方法广播数据
        DataStream<Tuple5<String, Float, Integer, Double, Double>> dataStreamBroadcast = env.fromElements(tuple5_1, tuple5_2, tuple5_3, tuple5_4, tuple5_5).broadcast();

        // 连接两个流并进行flatMap操作(了解与map操作的区别)   一个流3列，一个流5列
        DataStream<Tuple7<String, String, Double, Float, Integer, Double, Double>> resultDataStream = dataStream.connect(dataStreamBroadcast)
                .flatMap(new DstreamOperatorFlatMap());
//                .map(new DstreamOperatorMap());

        resultDataStream.print();

        env.execute();
    }
}
