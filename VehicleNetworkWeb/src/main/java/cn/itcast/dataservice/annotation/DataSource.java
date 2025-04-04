package cn.itcast.dataservice.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author laowei
 * @commpany itcast
 * @Date 2020/9/16 0:49
 * @Description 自定义数据源注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface DataSource {
    // 有多个数据源，动态选择数据源
    //  value成员变量，应为注解修饰类型为“Method”，即日定义注解内容为value方法，default是设置默认值
    String value() default MYSQL;
    //  定义mysql数据源key=value
    String MYSQL = "mysqlDataSource";
    //  定义hive数据源key=value
    String HIVE = "hiveDataSource";
}