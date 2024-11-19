package cn.itcast.dataservice.service;

/**
 * @author laowei
 * @commpany itcast
 * @Date 2020/9/17 23:09
 * @Description 车辆可视化页面服务
 */
public interface VehicleViewService {
    //  统计平台车辆总数
    Long totalNum();
    //  统计平台在线车辆
    Long onlineNum();
    //  统计平台行驶中的车辆
    Integer drivingNum();
}