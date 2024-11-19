package cn.itcast.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

// 日期处理类，根据枚举类来设定
import cn.itcast.entity.DateFormatDefine;
/**
 * 简单的时间处理类
 * 包含功能：
 * 1. 转换成 yyyy-MM-dd HH:mm:ss
 * 2. 转换成 yyyyMMdd
 * 3. 转换成 yyyyMMdd日期格式的日期
 * 4. 转换成 yyyy-MM-dd 格式的日期
 * 5. 转换成 yyyy-MM-dd HH:mm:ss 格式的日期时间
 * 6. 传入日期时间转换成日期字符串
 */
public class DateUtil {
    /**
     *  1、直接获得当前日期，格式：“yyyy-MM-dd HH:mm:ss”
     * @return
     */
    public static String getCurrentDateTime(){
        // DATE_TIME_FORMAT.getFormat()  私有方法调用
        return new SimpleDateFormat(DateFormatDefine.DATE_TIME_FORMAT.getFormat()).format(new Date());
    }

    /**
     * 2、直接获得当前日期，格式：”yyyyMMdd”
     * @return
     */
    public static String getCurrentDate(){
        return new SimpleDateFormat(DateFormatDefine.DATE_FORMAT.getFormat()).format(new Date());
    }
    /**
     *  3、字符串日期格式转换，传入参数格式：“yyyyMMdd”，转成Date类型
     * @param str
     * @return
     */
    public static Date convertStringToDate(String str) {
        Date date = null;
        try {
            //注意SimpleDateFormat是线程非安全的，因此使用的时候必须要每次创建一个新的实例才可以
            SimpleDateFormat formatter = new SimpleDateFormat(DateFormatDefine.DATE_FORMAT.getFormat());
            date = formatter.parse(str);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    /**
     * 4、字符串日期格式转换，传入参数格式：“yyyy-MM-dd”，转成Date类型
     * @param str
     * @return
     */
    public static Date convertDateStrToDate(String str){
        Date date = null;
        try {
            //注意：SimpleDateFormat是线程非安全的，因此使用的时候每次都必须要创建一个实例
            SimpleDateFormat format = new SimpleDateFormat(DateFormatDefine.DATE2_FORMAT.getFormat());
            date = format.parse(str);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    /**
     *  5、字符串日期格式转换，传入参数格式：“yyyy-MM-dd HH:mm:ss”，转成Date类型
     * @param str
     * @return
     */
    public static Date convertStringToDateTime(String str){
        Date date = null;
        try {
            //注意SimpleDateFormat是线程非安全的，因此使用的时候必须要每次创建一个新的实例才可以
            date = new SimpleDateFormat(DateFormatDefine.DATE_TIME_FORMAT.getFormat()).parse(str);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    /**
     * 6、字符串日期格式转换，传入参数格式：”yyyy-MM-dd HH:mm:ss“，转成”yyyyMMdd”格式
     * @param str
     * @return
     */
    public static String convertStringToDateString(String str){
        String dateStr = null;
        //第一步：先将日期字符串转换成日期对象
        Date date = convertStringToDateTime(str);
        //第二步：再将日期对象转换成指定的日期字符串
        dateStr = new SimpleDateFormat(DateFormatDefine.DATE_FORMAT.getFormat()).format(date);
        return dateStr;
    }

    /**
     * 将日期格式的转化为毫秒级的时间戳
     * @param dateStr
     * @param format
     * @return
     */
    public static long dateToTimestamp(String dateStr, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            Date date = sdf.parse(dateStr);
            // long类型
            return date.getTime();
        } catch (ParseException e) {
            System.err.println("日期格式解析错误: " + e.getMessage());
            return -1;
        }
    }
}