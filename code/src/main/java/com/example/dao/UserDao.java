package com.example.dao;

import java.util.List;

public interface UserDao {
    /**
     * 加载用户数据
     * @return
     */
    public List<String> findAll();
}
