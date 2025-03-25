package cn.itcast.streaming.source;

import cn.itcast.utils.DateUtil;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class MysqlElectricFenceSouce extends RichSourceFunction<HashMap<String, ElectricFenceResultTmp>> {
    private static Logger logger = LoggerFactory.getLogger(MysqlElectricFenceSouce.class);
    ParameterTool globalJobParameters;
    //定义connection连接对象
    Connection connection = null;
    //定义statement
    Statement statement = null;
    //定义boolean, 是否运行的标记
    Boolean isRunning = true;

    /**
     * 初始化资源
     * @param parameters
     * @throws Exception
     */
    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        globalJobParameters = (ParameterTool)getRuntimeContext().getExecutionConfig().getGlobalJobParameters();
        //注册驱动
        Class.forName("com.mysql.cj.jdbc.Driver");
        //实例化connection对象
        String url = globalJobParameters.getRequired("jdbc.url");
        String user = globalJobParameters.getRequired("jdbc.user");
        String passWord = globalJobParameters.getRequired("jdbc.password");
        connection = DriverManager.getConnection(url, user, passWord);
        //实例化statement
        statement = connection.createStatement();
    }

    /**
     * 关闭连接，释放资源
     * @throws Exception
     */
    @Override
    public void close() throws Exception {
        super.close();
        if(statement != null) statement.close();
        if(connection!=null) connection.close();
    }

    /**
     * 核心方法
     * @param sourceContext
     * @throws Exception
     */
    @Override
    public void run(SourceContext<HashMap<String, ElectricFenceResultTmp>> sourceContext) throws Exception {
        try {
            while (isRunning){
                //定义返回的hashMap对象
                HashMap<String, ElectricFenceResultTmp> vehicalInfoMap = new HashMap<>();
                ResultSet resultSet = statement.executeQuery("select vin,name,address,radius,longitude,latitude,start_time,end_time,setting.id " +
                        "from test_data.electronic_fence_vins as vins " +
                        // 将electronic_fence_vins中的车辆与电子围栏的数据关联   （筛选status为1的）
                        "INNER JOIN test_data.electronic_fence_setting setting on vins.setting_id=setting.id and status=1");
                while (resultSet.next()){
                    vehicalInfoMap.put(resultSet.getString("vin"),
                            new ElectricFenceResultTmp(
                                    resultSet.getInt("id"),
                                    resultSet.getString("name"),
                                    resultSet.getString("address"),
                                    resultSet.getFloat("radius"),
                                    resultSet.getDouble("longitude"),
                                    resultSet.getDouble("latitude"),
                                    DateUtil.convertStringToDate(resultSet.getString("start_time")),
                                    DateUtil.convertStringToDate(resultSet.getString("end_time"))
                            ));
                }

                if(vehicalInfoMap.isEmpty()){
                    System.out.println("从mysql中的electronic_fence_setting、electronic_fence_vins表中读取相关数据为空.");
                }else{
                    System.out.println("从mysql中的electronic_fence_setting、electronic_fence_vins表中查询到的车辆总数为："+vehicalInfoMap.size());
                }
                resultSet.close();
                System.out.println("vehicalInfoMap："+vehicalInfoMap);
                sourceContext.collect(vehicalInfoMap);
                //两次业务表查询的间隔时间
                System.out.println("vehinfo.millionseconds："+globalJobParameters.getRequired("vehinfo.millionseconds"));
                // 两张mysql的表的查询间隔
                TimeUnit.SECONDS.sleep(Integer.parseInt(globalJobParameters.getRequired("vehinfo.millionseconds")));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            System.out.println(throwables.getMessage());
            logger.error(throwables.getMessage());
        }
    }

    /**
     * 取消操作
     */
    @Override
    public void cancel() {
        isRunning = false;
    }

}
