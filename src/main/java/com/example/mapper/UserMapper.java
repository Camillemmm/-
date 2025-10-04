package com.example.mapper;

import com.example.pojo.LoginInfo;
import com.example.pojo.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper {
    //@Select("select id, username, password, name, age,updatetime from user")
    List<User> findAll();

    @Select("select * from user where username=#{username} and password=#{password}")
    User login(User user);

    void deletebyIds(List<Integer> ids);

    //@Options(useGeneratedKeys = true, keyProperty = "id") //获取到生成的主键id -- 主键返回
    void insertinfo(User user);

    User getById(Integer id);

    List<User> searchUsers(int pagesize, int offset, String keyword);

    void update(User user);


    int changePassword(Integer userId, String newPassword);


    // 统计总用户数
    @Select("SELECT COUNT(*) FROM user")
    Long countTotalUsers();

    // 统计今日新增用户数
    @Select("SELECT COUNT(*) FROM user WHERE DATE(updatetime) = CURDATE()")
    Integer countTodayAdded();

    // 方案1: 使用最近创建的用户作为活跃用户（例如最近30天内创建）
    //@Select("SELECT COUNT(*) FROM users WHERE updatetime >= DATE_SUB(NOW(), INTERVAL 30 DAY) AND status = 1")
    //Integer countActiveUsers();

    // 方案2: 如果有更新时间的字段
     @Select("SELECT COUNT(*) FROM user WHERE updatetime >= DATE_SUB(NOW(), INTERVAL 30 DAY)")
     Integer countActiveUsers();

    // 方案3: 如果没有时间字段，直接返回总用户数或固定比例
    // @Select("SELECT COUNT(*) FROM users WHERE status = 1")
    // Integer countActiveUsers();

    // 计算平均年龄
    @Select("SELECT AVG(age) FROM user WHERE age IS NOT NULL")
    Double getAverageAge();

    //查询username是否已存在
    User selectByusername(String username);

    // 批量插入用户
    void batchInsert(List<User> users);
}
