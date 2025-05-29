package com.reneyao.realtime.demo;

import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.util.Random;

public class DataSkewState {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env =
                StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        // 读取数据源
        env
                .fromElements(
                        // f0是数据key，f1是个数，f2是事件时间
                        Tuple3.of("a", 2L, 1000L),     // f0,f1,f2
                        Tuple3.of("a", 1L, 2000L),
                        Tuple3.of("a", 1L, 3000L),
                        Tuple3.of("a", 1L, 4000L),
                        Tuple3.of("a", 1L, 5000L),
                        Tuple3.of("a", 1L, 6000L),
                        Tuple3.of("a", 1L, 7000L),
                        Tuple3.of("a", 1L, 8000L),
                        Tuple3.of("a", 1L, 9000L),
                        Tuple3.of("a", 1L, 10000L),
                        Tuple3.of("a", 1L, 11000L),
                        Tuple3.of("b", 1L, 12000L)
                )
                .map(new MapFunction<Tuple3<String, Long, Long>, Tuple3<String, Long,
                                        Long>>() {
                    @Override
                    public Tuple3<String, Long, Long> map(Tuple3<String, Long, Long> value)
                            throws Exception {
                        Random rand = new Random();
                        // 手动加数，分区
                        return Tuple3.of(value.f0 + "-" + rand.nextInt(4), value.f1,
                                value.f2);
                    }
                })
                // 设置水位线
                .assignTimestampsAndWatermarks(WatermarkStrategy.<Tuple3<String, Long,
                        // 水位线策略，表示时间是单调增加的，没有对乱序数据做处理，无最大延迟时间
                        Long>>forMonotonousTimestamps()
                        // 从每个元素中提取事件时间
                .withTimestampAssigner(
                        // 使用匿名内部类
                        new SerializableTimestampAssigner<Tuple3<String,
                                        Long, Long>>() {
                    @Override
                    public long extractTimestamp(Tuple3<String, Long, Long> element, long
                            recordTimestamp) {
                        // 记录事件时间
                        return element.f2;
                    }
                }))
                .keyBy(r -> r.f0)
                .process(new KeyedProcessFunction<String, Tuple3<String, Long, Long>,
                                        Tuple2<String, Long>>() {
                    // TODO:ValueState
                    private ValueState<Tuple2<String, Long>> sum;
                    private ValueState<Long> timerTs;        // 记录时间定时器
                    @Override
                    public void open(Configuration parameters) throws Exception {
                        super.open(parameters);
                        sum = getRuntimeContext().getState(new
                                ValueStateDescriptor<Tuple2<String, Long>>("sum",
                                Types.TUPLE(Types.STRING, Types.LONG)));
                        timerTs = getRuntimeContext().getState(new
                                ValueStateDescriptor<Long>("timer", Types.LONG));
                    }
                    @Override
                    public void processElement(Tuple3<String, Long, Long> value, Context
                            ctx, Collector<Tuple2<String, Long>> out) throws Exception {
                        if (sum.value() == null) {
                            // 通过状态变量来判断是不是新事件（也是统计10s内的--10s才触发一次ontimer
                            sum.update(Tuple2.of(value.f0, value.f1));
                            // 注册事件时间定时器（定时器时间设定为10s
                            ctx.timerService().registerEventTimeTimer(value.f2 + 10 *
                                    1000L);
                            timerTs.update(value.f2 + 10 * 1000L);
                        } else {
                            Long cnt = sum.value().f1;
                            sum.update(Tuple2.of(value.f0, cnt + value.f1));
                            if (timerTs.value() == null) {
                                // 判断是有注册器（超10s了，之前的注册器已经删除了，再次注册一个
                                ctx.timerService().registerEventTimeTimer(value.f2 + 10 *
                                        1000L);
                                timerTs.update(value.f2 + 10 * 1000L);
                            }
                        }
                    }
                    @Override
                    public void onTimer(long timestamp, OnTimerContext ctx,
                                        Collector<Tuple2<String, Long>> out) throws Exception {
                        super.onTimer(timestamp, ctx, out);
                        out.collect(Tuple2.of(ctx.getCurrentKey(), sum.value().f1));
                        timerTs.clear();       // 对时间器清空
                    }
                })
                .map(new MapFunction<Tuple2<String, Long>, Tuple3<String, Integer, Long>>()
                {
                    @Override
                    public Tuple3<String, Integer, Long> map(Tuple2<String, Long> value)
                            throws Exception {
                        return Tuple3.of(value.f0.split("-")[0],
                                Integer.parseInt(value.f0.split("-")[1]), value.f1);
                    }
                })
                .keyBy(r -> r.f0)
                .process(new KeyedProcessFunction<String, Tuple3<String, Integer, Long>,
                        Tuple2<String, Long>>() {
                    // TODO:MapState
                    private MapState<Long, Long> mapState;   // 能够记录多个状态
                    @Override
                    public void open(Configuration parameters) throws Exception {
                        // 初始化
                        super.open(parameters);
                        mapState = getRuntimeContext().getMapState(
                                new MapStateDescriptor<Long, Long>("map", Types.LONG,
                                        Types.LONG)
                        );
                    }
                    @Override
                    public void processElement(Tuple3<String, Integer, Long> value, Context
                            ctx, Collector<Tuple2<String, Long>> out) throws Exception {
                        // 放入f1，和f2
                        mapState.put((long)value.f1, value.f2);
                        long sum = 0L;
                        for (Long v : mapState.values()) {
                            sum += v;
                        }
                        out.collect(Tuple2.of(value.f0, sum));
                    }
                })
                .print();
        env.execute();
    }
}
