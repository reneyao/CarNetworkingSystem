package cn.itcast.dataservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoReactiveDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;


/**
 * 这个项目配置的数据库有：
 * mysql，mongo（也是nosql数据库，hive
 * @author rene
 * @commpany itcast
 * @Date 2024/9/16 0:48
 * @Description  后台数据服务SpringBoot启动类
 *  @SpringBootApplication: 配置springboot快速启动类注解
 *  @EnableSwagger2 ： 接口开发工具
 *  @MapperScan ： 扫描加载mybatis的接口的包路径
 *
 */
@SpringBootApplication(exclude = {
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class,
        MongoReactiveAutoConfiguration.class,
        MongoReactiveDataAutoConfiguration.class
})
//@SpringBootApplication
@EnableSwagger2
@MapperScan("cn.itcast.dataservice.mapper")
public class DataApplication {
// localhost:8021/swagger-ui.html    http://localhost:8021/v2/api-docs
    // 进入页面：http://localhost:8021/index.html(已经移除）
    // 能够启动，但是无法连接到mogodb，考虑修改连接redis  后续移除mogodb
    // 检验：http://localhost:8021/hello
    public static void main(String[] args) {
        SpringApplication.run(DataApplication.class, args);
    }

}