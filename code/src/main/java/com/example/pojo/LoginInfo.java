package com.example.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 登录成功结果封装类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginInfo {
    private Integer id;//员工ID
    private String username;//用户名
    private String name;//姓名
    private Integer age;//年龄
    private LocalDateTime updateTime;//更新时间
    private String token;//令牌
}
