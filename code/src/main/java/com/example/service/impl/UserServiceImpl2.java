/*
package com.example.service.impl;

import com.example.dao.UserDao;
import com.example.pojo.User;
import com.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
//@Primary // 表示优先使用UserServiceImpl2
//@Component //表示该类为组件，会被Spring IOC容器管理
@Service //表示该类为服务层，会被Spring IOC容器管理
public class UserServiceImpl2 implements UserService {
    //private UserDao userDao = new UserDaoImpl();//还存在耦合问题->解耦 使用注解@Service/@Component

    @Autowired //注入UserDaoImpl对象 应用程序运行时，会自动查询该类型bean对象，并赋值给UserDao成员变量
    private UserDao userDao;
    @Override
    public List<User> findAll() {
        //1. 调用Dao层，查询用户数据
        List<String> lines=userDao.findAll();
        //2.解析用户信息，封装为user对象 -> list集合
        List<User> userList = lines.stream().map(line->{
            String[] paths= line.split(",");
            Integer id = Integer.parseInt(paths[0]);
            String username = paths[1];
            String password = paths[2];
            String name = paths[3];
            Integer age = Integer.parseInt(paths[4]);
            LocalDateTime updateTime = LocalDateTime.parse(paths[5], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return (User)new User(id+200,username,password,name,age,updateTime);
        }).collect(Collectors.toList());//toList()jdk16以后能用
        return userList;
    }
}
*/
