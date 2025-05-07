package cn.itcast.dataservice.controller;

import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class HelloController {

    @GetMapping("/")
    @ApiOperation(value = "检测是否正常启动")
    public String helloWorld() {
        return "Hello World";
    }
}
