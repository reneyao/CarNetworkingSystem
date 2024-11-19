package cn.itcast.dataservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
@SpringBootApplication
@EnableSwagger2
@MapperScan("cn.itcast.dataservice.mapper")
public class DataApplication {
// localhost:8021/swagger-ui.html    http://localhost:8021/v2/api-docs
    // 进入页面：http://localhost:8021/index.html
    // 能够启动，但是无法连接到mogodb，考虑修改连接mysql
    public static void main(String[] args) {
        SpringApplication.run(DataApplication.class, args);
    }

}