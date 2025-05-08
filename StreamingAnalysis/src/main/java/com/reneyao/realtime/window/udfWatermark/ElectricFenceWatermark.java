package com.reneyao.realtime.window.udfWatermark;

import com.reneyao.realtime.entity.ElectricFenceModel;
import com.reneyao.realtime.utils.DateUtil;
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks;
import org.apache.flink.streaming.api.watermark.Watermark;

import javax.annotation.Nullable;
import java.io.Serializable;

// 4.1 自定义水印 解决相邻两条数据乱序问题
public class ElectricFenceWatermark implements AssignerWithPeriodicWatermarks<ElectricFenceModel>, Serializable {

    Long currentMaxTimestamp = 0L;
    @Nullable
    @Override
    public Watermark getCurrentWatermark() {
        // 获得的水位线：最大事件时间-延迟时间
        return new Watermark(currentMaxTimestamp);
    }

    @Override
    public long extractTimestamp(ElectricFenceModel element, long previousElementTimestamp) {
//        currentMaxTimestamp = Math.max(element.getTerminalTimestamp(), currentMaxTimestamp);
        //        return element.getTerminalTimestamp();

        // 测试使用
        // TerminalTimestamp 时间为事件时间
        // terminalTime=2021-03-05 16:03:00, terminalTimestamp=1606924800000 一个是date，一个是时间戳
        // "yyyy-MM-dd HH:mm:ss"
        long tempTime = DateUtil.dateToTimestamp(element.getTerminalTime(),"yyyy-MM-dd HH:mm:ss");
        currentMaxTimestamp = Math.max(tempTime, currentMaxTimestamp);

        return tempTime;
    }
}
