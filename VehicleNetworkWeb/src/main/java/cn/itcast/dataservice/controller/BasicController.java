package cn.itcast.dataservice.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * @author <a href="mailto:chenxilzx1@gmail.com">theonefx</a>
 */
//@CrossOrigin(origins = "*",allowCredentials="true",allowedHeaders = "",methods = {})
@Controller
public class BasicController {

    // http://127.0.0.1:8021/hello?name=lisi
    @RequestMapping("/hello")
    @ResponseBody
    public String hello(@RequestParam(name = "name", defaultValue = "unknown user") String name) {
        return "Hello " + name;
    }


    // http://127.0.0.1:8021/html
    @RequestMapping("/Bihtml")
    public String html() {
        // Bi.html文件没有正常运行
        return "Bi2.html";
    }



    // http://127.0.0.1:8021/finehtml
    @RequestMapping("/finehtml")
    public String finehtml() {
        return "fine.html";
    }
}

