package com.reneyao.offline.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class testlg {

        public static void main(String[] args) {
            Logger logger = LoggerFactory.getLogger(testlg.class.getSimpleName());

            logger.error("This is an info message");
        }

}
