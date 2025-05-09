package com.reneyao.realtime.streaming.sink;

import com.reneyao.realtime.utils.DateUtil;
import com.reneyao.realtime.entity.ItcastDataObj;
import com.reneyao.realtime.utils.MyStringUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.mapreduce.TableInputFormat;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class VehicleDetailSinkOptimizer extends RichSinkFunction<ItcastDataObj> {
    // 自定义三个方法 open  invoke  close

    private static Logger logger = LoggerFactory.getLogger(VehicleDetailSinkOptimizer.class.getSimpleName());

    private String tableName;
    private Connection conn = null;
    private BufferedMutator mutator = null;    // hbase客户端中的数据写缓存对象
    private String cf = "cf";

    public VehicleDetailSinkOptimizer(String tableName) {
        this.tableName = tableName;
    }


    @Override
    public void open(Configuration parameters) throws Exception {
        // 初始化连接
        super.open(parameters);
        ParameterTool parameterTool = (ParameterTool) getRuntimeContext().getExecutionConfig().getGlobalJobParameters();
        // 定义hbase的连接配置对象
        org.apache.hadoop.conf.Configuration configuration = HBaseConfiguration.create();
        configuration.set("hbase.zookeeper.quorum", parameterTool.getRequired("zookeeper.quorum"));
        configuration.set("hbase.zookeeper.property.clientPort", parameterTool.getRequired("zookeeper.clientPort"));
        configuration.set(TableInputFormat.INPUT_TABLE, tableName);
        // 设置当前连接默认的schema（命名空间）  ： 没有指定的时候
        configuration.set("hbase.client.default.schema", "GMALL");
        // 创建hbase的连接对象
        conn = ConnectionFactory.createConnection(configuration);
        //定义BufferedMutatorParams的参数对象   指定需要写入的表
        // 需要指定“GMALL”命名空间
        BufferedMutatorParams params = new BufferedMutatorParams(TableName.valueOf("GMALL", tableName));
//        BufferedMutatorParams params = new BufferedMutatorParams(TableName.valueOf( tableName));
        params.writeBufferSize(128*1024*1024);
        //实例化bufferedMutator对象实例
        mutator = conn.getBufferedMutator(params);
        logger.warn("获得hbase的连接对象，{}表对象初始化成功", tableName);

    }

    @Override
    public void close() throws Exception {
        super.close();
        if(mutator!=null) mutator.close();
        logger.warn("没有继续获取到数据，关闭hbase的连接对象");
        if(conn != null) conn.close();

    }


    // 逐行处理数据
    @Override
    public void invoke(ItcastDataObj value, Context context) throws Exception {
        // 处理数据

        //将每条数据写入到hbase中，需要将itcastDataObject转换成Put对象
        Put put = setDataSourcePut(value);

        mutator.mutate(put);
        //强制刷新数据到hbase中
        mutator.flush();

    }


    private Put setDataSourcePut(ItcastDataObj itcastDataObj) {
        //确定rowkey
        String rowKey = itcastDataObj.getVin() + MyStringUtil.reverse(itcastDataObj.getTerminalTimeStamp().toString());
        Put put = new Put(Bytes.toBytes(rowKey));   // phoenix建表需要注意PK,"ROWKEY" 
        //这两个列一定不为空，如果为空就不是正常数据了
        put.addColumn(Bytes.toBytes(cf), Bytes.toBytes("vin"), Bytes.toBytes(itcastDataObj.getVin()));
        put.addColumn(Bytes.toBytes(cf), Bytes.toBytes("terminalTime"), Bytes.toBytes(itcastDataObj.getTerminalTime()));

        //电量百分比(currentElectricity)、当前电量(remainPower)、百公里油耗(fuelConsumption100km)、
        // 发动机速度(engineSpeed)、车辆速度(vehicleSpeed)
        if(itcastDataObj.getCurrentElectricity() != -999999D){
            put.addColumn(Bytes.toBytes(cf), Bytes.toBytes("currentElectricity"), Bytes.toBytes(itcastDataObj.getCurrentElectricity()+""));
        }
        if(itcastDataObj.getRemainPower() != -999999D){
            put.addColumn(Bytes.toBytes(cf), Bytes.toBytes("remainPower"), Bytes.toBytes(itcastDataObj.getRemainPower()+""));
        }
        // org.apache.commons.lang.StringUtils的方法来判断不是空值
        if(StringUtils.isNotEmpty(itcastDataObj.getFuelConsumption100km()) ){
            put.addColumn(Bytes.toBytes(cf), Bytes.toBytes("fuelConsumption100km"), Bytes.toBytes(itcastDataObj.getFuelConsumption100km()));
        }
        if(StringUtils.isNotEmpty(itcastDataObj.getEngineSpeed()) ){
            put.addColumn(Bytes.toBytes(cf), Bytes.toBytes("engineSpeed"), Bytes.toBytes(itcastDataObj.getEngineSpeed()));
        }
        if(itcastDataObj.getVehicleSpeed() != -999999D){
            put.addColumn(Bytes.toBytes(cf), Bytes.toBytes("vehicleSpeed"), Bytes.toBytes(itcastDataObj.getVehicleSpeed()+""));
        }
        put.addColumn(Bytes.toBytes(cf), Bytes.toBytes("processTime"), Bytes.toBytes(DateUtil.getCurrentDateTime()));

        //返回put对象
        return  put;
    }

}


