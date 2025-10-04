package com.example.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data //lombok生成getter setter方法
@NoArgsConstructor //lombok生成无参构造方法
@AllArgsConstructor //lombok生成全参构造方法
public class User {
    private Integer id;
    private String username;
    private String password;
    private String name;
    private Integer age;
    private LocalDateTime updateTime;
}
