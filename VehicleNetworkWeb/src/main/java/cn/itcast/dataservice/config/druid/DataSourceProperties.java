package cn.itcast.dataservice.config.druid;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 *  数据配置类的中央管理
 * @author rene
 * @commpany itcast
 * @Date 2024/9/16 1:03
 * @Description  存放DataSource属性对象，第三级对象
 *                  @ConfigurationProperties： 这个注解配置之后，意味着会自定去springboot的默认文件
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "spring.datasource", ignoreUnknownFields = false)
public class DataSourceProperties {
    // 将application.yml文件中的数据源配置映射到java对象中，方便各个配置类，取得配置信息（中央管理）
    private Map<String,String> mysql;
    private Map<String,String> hive;
    private Map<String,String> commonconfig;
}