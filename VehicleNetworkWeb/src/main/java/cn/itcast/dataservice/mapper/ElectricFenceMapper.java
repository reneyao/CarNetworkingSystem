package cn.itcast.dataservice.mapper;

import cn.itcast.dataservice.bean.ElectricFenceBean;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author rene
 * @commpany itcast
 * @Date 2024/9/16 2:04
 * @Description  电子围栏后台数据服务接口Mapper接口，对应xml文件名称
 */
@Repository
public interface ElectricFenceMapper {
    List<ElectricFenceBean> queryAll(@Param("pageNo") Integer pageNo, @Param("pageSize") Integer pageSize);
    Long totalNum();
}