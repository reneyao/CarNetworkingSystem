package cn.itcast.window.function;

import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple5;
import org.apache.flink.api.java.tuple.Tuple7;
import org.apache.flink.streaming.api.functions.co.CoMapFunction;

// 自定义CoMapFunction
public class DstreamOperatorMap implements CoMapFunction<Tuple3<String, String, Double>, Tuple5<String, Float, Integer, Double, Double>, Tuple7<String, String, Double, Float, Integer, Double, Double>> {

    // map算子是一定要一对一出的
    Tuple5<String, Float, Integer, Double, Double> tuple5 = new Tuple5<String, Float, Integer, Double, Double>("", 0.0f, 0, 0.0, 0.0);

    @Override
    public Tuple7<String, String, Double, Float, Integer, Double, Double> map1(Tuple3<String, String, Double> value) throws Exception {
        return Tuple7.of(value.f0, value.f1, value.f2, tuple5.f1, tuple5.f2, tuple5.f3, tuple5.f4);

    }

    @Override
    public Tuple7<String, String, Double, Float, Integer, Double, Double> map2(Tuple5<String, Float, Integer, Double, Double> value) throws Exception {
        tuple5 = value;
        return new Tuple7<>();
    }
}