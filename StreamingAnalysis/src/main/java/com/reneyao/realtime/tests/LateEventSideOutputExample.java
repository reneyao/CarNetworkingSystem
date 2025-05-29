package com.reneyao.realtime.tests;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;


import java.time.Duration;
import java.util.Arrays;

public class LateEventSideOutputExample {

    // 定义事件类
    public static class Event {
        public String id;
        public int value;
        public long timestamp;

        public Event() {}

        public Event(String id, int value, long timestamp) {
            this.id = id;
            this.value = value;
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return "Event{" +
                    "id='" + id + '\'' +
                    ", value=" + value +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }

    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        // 侧输出标签，用于接收晚到事件
        final OutputTag<Event> lateOutputTag = new OutputTag<Event>("late-events"){};

        // 模拟输入数据，包含正常事件和晚到事件
        Event[] events = new Event[]{
                new Event("A", 1, 1000L),
                new Event("B", 2, 2000L),
                new Event("C", 3, 3000L), // 正常事件
                new Event("D", 4, 1500L)  // 晚到事件，事件时间小于之前的
        };

        SingleOutputStreamOperator<String> windowedStream = env
                .fromCollection(Arrays.asList(events))
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy.<Event>forBoundedOutOfOrderness(Duration.ofSeconds(2))
                                .withTimestampAssigner(new SerializableTimestampAssigner<Event>() {
                                    @Override
                                    public long extractTimestamp(Event element, long recordTimestamp) {
                                        return element.timestamp;
                                    }
                                })
                )
                .keyBy(event -> event.id)
                .timeWindow(Time.seconds(5))
                .allowedLateness(Time.seconds(2))  // 允许2秒迟到
                .sideOutputLateData(lateOutputTag) // 晚到数据侧输出
                .process(new ProcessWindowFunction<Event, String, String, TimeWindow>() {
                    @Override
                    public void process(String key, Context context, Iterable<Event> elements, Collector<String> out) {
                        long windowStart = context.window().getStart();
                        long windowEnd = context.window().getEnd();
                        int count = 0;
                        for (Event e : elements) {
                            count++;
                        }
                        out.collect("Key: " + key + ", Window: [" + windowStart + "," + windowEnd + ") count=" + count);
                    }
                });

        // 打印正常窗口结果
        windowedStream.print("Window Result");

        // 获取并打印晚到事件侧输出流
        windowedStream.getSideOutput(lateOutputTag).print("Late Event");

        env.execute("Flink Late Event Side Output Example");
    }
}
