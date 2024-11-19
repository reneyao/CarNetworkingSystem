package cn.itcast.dataservice.aop;

import cn.itcast.dataservice.annotation.DataSource;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;


// 切面实现
/**
 * @author laowei
 * @commpany itcast
 * @Date 2020/9/16 0:51
 * @Description  定义动态数据源切面实现类
 *               @Aspect：定义切面类注解
 *               @Component：当前类创建对象，交给spring管理
 */
@Aspect
@Component                // 使其成为spring 管理的Bean
public class DynamicDataSourceAspect {

    private final Logger logger = LoggerFactory.getLogger(DynamicDataSourceAspect.class);
    //  配置切入点
    @Pointcut("@annotation(cn.itcast.dataservice.annotation.DataSource)")
    public void annotationPointCut() { }
    // 进入切入点后，执行通知方法，最先执行此方法
    @Before("annotationPointCut()")
    public void beforeSwitchDataSource(JoinPoint point) {
        //  获得切入点实例对象
        Signature signature = point.getSignature();
        //  获得访问的方法名
        String methodName = signature.getName();
        //  得到方法的参数的类型
        Class[] argClass = ((MethodSignature) signature).getParameterTypes();
        // 数据名称
        String dataSource = null;
        try {
            // 获得当前访问的class
            Class<?> className = point.getTarget().getClass();

            //  得到访问的方法对象
            Method method = className.getMethod(methodName, argClass);

            //  判断是否存在@DataSource注解
            if (method.isAnnotationPresent(DataSource.class)) {
                DataSource annotation = method.getAnnotation(DataSource.class);
                // 取出注解中的数据源名
                dataSource = annotation.value();
            }
        } catch (Exception e) {
            logger.error(null, e);
            e.printStackTrace();
        }

        //  切换数据源
        DynamicDataSource.setDataSource(dataSource);

    }

    //  出切入点之前，最后执行此方法
    @After("annotationPointCut()")
    public void afterSwitchDataSource(JoinPoint point) {
        DynamicDataSource.clearDataSource();
    }
}