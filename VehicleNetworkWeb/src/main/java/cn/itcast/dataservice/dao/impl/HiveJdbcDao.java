package cn.itcast.dataservice.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

/**
 * @author laowei
 * @commpany itcast
 * @Date 2020/9/16 1:09
 * @Description hive使用jdbcTemplate加载hive的dataSource数据源访问数据
 */
@Repository
public class HiveJdbcDao {
    private JdbcTemplate jdbcTemplate;

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    @Autowired
    // TODO： 根据@Qualifier("hiveDataSource") DataSource dataSource  来调用那个数据源头，要配置好hive
    public void setJdbcTemplate(@Qualifier("hiveDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }
}