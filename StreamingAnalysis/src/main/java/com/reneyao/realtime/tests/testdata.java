package com.reneyao.realtime.tests;

import com.reneyao.realtime.entity.ItcastDataObj;
import com.reneyao.realtime.utils.DateUtil;

public class testdata {
    public static void main(String[] args) {

        String currentDateTime = DateUtil.getMillisKey();

//        System.out.println(currentDateTime);

        ItcastDataObj itcastDataObj = new ItcastDataObj();
        // 确实是返回毫秒级别的时间戳   存在许多操作时间相同的操作？
//        System.out.println(DateUtil.convertStringToDate(itcastDataObj.getTerminalTime()).getTime());

        System.out.println(DateUtil.convertStringToDateTime("2019-11-20 15:34:01"));

        // 使用的方法是有问题的，不能正确获得毫秒  导致rowkey的值不对  TODO
        System.out.println(DateUtil.convertStringToDateTime("2019-11-20 15:34:01").getTime());

        System.out.println(DateUtil.convertStringToDateTime("2019-11-20 15:34:00").getTime());





    }
}
