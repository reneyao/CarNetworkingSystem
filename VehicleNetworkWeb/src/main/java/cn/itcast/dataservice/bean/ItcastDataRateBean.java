package cn.itcast.dataservice.bean;

import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

/**
 *  数据准确率和错误率计算后台数据服务接口对象
 */
@Getter
@Setter
public class ItcastDataRateBean {
    /**  记录序列号 */
    private String seriesNo;
    /** 原数据正确数据总数 */
    private BigInteger srcTotalNum;
    /**  原数据错误数据总数 */
    private BigInteger errorSrcTotalNum;
    /**原始数据正确率 */
    private Float dataAccuracy;
    /** 原始数据错误率 */
    private Float dataErrorRate;
    /** 记录计算时间 */
    private String processDate;
}