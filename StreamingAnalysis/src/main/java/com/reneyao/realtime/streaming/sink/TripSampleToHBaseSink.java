package com.reneyao.realtime.streaming.sink;

import com.reneyao.realtime.utils.ConfigLoader;
import com.reneyao.realtime.utils.DateUtil;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.BufferedMutator;
import org.apache.hadoop.hbase.client.BufferedMutatorParams;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.mapreduce.TableInputFormat;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.hbase.client.Connection;

// hbase连接
/**
 * 将驾驶行程采样数据写入到hbase表中，作为采样分析的数据源
 */
public class TripSampleToHBaseSink extends RichSinkFunction<String[]> {
    //定义日志操作对象
    private final static Logger logger = LoggerFactory.getLogger(TripSampleToHBaseSink.class);
    //定义操作的hbase的表名
    private String tableName;
    //定义connection连接对象
    private Connection connection;
    //定义BufferedMutator对象  这个是缓存对象，多个结果才接入一次hbase
    private BufferedMutator bufferedMutator;
    //定义列族的名称
    private String cf = "cf";

    public TripSampleToHBaseSink(String tableName){
        this.tableName = tableName;
    }

    /**
     * 初始化资源
     * @param parameters
     * @throws Exception
     */
    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        // 1）定义hbase的连接配置对象
        org.apache.hadoop.conf.Configuration configuration = HBaseConfiguration.create();
        configuration.set("hbase.zookeeper.quorum", ConfigLoader.getProperty("zookeeper.quorum"));
        configuration.set("hbase.zookeeper.property.clientPort", ConfigLoader.getProperty("zookeeper.clientPort"));
        configuration.set(TableInputFormat.INPUT_TABLE, tableName);
        // 2）创建hbase的连接对象     连接对象的问题

        connection = ConnectionFactory.createConnection(configuration);
        //实例化BufferedMutator所需要的参数
        BufferedMutatorParams params = new BufferedMutatorParams(TableName.valueOf(tableName));
        //写缓存的大小
        params.writeBufferSize(128*1024*1024);
//        bufferedMutator = connection.getMetaData(params);
        logger.warn("获得hbase的连接对象，{}表对象初始化成功！", tableName);
    }

    /**
     * 每条数据调用一次该方法
     * @param value
     * @param context
     * @throws Exception
     */
    @Override
    public void invoke(String[] value, Context context) throws Exception {
        Put put = markPut(value);
        //将put对象追加到缓冲区
        bufferedMutator.mutate(put);
        //缓冲区写满后，刷到hbase中
        bufferedMutator.flush();
    }

    //生成put对象
    private Put markPut(String[] value){
        //生成rowkey
        String rowKey = value[0] + value[1];
        Put put = new Put(Bytes.toBytes(rowKey));      // 也是7个元素
        put.addColumn(Bytes.toBytes(cf), Bytes.toBytes("soc"), Bytes.toBytes(value[2]));
        put.addColumn(Bytes.toBytes(cf), Bytes.toBytes("mileage"), Bytes.toBytes(value[3]));   // 里程
        put.addColumn(Bytes.toBytes(cf), Bytes.toBytes("speed"), Bytes.toBytes(value[4]));
        put.addColumn(Bytes.toBytes(cf), Bytes.toBytes("gps"), Bytes.toBytes(value[5]));   // 地理位置
        put.addColumn(Bytes.toBytes(cf), Bytes.toBytes("terminalTime"), Bytes.toBytes(value[6]));
        put.addColumn(Bytes.toBytes(cf), Bytes.toBytes("processTime"), Bytes.toBytes(DateUtil.getCurrentDate()));
        return put;
    }


    /**
     * 释放资源
     * @throws Exception
     */
    @Override
    public void close() throws Exception {
        super.close();
        //释放资源的时候需要将缓冲区的数据递交一次hbase，否则数据可能会被丢失
        if(bufferedMutator!=null) bufferedMutator.close();
        if(connection!=null) connection.close();
    }
}