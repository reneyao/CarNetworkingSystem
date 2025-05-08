package com.reneyao.offline.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class FlinkDDLGenerator {

    public static void main(String[] args) {
        String url = "jdbc:mysql://120.55.78.114:3306/test_data";
        String user = "root";
        String password = "@Aabc939596";
        String tableName = "vehicle_data";

        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("DESCRIBE " + tableName);

            StringBuilder ddl = new StringBuilder();
            ddl.append("CREATE TABLE ").append(tableName).append(" (\n");

            while (resultSet.next()) {
                String columnName = resultSet.getString("Field");
                String mysqlType = resultSet.getString("Type");
                String flinkType = mapToFlinkType(mysqlType);

                ddl.append("  ").append(columnName).append(" ").append(flinkType).append(",\n");
            }

            // Remove the last comma and newline
            ddl.setLength(ddl.length() - 2);

            ddl.append("\n) WITH (\n")
                    .append("  'connector' = 'jdbc',\n")
                    .append("  'url' = '").append(url).append("',\n")
                    .append("  'table-name' = '").append(tableName).append("',\n")
                    .append("'driver' = 'com.mysql.cj.jdbc.Driver'").append("',\n")
                    .append("  'username' = '").append(user).append("',\n")
                    .append("  'password' = '").append(password).append("'\n")
                    .append(");");

            System.out.println(ddl.toString());

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static String mapToFlinkType(String mysqlType) {
        // 基本的MySQL类型到Flink类型的映射，可以根据需要扩展
        if (mysqlType.contains("int")) {
            return "INT";
        } else if (mysqlType.contains("varchar") || mysqlType.contains("text")) {
            return "STRING";
        } else if (mysqlType.contains("float") || mysqlType.contains("double")) {
            return "DOUBLE";
        } else if (mysqlType.contains("datetime") || mysqlType.contains("timestamp")) {
            return "TIMESTAMP";
        } else if (mysqlType.contains("date")) {
            return "DATE";
        } else {
            return "STRING"; // 默认映射为STRING类型
        }
    }
}
