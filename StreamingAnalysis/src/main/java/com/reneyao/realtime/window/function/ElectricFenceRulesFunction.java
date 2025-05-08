package com.reneyao.realtime.window.function;

import com.reneyao.realtime.entity.ElectricFenceModel;
import com.reneyao.realtime.entity.ItcastDataObj;
import com.reneyao.realtime.utils.DateUtil;
import com.reneyao.realtime.utils.DistanceCaculateUtil;
import com.reneyao.realtime.bean.ElectricFenceResultTmp;
import org.apache.flink.streaming.api.functions.co.CoFlatMapFunction;
import org.apache.flink.util.Collector;

import java.util.HashMap;

/**
 * 自定义coFlatMapFunction的函数对象，实现原始车辆和电子围栏广播流数据的合并，返回电子围栏规则模型流数据
 */
/**
 * 自定义coFlatMapFunction的函数对象，实现原始车辆和电子围栏广播流数据的合并，返回电子围栏规则模型流数据
 */
public class ElectricFenceRulesFunction implements CoFlatMapFunction<ItcastDataObj, HashMap<String, ElectricFenceResultTmp>, ElectricFenceModel> {
    //定义电子围栏广播流数据的对象
    HashMap<String, ElectricFenceResultTmp> vehicalInfoMap = new HashMap<>();

    /**
     * 作用于itcastJsonDataStream流的flatmap操作
     * @param dataPartObj
     * @param collector
     * @throws Exception
     */
    @Override
    public void flatMap1(ItcastDataObj dataPartObj, Collector<ElectricFenceModel> collector) throws Exception {
        //判断经度、维度、gpsTime判断，是否为空，如不为空才可以进行数据处理
        if(dataPartObj.getLng() != 0 && dataPartObj.getLat() != 0 && dataPartObj.getLng() != -999999.0 && dataPartObj.getLat() != -999999.0
                && !dataPartObj.getGpsTime().isEmpty()){
            // 对数据进行验证
            System.out.println("验证通过, dataPartObj："+dataPartObj);
            ElectricFenceModel fenceModel = new ElectricFenceModel();
            //根据车架id得到该车架id对应的电子围栏规则
            ElectricFenceResultTmp electricFenceResultTmp = vehicalInfoMap.get(dataPartObj.getVin());
            // 返回这个vin id对应的电子围栏规则
            System.out.println("当前vin的电子围栏规则:"+electricFenceResultTmp);
            System.out.println("已存在的电子围栏信息关联vehicalInfoMap:"+vehicalInfoMap);

            //如果当前车辆是被监控的车辆
            if(electricFenceResultTmp!=null){
                //获取gpsTime（位置时间）判断是否在电子围栏的有效时间内
                long eventTime = DateUtil.convertStringToDateTime(dataPartObj.getGpsTime()).getTime();
                System.out.println("eventTime:"+eventTime+", EndTime:"+electricFenceResultTmp.getEndTime().getTime()+", StartTime:"+electricFenceResultTmp.getStartTime());
                if(eventTime< electricFenceResultTmp.getEndTime().getTime() && eventTime > electricFenceResultTmp.getStartTime().getTime()){
                    //车辆的位置时间在电子围栏的有效时间内
                    fenceModel.setVin(dataPartObj.getVin());
                    fenceModel.setGpsTime(dataPartObj.getGpsTime());
                    //车辆的位置经度
                    fenceModel.setLng(dataPartObj.getLng());
                    //车辆的维度
                    fenceModel.setLat(dataPartObj.getLat());
                    //车辆终端时间
                    fenceModel.setTerminalTime(dataPartObj.getTerminalTime());
                    fenceModel.setTerminalTimestamp(dataPartObj.getTerminalTimeStamp());
                    //电子围栏id
                    fenceModel.setEleId(electricFenceResultTmp.getId());
                    //电子围栏名称
                    fenceModel.setEleName(electricFenceResultTmp.getName());
                    //电子围栏地址
                    fenceModel.setAddress(electricFenceResultTmp.getAddress());
                    //电子围栏半径
                    fenceModel.setRadius(electricFenceResultTmp.getRadius());
                    //电子围栏经度
                    fenceModel.setLongitude(electricFenceResultTmp.getLongitude());
                    //电子围栏的维度
                    fenceModel.setLatitude(electricFenceResultTmp.getLatitude());
                    //计算车辆位置和电子围栏中心点的距离
                    Double distance = DistanceCaculateUtil.getDistance(
                            electricFenceResultTmp.getLatitude(), electricFenceResultTmp.getLongitude(),
                            dataPartObj.getLat(), dataPartObj.getLng());
                    //判断当前车辆是否在电子围栏范围内，半径单位是：km
                    if(distance/ 1000 < electricFenceResultTmp.getRadius()){
                        //在电子围栏以内
                        fenceModel.setNowStatus(0);
                    }else{
                        //在电子围栏以外
                        fenceModel.setNowStatus(1);
                    }
                    System.out.println("关联成功，返回-fenceModel："+fenceModel);
                    //TODO 返回数据
                    collector.collect(fenceModel);
                }  else{
                    System.out.println("电子围栏规则有效时间不在原始数据的位置时间内，eventTime："+eventTime+"，electricFenceResultTmp："+electricFenceResultTmp);
                }
            } else{
                System.out.println("没有获取到匹配的车辆电子围栏配置信息，vin："+dataPartObj.getVin()+"，ElectricFenceResultTmp：" + electricFenceResultTmp);
            }
        }
        else{
            System.out.println("验证失败，dataPartObj："+dataPartObj);
        }
    }

    /**
     * 作用于electricFenceVinsStream流的flatmap操作
     * @param electricFenceResultTmpHashMap
     * @param collector
     * @throws Exception
     */
    @Override
    public void flatMap2(HashMap<String, ElectricFenceResultTmp> electricFenceResultTmpHashMap, Collector<ElectricFenceModel> collector) throws Exception {
        //将该方法传递的map对象赋值给全局map对象
        vehicalInfoMap = electricFenceResultTmpHashMap;
        //System.out.println("获取到mysql的电子围栏数据："+vehicalInfoMap);
    }
}
