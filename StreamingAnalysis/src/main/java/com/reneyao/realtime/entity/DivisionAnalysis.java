package com.reneyao.realtime.entity;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DivisionAnalysis {
    // 对应mysql的表t_division_result
    private String vin;
    private String name;
    private String analyzeValue1;
    private String analyzeValue2;
    private String analyzeValue3;
    private float analyzeType;
    private String terminalTime;

}

