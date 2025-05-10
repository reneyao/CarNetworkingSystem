package cn.itcast.annotation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class testlogg {
    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(testlogg.class.getSimpleName());

        logger.error("This is an info message");    // logger 测试没有问题
    }
}
