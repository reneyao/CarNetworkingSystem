package com.reneyao.realtime.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleLocationInfo {

    private String province;

    private String city;

    private String county;

    private Double longitude;

    private Double latitude;

    private String geohash;

}
