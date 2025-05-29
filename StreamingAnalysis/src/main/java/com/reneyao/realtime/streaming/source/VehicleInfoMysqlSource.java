package com.reneyao.realtime.streaming.source;

import com.reneyao.realtime.entity.VehicleModel;
import com.reneyao.realtime.utils.ConfigLoader;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.HashMap;

/**
 * 自定实现数据源的获取
 * 加载车型车系及车辆销售信息数据（8个字段）
 */
public class VehicleInfoMysqlSource extends RichSourceFunction<HashMap<String, VehicleModel>> {
    //定义日志打印的对象
    private Logger logger = LoggerFactory.getLogger(VehicleInfoMysqlSource.class);
    ParameterTool globalJobParameters;
    //定义连接对象
    private Connection connection = null;
    //定义preparedStatement
    private PreparedStatement pstmt = null;
    //定义是否运行的标记
    private boolean isRunning = true;

    /**
     * 初始化资源
     * @param parameters
     * @throws Exception
     */
    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        Class.forName("com.mysql.jdbc.Driver");
        globalJobParameters = (ParameterTool)getRuntimeContext().getExecutionConfig().getGlobalJobParameters();
        //实例化connection对象
        String url = globalJobParameters.getRequired("jdbc.url");
        String user = globalJobParameters.getRequired("jdbc.user");
        String passWord = globalJobParameters.getRequired("jdbc.password");
        connection = DriverManager.getConnection(url, user, passWord);
        String querySql = "select t12.vin,t12.series_name,t12.model_name,t12.series_code,t12.model_code,t12.nick_name,t3.sales_date,t4.car_type\n" +
                " from (\n" +
                "select t1.vin, t1.series_name, t2.show_name as model_name, t1.series_code,t2.model_code,t2.nick_name,t1.vehicle_id\n" +
                " from vehicle_networking.dcs_vehicles t1 left join vehicle_networking.t_car_type_code t2 on t1.model_code = t2.model_code) t12\n" +
                " left join  (select vehicle_id, max(sales_date) sales_date from vehicle_networking.dcs_sales group by vehicle_id) t3\n" +
                " on t12.vehicle_id = t3.vehicle_id\n" +
                " left join\n" +
                " (select tc.vin,'net_cat' car_type from vehicle_networking.t_net_car tc\n" +
                " union all select tt.vin,'taxi' car_type from vehicle_networking.t_taxi tt\n" +
                " union all select tp.vin,'private_car' car_type from vehicle_networking.t_private_car tp\n" +
                " union all select tm.vin,'model_car' car_type from vehicle_networking.t_model_car tm) t4\n" +
                " on t12.vin = t4.vin";
        pstmt = connection.prepareStatement(querySql);
    }

    /**
     * 释放资源
     * @throws Exception
     */
    @Override
    public void close() throws Exception {
        super.close();
        if(pstmt != null) pstmt.close();
        if(connection !=null) connection.close();
    }

    /**
     * 核心方法
     * @param sourceContext
     * @throws Exception
     */
    @Override
    public void run(SourceContext<HashMap<String, VehicleModel>> sourceContext) throws Exception {
        while (isRunning) {
            ResultSet resultSet = pstmt.executeQuery();
            HashMap<String, VehicleModel> vehicleInfoMap = new HashMap<>();
            while (resultSet.next()) {
                VehicleModel vehicleModel = new VehicleModel();
                //车架号
                String vin = resultSet.getString("vin");
                //车系
                String seriesName = resultSet.getString("series_name");
                //车型
                String modelName = resultSet.getString("model_name");
                //车系编码
                String seriesCode = resultSet.getString("series_code");
                //车型编码
                String modelCode = resultSet.getString("model_code");
                //车辆类型简称
                String nickName = resultSet.getString("nick_name");
                //出售日期
                String salesDate = resultSet.getString("sales_date");
                //车辆用途
                String carType = resultSet.getString("car_type");

                //年限
                String liveTime = "-1";
                if (salesDate != null) {
                    //当前日期-售出日期=使用年限
                    liveTime = String.valueOf((new Date().getTime() - resultSet.getDate("sales_date").getTime()) / 1000 / 3600 / 24 / 365);
                }
                if (null == vin) {
                    vin = "未知";
                }
                if (null == seriesName) {
                    seriesName = "未知";
                }
                if (null == modelName) {
                    modelName = "未知";
                }
                if (null == seriesCode) {
                    seriesCode = "未知";
                }
                if (null == modelCode) {
                    modelCode = "未知";
                }
                if (null == nickName) {
                    nickName = "未知";
                }
                if (null == salesDate) {
                    salesDate = "未知";
                }
                if (null == carType) {
                    carType = "未知";
                }

                vehicleModel.setSeriesName(seriesName);
                vehicleModel.setSeriesCode(seriesCode);
                vehicleModel.setModelName(modelName);
                vehicleModel.setModelCode(modelCode);
                vehicleModel.setLiveTime(liveTime);
                vehicleModel.setNickName(nickName);
                vehicleModel.setCarType(carType);
                vehicleModel.setSalesDate(salesDate);
                //将车辆基础数据封装到集合返回
                vehicleInfoMap.put(vin, vehicleModel);
            }
            if (vehicleInfoMap.isEmpty()) {
                logger.warn("从车辆基础数据表中查询数据为空...");
            } else {
                sourceContext.collect(vehicleInfoMap);
            }

            resultSet.close();
            //设置多久查询一次数据
            Thread.sleep(ConfigLoader.getInteger("vehinfo.millionseconds"));
        }
    }

    /**
     * 取消方法
     */
    @Override
    public void cancel() {
        isRunning = false;
    }
}


