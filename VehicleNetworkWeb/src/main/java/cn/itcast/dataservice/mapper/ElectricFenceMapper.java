package cn.itcast.dataservice.mapper;

import cn.itcast.dataservice.bean.ElectricFenceBean;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ElectricFenceMapper {
    List<ElectricFenceBean> queryAll(@Param("pageNo") Integer pageNo, @Param("pageSize") Integer pageSize);
    Long totalNum();
}