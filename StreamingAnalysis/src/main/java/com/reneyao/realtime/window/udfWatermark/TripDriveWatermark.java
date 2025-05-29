package com.reneyao.realtime.window.udfWatermark;

import com.reneyao.realtime.entity.ItcastDataObj;
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks;
import org.apache.flink.streaming.api.watermark.Watermark;

import javax.annotation.Nullable;
import java.io.Serializable;

/**
 *  驾驶行程自定义水位线对象：解决数据迟到30秒的问题
 */
public class TripDriveWatermark implements AssignerWithPeriodicWatermarks<ItcastDataObj>, Serializable {
    //  允许最大乱序时间为:30秒（延迟时间，其他数据最多晚到30s
    long maxOutOfOrderness = 1000 * 30;
    //  初始化当前水位线时间戳
    Long currentMaxTimestamp = 0L;

    @Nullable              // 该方法的返回值可能是 null
    @Override
    public Watermark getCurrentWatermark() {
        // 当前时间的最大时间戳减去最大乱序时间戳
        return new Watermark(currentMaxTimestamp - maxOutOfOrderness);  // 会更新
    }

    @Override
    public long extractTimestamp(ItcastDataObj element, long previousElementTimestamp) {
        // 重置当前最大时间
        currentMaxTimestamp = Math.max(element.getTerminalTimeStamp(), currentMaxTimestamp);
        return element.getTerminalTimeStamp();           // 指定事件发生的时间
    }
}
