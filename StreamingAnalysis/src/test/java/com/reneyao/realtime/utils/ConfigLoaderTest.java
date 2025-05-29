package com.reneyao.realtime.utils;
// 测试工具类

public class ConfigLoaderTest {
    public static void main(String[] args) {
        String key = "hdfsUri";
        String s = ConfigLoader.getProperty(key);
        System.out.println(s);

    }
}
