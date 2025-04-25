package cn.itcast.dataservice.service;

import cn.itcast.dataservice.bean.ItcastDataRateBean;

import java.util.List;


public interface ItcastDataRateService {
    List<ItcastDataRateBean> queryAll(int pageNo, int pageSize);
    Long totalNum();
}
