package cn.itcast.dataservice.config.request;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * @author rene
 * @commpany itcast
 * @Date 2020/9/17 23:05
 * @Description 后台解决json跨域请求
 */
@Configuration
public class CorsConfig {
    private CorsConfiguration buildConfig() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        // 对所有地址都可以访问
        corsConfiguration.addAllowedOrigin("*");
        //  跨域请求头
        corsConfiguration.addAllowedHeader("*");
        //  关于请求方法
        corsConfiguration.addAllowedMethod("*");
        //  跨域请求 允许获得同一个 session
        // corsConfiguration.setAllowCredentials(true);
        return corsConfiguration;
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 配置可以访问的地址(路径)
        source.registerCorsConfiguration("/**", buildConfig());
        return new CorsFilter(source);
    }
}