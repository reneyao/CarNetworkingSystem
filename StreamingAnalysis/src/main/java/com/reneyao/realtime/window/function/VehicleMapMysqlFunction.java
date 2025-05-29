package com.reneyao.realtime.window.function;

import com.reneyao.realtime.entity.OnlineDataObj;
import com.reneyao.realtime.entity.VehicleModel;
import com.reneyao.realtime.utils.ConfigLoader;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.CoFlatMapFunction;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.HashMap;

/**
 * 自定实现数据源的获取
 * 加载车型车系及车辆销售信息数据（8个字段）
 */
/**
 * 自定义在线故障分析的窗口数据与mysql数据库数据的关联
 */
public class VehicleMapMysqlFunction implements CoFlatMapFunction<OnlineDataObj,
        HashMap<String, VehicleModel>,
        OnlineDataObj> {
    //定义车辆相关信息Map对象
    HashMap<String, VehicleModel> vehicalInfoMap = new HashMap<String, VehicleModel>();

    /**
     * 针对第一个数据集的操作：窗口流数据
     * @param onlineDataObj
     * @param collector
     * @throws Exception
     */
    @Override
    public void flatMap1(OnlineDataObj onlineDataObj, Collector<OnlineDataObj> collector) throws Exception {
        //根据车架号找到对应的车辆信息
        VehicleModel vehicalInfo = vehicalInfoMap.get(onlineDataObj.getVin());
        if(vehicalInfo!=null){
            //车系
            onlineDataObj.setSeriesName(vehicalInfo.getSeriesName());
            //车型
            onlineDataObj.setModelName(vehicalInfo.getModelName());
            //年限(单位:月，未查到数据显示-1)
            onlineDataObj.setLiveTime(vehicalInfo.getLiveTime());
            //销售日期
            onlineDataObj.setSalesDate(vehicalInfo.getSalesDate());
            //车辆类型
            onlineDataObj.setCarType(vehicalInfo.getCarType());

            //返回数据
            collector.collect(onlineDataObj);
        }else{
            System.out.println("没有找到匹配的车辆数据，vin："+onlineDataObj.getVin());
        }
    }

    /**
     * 针对第二个数据集的操作：mysql的车辆广播流数据
     * @param inVehicalInfoMap
     * @param collector
     * @throws Exception
     */
    @Override
    public void flatMap2(HashMap<String, VehicleModel> inVehicalInfoMap, Collector<OnlineDataObj> collector) throws Exception {
        this.vehicalInfoMap = inVehicalInfoMap;
    }
}
