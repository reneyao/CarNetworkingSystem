package cn.itcast.offline.window.udfWatermark;


import cn.itcast.bean.ItcastDataObj;
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks;
import org.apache.flink.streaming.api.watermark.Watermark;

import javax.annotation.Nullable;
import java.io.Serializable;

// AssignerWithPeriodicWatermarks 过时了，最好使用 WatermarkStrategy
/**
 * 驾驶行程自定义水位线对象：解决数据迟到30秒的问题
 */
public class TripDriveWatermark implements AssignerWithPeriodicWatermarks<ItcastDataObj>, Serializable {
    //  允许最大乱序时间为:30秒（延迟时间
    long maxOutOfOrderness = 1000 * 30;
    // 初始化当前水位线时间戳
    Long currentMaxTimestamp = 0L;

    @Nullable
    @Override
    public Watermark getCurrentWatermark() {
        return new Watermark(currentMaxTimestamp - maxOutOfOrderness);
    }

    @Override
    public long extractTimestamp(ItcastDataObj element, long previousElementTimestamp) {
        currentMaxTimestamp = Math.max(element.getTerminalTimeStamp(), currentMaxTimestamp);
        return element.getTerminalTimeStamp();
    }
}
