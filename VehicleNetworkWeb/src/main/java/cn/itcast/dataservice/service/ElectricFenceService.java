package cn.itcast.dataservice.service;

import cn.itcast.dataservice.bean.ElectricFenceBean;

import java.util.List;

/**
  电子围栏后台数据服务接口,服务接口类
 */
public interface ElectricFenceService {
    List<ElectricFenceBean> queryAll(Integer pageNo, Integer pageSize);
    Long totalNum();
}