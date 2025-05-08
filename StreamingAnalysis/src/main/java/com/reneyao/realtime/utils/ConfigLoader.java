package com.reneyao.realtime.utils;


// 配置文件的加载类
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 配置文件读取的工具类
 * 配置文件加载工具，加载key在conf.properties文件中对应的值
 */
public class ConfigLoader {
    /**
     * 开发步骤：
     * 1）使用classLoader（类加载器），加载conf.properties
     * 2）使用Properties的load方法加载inputStream
     * 3）编写方法获取配置项的key对应的值
     * 4）编写方法获取int的key值
     */

    //1）使用classLoader（类加载器），加载conf.properties
    private final static InputStream inputStream = ConfigLoader.class.getClassLoader().getResourceAsStream("conf.properties");

    //定义properties
    private  final static Properties props = new Properties();

    // 2）使用Properties的load方法加载inputStream
    static {
        try {
            //加载inputStream-》conf.properties
            props.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 3）编写方法获取配置项的key对应的值
    public static String getProperty(String key) { return  props.getProperty(key); }

    //4）编写方法获取int的key值
    public static Integer getInteger(String key){
        //将获取到的value值转换成int类型返回
        return  Integer.parseInt(props.getProperty(key));
    }
}

