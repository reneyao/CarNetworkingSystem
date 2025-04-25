package cn.itcast.dataservice.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

/**
  mysql使用jdbcTemplate加载mysql的dataSource数据源访问数据
 */
@Repository
public class MysqlJdbcDao {
    private JdbcTemplate jdbcTemplate;

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    @Autowired
    public void setJdbcTemplate(@Qualifier("mysqlDataSource") DataSource dataSource) {
        // 注入数据源的配置
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }
}