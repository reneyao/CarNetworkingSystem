package cn.itcast.dataservice.aop;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 *  动态数据源切换实现类
 */
public class DynamicDataSource extends AbstractRoutingDataSource {

    // 定义一个本地的线程
    // JDK中sql包下，数据库数据源接口，可以各种获得数据库连接
    private static final ThreadLocal<String> dataSources = new InheritableThreadLocal<>();

    //  设置数据源对象
    public static void setDataSource(String dataSource) {
        dataSources.set(dataSource);
    }

    public static void clearDataSource() {
        dataSources.remove();
    }

    //  检查数据绑定的数据源上下文信息
    @Override
    protected Object determineCurrentLookupKey() {
        return dataSources.get();
    }

}
