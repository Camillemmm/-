package com.example.service;

import com.example.pojo.LoginInfo;
import com.example.pojo.StatsDTO;
import com.example.pojo.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface UserService {
    /**
     * 查询所有用户信息
     * @return
     */
    public List<User> findAll();

    void deleteByIds(List<Integer> id);

    LoginInfo login(User user);


    void save(User user);

    User getById(Integer id);

    List<User> searchUsers(int page, int pagesize,String keyword);

    void update(User user);

    void changePassword(Integer id, String currentPassword, String newPassword);

    StatsDTO getUserStats();

    /**
     * 导出用户数据
     * @param format 导出格式 (excel, csv, json)
     * @param scope 导出范围 (all, current)
     * @return 文件字节数组
     */
    byte[] exportUsers(String format, String scope);

    /**
     * 获取导出文件名
     * @param format 导出格式
     * @return 文件名
     */
    String getExportFilename(String format);

    /**
     * 导入用户数据
     * @param file 上传的文件
     * @param skipDuplicates 是否跳过重复用户
     * @param updateExisting 是否更新已存在用户
     * @return 导入结果统计
     */
    Map<String, Object> importUsers(MultipartFile file, boolean skipDuplicates, boolean updateExisting);

    /**
     * 预览导入文件内容
     * @param file 上传的文件
     * @return 预览数据
     */
    Map<String, Object> previewImportFile(MultipartFile file);
}
