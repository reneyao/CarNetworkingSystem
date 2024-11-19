package cn.itcast.dataservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

// 这个代码是直接导入的
/**
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
// http://localhost:8021/swagger-ui.html#/
    // 此模块主要用于学习使用swagger写数据接口，bi可视化可以使用自己熟悉的fineReport
    public static void main(String[] args) {
        SpringApplication.run(DataApplication.class, args);
    }

}