package com.reneyao.realtime.window.function;

import com.alibaba.fastjson.JSON;
import com.reneyao.realtime.entity.OnlineDataObj;
import com.reneyao.realtime.entity.VehicleLocationInfo;
import com.reneyao.realtime.utils.GeoHashUtil;
import com.reneyao.realtime.utils.RedisUtil;
import org.apache.flink.api.common.functions.MapFunction;

/**
 * 自定义flatMap函数
 * 将窗口流数据与车辆基础表的数据进行关联，关联后的结果返回
 */
public class VehicleInfoRedisFunction implements MapFunction<OnlineDataObj, OnlineDataObj> {
    @Override
    public OnlineDataObj map(OnlineDataObj onlineDataObj) throws Exception {
        //根据经度和维度获取到geoHash（可以将geohash作为redis的key进行存储）
        String geoHash = GeoHashUtil.encode(onlineDataObj.getLat(), onlineDataObj.getLng());
        byte[] locationInfo = RedisUtil.get(geoHash.getBytes());
        if(locationInfo!=null) {
            System.out.println("locationInfo.toString():" + new String(locationInfo));
            VehicleLocationInfo vehicleLocationInfo = JSON.parseObject(new String(locationInfo), VehicleLocationInfo.class);

            if (vehicleLocationInfo != null) {
                onlineDataObj.setProvince(vehicleLocationInfo.getProvince());
                onlineDataObj.setCity(vehicleLocationInfo.getCity());
                onlineDataObj.setCounty(vehicleLocationInfo.getCounty());
            } else {
                onlineDataObj.setProvince(null);
                onlineDataObj.setCity(null);
                onlineDataObj.setCounty(null);
                System.out.println("根据geohash没有获取到对应的省份、城市和地区，geoHash：" + geoHash);
            }
        }
        //返回数据
        return onlineDataObj;
    }
}
