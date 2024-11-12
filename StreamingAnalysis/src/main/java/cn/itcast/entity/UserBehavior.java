package cn.itcast.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

// POJO类
@Data
@NoArgsConstructor
public class UserBehavior {

    //    $("userId"),
//    $("itemId"),
//    $("categoryId"),
//    $("behavior"),
//    $("timestamp").rowtime().as("ts")
    public String userId;
    public String itemId;
    public String categoryId;
    public String behavior;
    public Long ts;


    public UserBehavior(String userId, String itemId, String categoryId, String behavior, Long ts) {
        this.userId = userId;
        this.itemId = itemId;
        this.categoryId = categoryId;
        this.behavior = behavior;
        this.ts = ts;
    }
}
