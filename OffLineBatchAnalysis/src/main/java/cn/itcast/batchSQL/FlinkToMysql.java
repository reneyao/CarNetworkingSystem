package cn.itcast.batchSQL;


import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
// flink java io 的jdbc
import org.apache.flink.api.java.io.jdbc.JDBCInputFormat;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.types.Row;

// JDBCInputFormat 格式输入，使用createInput（）转化格式
public class FlinkToMysql {
    public static void main(String[] args) throws Exception {
        // 批处理环境
        ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        // 全局工具
        ParameterTool parameterTool = ParameterTool.fromPropertiesFile(FlinkToMysql.class.getClassLoader().getResourceAsStream("conf.properties"));
        env.getConfig().setGlobalJobParameters(parameterTool);

        String SQL = "select * from user_info";
            // JDBCInputFormat 格式
        JDBCInputFormat test1 = JDBCInputFormat.buildJDBCInputFormat()
                // 数据库连接驱动名称
                .setDrivername(parameterTool.getRequired("jdbc.driver"))
                // 数据库连接驱动名称
                .setDBUrl(parameterTool.getRequired("jdbc.url"))
                // 数据库连接用户名
                .setUsername(parameterTool.getRequired("jdbc.user"))
                // 数据库连接密码
                .setPassword(parameterTool.getRequired("jdbc.password"))
                // 数据库连接查询SQL
                .setQuery(SQL)
                // 字段类型,顺序个个数必须与SQL保持一致
                .setRowTypeInfo(new RowTypeInfo(BasicTypeInfo.STRING_TYPE_INFO, BasicTypeInfo.INT_TYPE_INFO, BasicTypeInfo.STRING_TYPE_INFO))
                .setFetchSize(10)
                .finish();
        // dataset格式
        DataSet<Row> jdbcInputFormat = env.createInput(test1);

        // 测试flink任务加载mysql数据源
        jdbcInputFormat.print();


    }
}
