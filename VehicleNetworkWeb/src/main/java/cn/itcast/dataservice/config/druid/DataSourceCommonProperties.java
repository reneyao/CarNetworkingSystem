package cn.itcast.dataservice.config.druid;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *  创建springboot中关于数据源的公共属性映射对象：第四级对象
 */
@Getter
@Setter
// ConfigurationProperties  配置类，从yml文件中过来配置
@ConfigurationProperties(prefix = "spring.datasource.commonconfig", ignoreUnknownFields = false)
public class DataSourceCommonProperties {

    // 各种通用配置

    // 用于对应yaml文件中的各种配置
    private int initialSize = 10;
    private int minIdle;
    private int maxActive;
    private int maxWait;
    private int timeBetweenEvictionRunsMillis;
    private int minEvictableIdleTimeMillis;
    private String validationQuery;
    private boolean testWhileIdle;
    private boolean testOnBorrow;
    private boolean testOnReturn;
    private boolean poolPreparedStatements;
    private int maxOpenPreparedStatements;
    private String filters;
    private String mapperLocations;

}