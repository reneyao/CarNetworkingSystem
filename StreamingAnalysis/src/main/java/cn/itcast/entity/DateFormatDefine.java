package cn.itcast.entity;

// 日期格式化的枚举类
public enum DateFormatDefine {
    DATE_TIME_FORMAT("yyyy-MM-dd HH:mm:SS"),
    DATE_FORMAT("yyyyMMdd"),
    DATE2_FORMAT("yyyy-MM-dd");


    private String format;
    DateFormatDefine(String _format) {
        this.format = _format;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
