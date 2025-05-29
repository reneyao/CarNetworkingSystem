package com.reneyao.offline.demo.flinkToMysqlDemo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

// 测试hive连接，查询和写入
public class TestFlinkOperatorHive {

    public static void main(String[] args) throws Exception {
        // 读取数据示例
        readDataFromHive();
        
        // 调用写入方法
//        insertDataToHive("1004", "Zhang San", 8000);
    }
    
    /**
     * 向Hive表中插入一条数据
     * 
     * @param id 员工ID
     * @param name 员工姓名
     * @param salary 薪资
     * @throws Exception SQL或连接异常
     */
    public static void insertDataToHive(String id, String name, int salary) throws Exception {
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            // 加载hive驱动
            Class.forName("org.apache.hive.jdbc.HiveDriver");
            
            // 连接hive数据库
            conn = DriverManager.getConnection("jdbc:hive2://hadoop102:10000/mydb", "rene", "123456");
            
            // 准备插入语句
            String insertSql = "INSERT INTO empt (id, name, salary) VALUES (?, ?, ?)";
            
            // 创建预处理语句
            pstmt = conn.prepareStatement(insertSql);
            
            // 设置参数
            pstmt.setString(1, id);
            pstmt.setString(2, name);
            pstmt.setInt(3, salary);
            
            // 执行插入
            pstmt.executeUpdate();
            
            System.out.println("成功向Hive表empt插入一条数据: id=" + id + ", name=" + name + ", salary=" + salary);
            
        } catch (Exception e) {
            System.err.println("插入数据到Hive失败: " + e.getMessage());
            throw e;
        } finally {
            // 关闭资源
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (Exception e) {
                    // 忽略关闭异常
                }
            }
            
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    // 忽略关闭异常
                }
            }
        }
    }
    
    // 将原有的main方法逻辑提取到这个方法中
    public static void readDataFromHive() throws Exception {
        Connection conn = null;
        PreparedStatement pstm = null;
        ResultSet rs = null;
        
        try {
            //加载hive驱动
            Class.forName("org.apache.hive.jdbc.HiveDriver");
            //连接hive数据库
            conn = DriverManager.getConnection("jdbc:hive2://hadoop102:10000/mydb", "rene", "123456");
            String sql = "select * from empt";
            pstm = conn.prepareStatement(sql);
            rs = pstm.executeQuery();
            while(rs.next()){
                String id = rs.getString("id");
                String name = rs.getString("name");
                int salary = rs.getInt("salary");
                System.out.println(id+":"+name+":"+salary);
            }
        } finally {
            // 关闭资源
            if (rs != null) {
                rs.close();
            }
            if (pstm != null) {
                pstm.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
    }
}
