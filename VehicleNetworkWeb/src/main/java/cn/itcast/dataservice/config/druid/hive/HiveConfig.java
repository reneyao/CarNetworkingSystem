package cn.itcast.dataservice.config.druid.hive;

import cn.itcast.dataservice.config.druid.DataSourceCommonProperties;
import cn.itcast.dataservice.config.druid.DataSourceProperties;
import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * @author laowei
 * @commpany itcast
 * @Date 2020/9/16 1:08
 * @Description  Hive配置信息对应的对象
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({DataSourceProperties.class, DataSourceCommonProperties.class})
public class HiveConfig {

    @Autowired
    private DataSourceProperties dataSourceProperties;

    @Autowired
    private DataSourceCommonProperties dataSourceCommonProperties;

    /**
     * @desc  根据hive配置信息获得dataSource对象
     * @return DataSource(hiveDataSource)
     */
    // rene：原是Bean("hiveDataSource")，改动为@Autowired
    @Bean(name = "hiveDataSource")
    @Qualifier("hiveDataSource")
    // TODO:返回的都是DataSource对象，调用时候通过Qualifier标识来确定调用的是那个数据源，需要配置好数据源
    public DataSource dataSource(){
        DruidDataSource datasource = new DruidDataSource();

        // 配置数据源属性
        datasource.setUrl(dataSourceProperties.getHive().get("url"));
        datasource.setUsername(dataSourceProperties.getHive().get("username"));
        datasource.setPassword(dataSourceProperties.getHive().get("password"));
        datasource.setDriverClassName(dataSourceProperties.getHive().get("driver-class-name"));

        // 配置统一属性
        datasource.setInitialSize(dataSourceCommonProperties.getInitialSize());
        datasource.setMinIdle(dataSourceCommonProperties.getMinIdle());
        datasource.setMaxActive(dataSourceCommonProperties.getMaxActive());
        datasource.setMaxWait(dataSourceCommonProperties.getMaxWait());
        datasource.setTimeBetweenEvictionRunsMillis(dataSourceCommonProperties.getTimeBetweenEvictionRunsMillis());
        datasource.setMinEvictableIdleTimeMillis(dataSourceCommonProperties.getMinEvictableIdleTimeMillis());
        datasource.setValidationQuery(dataSourceCommonProperties.getValidationQuery());
        datasource.setTestWhileIdle(dataSourceCommonProperties.isTestWhileIdle());
        datasource.setTestOnBorrow(dataSourceCommonProperties.isTestOnBorrow());
        datasource.setTestOnReturn(dataSourceCommonProperties.isTestOnReturn());
        datasource.setPoolPreparedStatements(dataSourceCommonProperties.isPoolPreparedStatements());
        try {
            datasource.setFilters(dataSourceCommonProperties.getFilters());
        } catch (SQLException e) {
            log.error("Druid configuration initialization filter error.", e);
        }
        return datasource;
    }
}
