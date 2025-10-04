package com.example.dao.impl;

import cn.hutool.core.io.IoUtil;
import com.example.dao.UserDao;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
//@Component //表示该类为组件，会被Spring IOC容器管理
@Repository("userDao") //表示该类为数据访问层，会被Spring IOC容器管理 | userDao为bean的自定义id，默认为类名首字母小写
public class UserDaoImpl implements UserDao {
    @Override
    public List<String> findAll() {
        //1.加载读取user.txt文件 获取用户数据
        InputStream in =this.getClass().getClassLoader().getResourceAsStream("user.txt");
        ArrayList<String> lines = IoUtil.readLines(in, StandardCharsets.UTF_8,new ArrayList<>());
        return lines;
    }
}
