package com.example.controller;

import com.example.pojo.LoginInfo;
import com.example.pojo.Result;
import com.example.pojo.User;
import com.example.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class LoginController {
    @Autowired
    private UserService userService;
    @PostMapping("/login")
    public Result login(@RequestBody User user){
        log.info("login...");
        LoginInfo loginInfo=userService.login(user);
        if(loginInfo!=null){
            return Result.success(loginInfo);
        }
        return Result.error("wrong");
    }
}
