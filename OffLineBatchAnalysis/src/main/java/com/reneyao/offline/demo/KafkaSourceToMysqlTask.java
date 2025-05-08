package com.reneyao.offline.demo;


import com.reneyao.offline.bean.ItcastDataObj;
import com.reneyao.offline.utils.JsonParseUtil;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

// 成功进数据了(从kafka读取数据落入到mysql，用的table api
public class KafkaSourceToMysqlTask {
    // 打印log日志
    private static final Logger LOG = LoggerFactory.getLogger(KafkaSourceToMysqlTask.class);

    public static void main(String[] args) throws Exception {

    // 创建流处理环境
    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
    // 创建table环境
    EnvironmentSettings bsSettings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build();
    StreamTableEnvironment bsTableEnv = StreamTableEnvironment.create(env, bsSettings);

    // 开启checkpoint ：检查点，操作快照
        env.enableCheckpointing(30*1000);  // 30s为间隔
        env.getCheckpointConfig().

    setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);  // 保证每条数据消费一次，数据不重复消费
    // 设置两次checkpoint的时间间隔
        env.getCheckpointConfig().

    setMinPauseBetweenCheckpoints(20000);
    // 设置checkpoint的超时时间
        env.getCheckpointConfig().

    setCheckpointTimeout(20000);
        env.getCheckpointConfig().

    setMaxConcurrentCheckpoints(1);
    //设置checkpoint取消的时候，是否保留checkpoint，checkpoint默认会在job取消的时候删除
        env.getCheckpointConfig().

    enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
    //设置执行job过程中，保存检查点错误时，job不失败
        env.getCheckpointConfig().

    setFailOnCheckpointingErrors(false);

    // 设置重启策略：设置为不重启的策略
        env.setRestartStrategy(RestartStrategies.noRestart());

    // 创建flink消费Kafka数据的对象
    Properties props = new Properties();
        props.setProperty("bootstrap.servers","120.55.78.114:9092");
        props.setProperty("group.id","test1");   // 消费者的组id
        props.setProperty("auto.offset.reset","earliest");
        props.setProperty("enable.auto.commit","false");

    FlinkKafkaConsumer<String> vehicledata = new FlinkKafkaConsumer<>(
            "vehiclejsondata",
            new SimpleStringSchema(),
            props
    );
    vehicledata.setStartFromEarliest();  // 从最开始消费

    vehicledata.setCommitOffsetsOnCheckpoints(true);
    DataStreamSource<String> streamSource = env.addSource(vehicledata);

    // 将json字符串解析成对象  借助map一一对应转化
    SingleOutputStreamOperator<ItcastDataObj> itcastDataObjStream = streamSource.map(JsonParseUtil::parseJsonToObject);
    itcastDataObjStream.print("解析后的数据>>>");



    bsTableEnv.createTemporaryView("itcastDataTable",itcastDataObjStream);

    String a = "CREATE TABLE vehicle_data_test (\n" +
            "    gearDriveForce INT ,\n" +
            "    batteryConsistencyDifferenceAlarm INT ,\n" +
            "    soc INT ,\n" +
            "    socJumpAlarm INT ,\n" +
            "    caterpillaringFunction INT ,\n" +
            "    satNum INT ,\n" +
            "    socLowAlarm INT ,\n" +
            "    chargingGunConnectionState INT ,\n" +
            "    minTemperatureSubSystemNum INT ,\n" +
            "    chargedElectronicLockStatus INT ,\n" +
            "    maxVoltageBatteryNum INT ,\n" +
            "    terminalTime VARCHAR(255) ,\n" +
            "    singleBatteryOverVoltageAlarm INT ,\n" +
            "    otherFaultCount INT ,\n" +
            "    vehicleStorageDeviceOvervoltageAlarm INT ,\n" +
            "    brakeSystemAlarm INT ,\n" +
            "    serverTime VARCHAR(255) ,\n" +
            "    vin VARCHAR(255) \n" +
            ")";
    // 创建 MySQL 输出表（数据类型要对上）
        bsTableEnv.executeSql(
                a +
                " WITH ("+
                "'connector' = 'jdbc',"+
                "'url' = 'jdbc:mysql://120.55.78.114:3306/test_data?characterEncoding=utf-8&useSSL=false',"+
                "'table-name' = 'vehicle_data_test',"+
                "'driver' = 'com.mysql.cj.jdbc.Driver',"+
                "'username' = 'root',"+
                "'password' = '@Aabc939596',"+
                "'sink.max-retries' = '5'"+
                ")"
                );

    // 插入数据
        bsTableEnv.executeSql("INSERT INTO vehicle_data_test SELECT gearDriveForce ,\n" +
                "    batteryConsistencyDifferenceAlarm ,\n" +
                "    soc ,\n" +
                "    socJumpAlarm ,\n" +
                "    caterpillaringFunction ,\n" +
                "    satNum ,\n" +
                "    socLowAlarm ,\n" +
                "    chargingGunConnectionState ,\n" +
                "    minTemperatureSubSystemNum ,\n" +
                "    chargedElectronicLockStatus ,\n" +
                "    maxVoltageBatteryNum ,\n" +
                "    terminalTime  ,\n" +
                "    singleBatteryOverVoltageAlarm ,\n" +
                "    otherFaultCount ,\n" +
                "    vehicleStorageDeviceOvervoltageAlarm ,\n" +
                "    brakeSystemAlarm ,\n" +
                "    serverTime  ,\n" +
                "    vin  FROM itcastDataTable");

    // 执行任务
        env.execute("Kafka to MySQL");
}
}
