package com.reneyao.realtime.window.function;

import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple5;
import org.apache.flink.api.java.tuple.Tuple7;
import org.apache.flink.streaming.api.functions.co.CoFlatMapFunction;
import org.apache.flink.util.Collector;

// 自定义CoFlatMapFunction对象  类似flatmap算子  进入数据，不一定一一匹配
public class DstreamOperatorFlatMap implements CoFlatMapFunction<Tuple3<String, String, Double>, Tuple5<String, Float, Integer, Double, Double>, Tuple7<String, String, Double, Float, Integer, Double, Double>> {
    // 使用connect连接数据后一定要对流进行处理
    // 可以使用HashMap去存储一个流的数据，然后去匹配
    Tuple5<String, Float, Integer, Double, Double> tuple5 = new Tuple5<String, Float, Integer, Double, Double>("", 0.0f, 0, 0.0, 0.0);
    // 第一个流有数据进来，调用flatMap1，第二个流有数据进行调用flatMap2
    @Override
    public void flatMap1(Tuple3<String, String, Double> value, Collector<Tuple7<String, String, Double, Float, Integer, Double, Double>> out) throws Exception {
        // 没匹配上的打为空，匹配上的就连接显示
        out.collect(Tuple7.of(value.f0, value.f1, value.f2, tuple5.f1, tuple5.f2, tuple5.f3, tuple5.f4));
    }

    @Override
    public void flatMap2(Tuple5<String, Float, Integer, Double, Double> value, Collector<Tuple7<String, String, Double, Float, Integer, Double, Double>> out) throws Exception {
        tuple5 = value;
    }
}
