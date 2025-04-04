package cn.itcast.window.function;

import cn.itcast.entity.ItcastDataObj;
import cn.itcast.entity.TripModel;
import org.apache.flink.shaded.guava18.com.google.common.collect.Lists;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.util.ArrayList;

/**
 * 实现驾驶行程数据的计算逻辑
 */
public class DriveTripWindowFunction implements WindowFunction<ItcastDataObj, TripModel, String, TimeWindow> {
    /**
     * 窗口划分的每一个车辆15分钟内的行程，计算行程结果，返回TripModel对象
     * @param key
     * @param timeWindow
     * @param iterable
     * @param collector
     * @throws Exception
     */
    @Override
    public void apply(String key, TimeWindow timeWindow, Iterable<ItcastDataObj> iterable, Collector<TripModel> collector) throws Exception {
        //窗口内的数据有水位线，因此需要对窗口内的数据进行排序，否则拼接出来的数据是不准确的
        //1：先将迭代器转换成集合对象
        ArrayList<ItcastDataObj> itcastDataObjArrayList = Lists.newArrayList(iterable);
        // 2：对每一个会话窗口内的元素进行排序操作
        itcastDataObjArrayList.sort(((o1, o2) -> {
            //如果第一个元素对象的TerminalTimeStamp，大于第二个元素对象的TerminalTimeStamp
            if(o1.getTerminalTimeStamp()> o2.getTerminalTimeStamp()){
                //升序排序，就会交换两个对象的值
                return  1;
            }else if(o1.getTerminalTimeStamp() < o2.getTerminalTimeStamp()){
                return  -1;
            }else{
                return 0;
            }
        }));
//        TripModel returnState = new TripModel();
        TripModel returnState = getTripModel(itcastDataObjArrayList);
        //返回转换后的javaBean对象
        collector.collect(returnState);
    }


    /**
     * 驾驶行程指标计算
     * @param itcastDataObjArrays
     * @return
     */
    private TripModel getTripModel(ArrayList<ItcastDataObj> itcastDataObjArrays) {
        //定义需要返回的JavaBean对象
        TripModel tripModel = new TripModel();
        //1：从第一条数据中得到
        ItcastDataObj firstItcastDataObj = itcastDataObjArrays.get(0);

        //  vin（车架号）、
        tripModel.setVin(firstItcastDataObj.getVin());
        //  tripStartTime（行程开始时间）、
        tripModel.setTripStartTime(firstItcastDataObj.getTerminalTime());
        //  start_BMS_SOC（行程开始Soc）、
        tripModel.setStart_BMS_SOC(firstItcastDataObj.getSoc());
        //  start_longitude（行程开始经度）、
        tripModel.setStart_longitude(firstItcastDataObj.getLng());
        //  start_latitude（行程开始维度）、
        tripModel.setStart_latitude(firstItcastDataObj.getLat());
        //  start_mileage（行程开始表显里程数）
        tripModel.setStart_mileage(firstItcastDataObj.getTotalOdometer());

        // 2：从最后一条数据中得到
        ItcastDataObj endItcastDataObj = itcastDataObjArrays.get(itcastDataObjArrays.size() - 1);
        //  tripEndTime（行程结束时间）、
        tripModel.setTripEndTime(endItcastDataObj.getTerminalTime());
        //  end_BMS_SOC（行程结束soc）、
        tripModel.setEnd_BMS_SOC(endItcastDataObj.getSoc());
        //  end_longitude（行程结束经度）、
        tripModel.setEnd_longitude(endItcastDataObj.getLng());
        //  end_latitude（行程结束维度）、
        tripModel.setEnd_latitude(endItcastDataObj.getLat());
        //  end_mileage（行程结束表显里程数）
        tripModel.setEnd_mileage(endItcastDataObj.getTotalOdometer());
        //  mileage（行程驾驶公里数）、
        tripModel.setMileage(Math.abs(endItcastDataObj.getTotalOdometer() - firstItcastDataObj.getTotalOdometer()));
        //  time_comsuption（行程消耗时间）、这里存储的是分钟数
        tripModel.setTime_comsuption(
                Math.abs((endItcastDataObj.getTerminalTimeStamp() - firstItcastDataObj.getTerminalTimeStamp())/1000/60D));
        //  lastSoc（上次的行程Soc）、将当前行程开始的电量消耗百分比作为上一个行程结束的电量消耗百分比
        tripModel.setLastSoc(firstItcastDataObj.getSoc() + 0D);
        //  lastMileage（上次的里程数）
        tripModel.setLastMileage(firstItcastDataObj.getTotalOdometer());

        // 3：遍历list，计算得到
        itcastDataObjArrays.forEach(itcastDataObj -> {
            // 获取每条数据的速度
            Double speed = itcastDataObj.getSpeed();
            // 获取上次行程报文的soc(剩余电量百分比)
            Double lastSoc = tripModel.getLastSoc();
            //计算每条数据的soc与lastSoc进行比较（剩余电量百分比：上次行程剩余电量-当前行程的当前数据的剩余电量百分比）
            Double socDiff = lastSoc - itcastDataObj.getSoc();
            // soc_comsuption（行程soc消耗）、两次上报的数据的soc消耗可能差额非常所以有小数的存在，因此对数据进行四舍五入
            if(socDiff > 0) {
                tripModel.setSoc_comsuption(Math.abs(socDiff));
            }

            // max_speed（最高行驶车速）、最高车速异常值的处理，速度超过150，最高速度设置为0
            //  if(speed > 150){
            //      tripModel.setMax_speed(0D);
            //  }

            //如果javaBean对象保存的最大车速小于当前数据的车速，那么将当前数据的车速覆盖掉javabean对象存储的最大车速
            if(tripModel.getMax_speed() < speed && speed < 150){
                //正常车速
                tripModel.setMax_speed(speed);
            }

            //低速行驶
            if(speed >= 0 && speed < 40){
                // total_low_speed_nums（低速行驶个数）、
                tripModel.setTotal_low_speed_nums(tripModel.getTotal_low_speed_nums()+1);
                // low_BMS_SOC（低速soc消耗）、(最后一条数据的低速油耗+每次低速消耗的油耗)
                tripModel.setLow_BMS_SOC(tripModel.getLow_BMS_SOC() + Math.abs(tripModel.getLastSoc() - itcastDataObj.getSoc()));
                // low_BMS_Mileage（低速里程）、(最后一条数据的低速里程+每次低速里程的差)
                tripModel.setLow_BMS_Mileage(tripModel.getLow_BMS_Mileage() + Math.abs(itcastDataObj.getMileageInformation() - tripModel.getLastMileage()));
            }
            //中速行驶
            if(speed >= 40 && speed < 80){
                // total_medium_speed_nums（中速行驶个数）、
                tripModel.setTotal_medium_speed_nums(tripModel.getTotal_medium_speed_nums()+1);
                // medium_BMS_SOC（中速soc消耗）、、(最后一条数据的中速油耗+每次中速消耗的油耗)
                tripModel.setMedium_BMS_SOC(tripModel.getMedium_BMS_SOC() + Math.abs(tripModel.getLastSoc() - itcastDataObj.getSoc()));
                // medium_BMS_Mileage（中速里程）、(最后一条数据的中速里程+每次中速里程的差)
                tripModel.setMedium_BMS_Mileage(tripModel.getMedium_BMS_Mileage() + Math.abs(itcastDataObj.getMileageInformation() - tripModel.getLastMileage()));
            }
            //高速行驶
            if(speed > 80 && speed < 150){
                // total_high_speed_nums（高速行驶个数）、
                tripModel.setTotal_high_speed_nums(tripModel.getTotal_high_speed_nums()+1);
                // high_BMS_SOC（高速soc消耗）、、(最后一条数据的高速油耗+每次高速消耗的油耗)
                tripModel.setHigh_BMS_SOC(tripModel.getHigh_BMS_SOC() + Math.abs(tripModel.getLastSoc() - itcastDataObj.getSoc()));
                // high_BMS_Mileage（高速里程）、、(最后一条数据的高速里程+每次高速里程的差)
                tripModel.setHigh_BMS_Mileage(tripModel.getHigh_BMS_Mileage() + Math.abs(itcastDataObj.getMileageInformation() - tripModel.getLastMileage()));
            }

            // lastSoc（上次的soc）、
            tripModel.setLastSoc(itcastDataObj.getSoc() + 0D);
            // lastMileage（上次的里程）
            tripModel.setLastMileage(itcastDataObj.getMileageInformation()+0D);
        });

        //4：增加扩展字段，判断是否有异常数据
        if(itcastDataObjArrays.size() > 1){
            //正常行程
            tripModel.setTripStatus(0);
        }else{
            //异常行程
            tripModel.setTripStatus(1);
        }

        return tripModel;
    }
}
