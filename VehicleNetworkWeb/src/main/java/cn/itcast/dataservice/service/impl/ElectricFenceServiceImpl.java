package cn.itcast.dataservice.service.impl;

import cn.itcast.dataservice.bean.ElectricFenceBean;
import cn.itcast.dataservice.mapper.ElectricFenceMapper;
import cn.itcast.dataservice.service.ElectricFenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class ElectricFenceServiceImpl implements ElectricFenceService {

    @Autowired
    private ElectricFenceMapper electricFenceMapper;

    @Override
    public List<ElectricFenceBean> queryAll(Integer pageNo, Integer pageSize) {
        // 物理分页，mysql查询从索引'0'开始，所以用页码数减1
        if (pageNo <= 1) {
            pageNo = 0;
        } else {
            pageNo -= 1;
        }
        return electricFenceMapper.queryAll(pageNo * pageSize, pageSize);
    }

    @Override
    public Long totalNum() {
        return electricFenceMapper.totalNum();
    }
}
