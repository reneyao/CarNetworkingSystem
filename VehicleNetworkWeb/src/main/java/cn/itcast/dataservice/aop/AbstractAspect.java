package cn.itcast.dataservice.aop;

import cn.itcast.dataservice.annotation.LogAudit;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.net.InetAddress;

/**
 *  根据切入点获得被添加注解的方法
 *  根据http请求获得请求客户端的ip地址
 */
@Slf4j
public abstract class AbstractAspect {
    /**
     * @desc 根据切点对象获得使用该注解的实例方法的名称
     * @param joinPoint
     * @return
     */
    protected String getMethodAnnotation(JoinPoint joinPoint){

        Signature signature = joinPoint.getSignature();
        String methodName = signature.getName();
        //  得到方法的参数的类型
        Class[] argClass = ((MethodSignature) signature).getParameterTypes();
        String annotationValue = null;
        try {
            //  获得当前访问的class
            Class<?> className = joinPoint.getTarget().getClass();

            //  得到访问的方法对象
            Method method = className.getMethod(methodName, argClass);

            //  判断是否存在@LogAudit注解
            if (method.isAnnotationPresent(LogAudit.class)) {
                LogAudit annotation = method.getAnnotation(LogAudit.class);
                //  取出注解中的方法注释
                annotationValue = annotation.value();
            }
            /**  判断方法是否使用的是SwagerUI对象中的ApiOperation抽象对象 */
            if(annotationValue.length() < 1 && method.isAnnotationPresent(ApiOperation.class)){
                ApiOperation annotation = method.getAnnotation(ApiOperation.class);
                //  取出注解中的方法注释
                annotationValue = annotation.value();
            }
        } catch (Exception e) {
            log.error(null, e);
            e.printStackTrace();
        }
        return annotationValue;
    }

    /**
     * @desc 根据http请求获得请求的ip地址
     * @param request
     * @return
     */
    protected String getIpAddr(HttpServletRequest request) {
        //  http请求头判断，通过浏览器请求都会有header
        String ip = request.getHeader("x-forwarded-for");
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
            if(ip.equals("127.0.0.1") || "0:0:0:0:0:0:0:1".equals(ip)){
                //  根据网卡取本机配置的IP
                InetAddress inet=null;
                try {
                    inet = InetAddress.getLocalHost();
                } catch (Exception e) {
                    log.debug("获取本机IP失败", e);
                }
                ip= inet.getHostAddress();
            }
        }
        //  多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
        if(ip != null && ip.length() > 15){
            if(ip.indexOf(",")>0){
                ip = ip.substring(0,ip.indexOf(","));
            }
        }
        return ip;
    }
}