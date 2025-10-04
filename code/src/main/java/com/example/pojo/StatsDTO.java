package com.example.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsDTO {
    /**
     * 总用户数
     */
    private Long totalUsers;

    /**
     * 今日新增用户数
     */
    private Integer todayAdded;

    /**
     * 活跃用户数
     */
    private Integer activeUsers;

    /**
     * 平均年龄
     */
    private Double avgAge;
}
