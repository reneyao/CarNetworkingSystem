package com.reneyao.realtime.tests;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class testlogger {
    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(testlogger.class.getSimpleName());

        logger.error("This is an info message");
    }
}
