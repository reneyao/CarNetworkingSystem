package cn.itcast.batch.window.function;


import cn.itcast.bean.ItcastDataObj;
import org.apache.flink.shaded.guava18.com.google.common.collect.Lists;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import java.util.ArrayList;

/**
 * 实现驾驶行程采样的自定义函数开发
 * 针对驾驶行程（某个车辆15分钟内所有的驾驶行程进行数据的获取及格式化）
 */
public class DriveSampleWindowFunction implements WindowFunction<ItcastDataObj, String[], String, TimeWindow> {
    /**
     * 重写apply方法，实现驾驶行程采样逻辑
     * @param key           分流的字段类型
     * @param timeWindow    窗口类型
     * @param iterable      某个车辆15分钟内所有的驾驶行程
     * @param collector     返回数据
     * @throws Exception
     */
    @Override
    public void apply(String key, TimeWindow timeWindow, Iterable<ItcastDataObj> iterable, Collector<String[]> collector) throws Exception {
        // 将迭代器转换为列表
        ArrayList<ItcastDataObj> itcastDataObjArrayList = Lists.newArrayList(iterable);
        if (itcastDataObjArrayList.isEmpty()) {
            System.out.println("传过来的对象内容为空");
            return;

        }
        System.out.println("对象正常");
        // 对列表按时间戳升序排序
        itcastDataObjArrayList.sort((o1, o2) -> Long.compare(o1.getTerminalTimeStamp(), o2.getTerminalTimeStamp()));

        // 获取排序后的第一条数据
        ItcastDataObj firstItcastDataObj = itcastDataObjArrayList.get(0);
        ItcastDataObj lastItcastDataObj = itcastDataObjArrayList.get(itcastDataObjArrayList.size() - 1);

        StringBuilder singleSoc = new StringBuilder(String.valueOf(firstItcastDataObj.getSoc()));
        StringBuilder singleMileage = new StringBuilder(String.valueOf(firstItcastDataObj.getTotalOdometer()));
        StringBuilder singleSpeed = new StringBuilder(String.valueOf(firstItcastDataObj.getVehicleSpeed()));
        StringBuilder gps = new StringBuilder(firstItcastDataObj.getLng() + "|" + firstItcastDataObj.getLat());
        StringBuilder terminalTime = new StringBuilder(String.valueOf(firstItcastDataObj.getTerminalTime()));

        Long startTime = firstItcastDataObj.getTerminalTimeStamp();
        Long endTime = lastItcastDataObj.getTerminalTimeStamp();

        // 遍历窗口内的所有数据
        for (ItcastDataObj itcastDataObj : itcastDataObjArrayList) {
            Long currentTimestamp = itcastDataObj.getTerminalTimeStamp();
            System.out.println("时间计算");
            if ((currentTimestamp - startTime) >= 5 * 1000 || currentTimestamp.equals(endTime)) {
                singleSoc.append(",").append(itcastDataObj.getSoc());
                singleMileage.append(",").append(itcastDataObj.getTotalOdometer());
                singleSpeed.append(",").append(itcastDataObj.getVehicleSpeed());
                gps.append(",").append(itcastDataObj.getLng()).append("|").append(itcastDataObj.getLat());
                terminalTime.append(",").append(itcastDataObj.getTerminalTime());
                startTime = currentTimestamp;
            }
        }

        // 创建结果数组并填充
        String[] result = new String[7];
        result[0] = firstItcastDataObj.getVin();
        result[1] = String.valueOf(firstItcastDataObj.getTerminalTimeStamp());
        result[2] = singleSoc.toString();
        result[3] = singleMileage.toString();
        result[4] = singleSpeed.toString();
        result[5] = gps.toString();
        result[6] = terminalTime.toString();

        collector.collect(result);
    }

}
