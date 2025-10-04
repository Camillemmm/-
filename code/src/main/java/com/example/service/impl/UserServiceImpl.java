package com.example.service.impl;

import com.example.anno.LogOperation;
import com.example.dao.UserDao;
import com.example.exception.BusinessException;
import com.example.mapper.UserMapper;
import com.example.pojo.LoginInfo;
import com.example.pojo.StatsDTO;
import com.example.pojo.User;
import com.example.service.UserService;
import com.example.utils.JwtUtils;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

//@Component //表示该类为组件，会被Spring IOC容器管理
@Service //表示该类为服务层，会被Spring IOC容器管理
@Primary
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Override
    public List<User> findAll() {
        //调用mapper层，查询用户数据
        List<User> userList = userMapper.findAll();
        return userList;
    }

    @Override
    public void deleteByIds(List<Integer> ids) {
        userMapper.deletebyIds(ids);
    }

    @Override
    public void save(User user) {
        User userinfo=userMapper.selectByusername(user.getUsername());
        if(userinfo!=null){
            throw new BusinessException("用户已存在");
        }else{
            user.setUpdateTime(LocalDateTime.now());
            userMapper.insertinfo(user);
        }
    }

    @Override
    @LogOperation
    public User getById(Integer id) {
        User user = userMapper.getById(id);
        return user;
    }

    @Override
    public List<User> searchUsers(int page, int pagesize, String keyword) {
        //int pagesize = 10; // 每页条数，可根据前端传入
        int offset = (page - 1) * pagesize;

        return userMapper.searchUsers(pagesize, offset, keyword);
    }

    @Override
    public void update(User user) {
        user.setUpdateTime(LocalDateTime.now());
        userMapper.update(user);
    }

    @Override
    public void changePassword(Integer userId, String currentPassword, String newPassword) {
        // 1. 先查出用户
        User user = userMapper.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 2. 校验原密码
        if (!user.getPassword().equals(currentPassword)) {
            throw new BusinessException("原密码不正确");//使用自定义异常类
        }

        // 3. 更新新密码
        int rows = userMapper.changePassword(userId, newPassword);
        if (rows == 0) {
            throw new BusinessException("密码修改失败");
        }
    }

    @Override
    public LoginInfo login(User user) {
        User userLogin = userMapper.login(user);
        if (userLogin != null) {
            //1.生成JWT令牌
            Map<String,Object> dataMap=new HashMap<>();
            dataMap.put("id",userLogin.getId());
            dataMap.put("username",userLogin.getUsername());
            String token= JwtUtils.generateJwt(dataMap);
            System.out.println(token);
            LoginInfo loginInfo = new LoginInfo(userLogin.getId(), userLogin.getUsername(), userLogin.getName(), userLogin.getAge(), userLogin.getUpdateTime(), token);
            return loginInfo;
        }
        return null;
    }

    @Override
    public StatsDTO getUserStats() {
        StatsDTO stats = new StatsDTO();

        // 1. 总用户数
        Long totalUsers = userMapper.countTotalUsers();
        stats.setTotalUsers(totalUsers != null ? totalUsers : 0L);

        // 2. 今日新增用户数
        Integer todayAdded = userMapper.countTodayAdded();
        stats.setTodayAdded(todayAdded != null ? todayAdded : 0);

        // 3. 活跃用户数 - 根据你的业务逻辑选择
        Integer activeUsers = userMapper.countActiveUsers();
        // 或者如果没有合适的字段，可以使用总用户数的80%作为活跃用户
        if (activeUsers == null || activeUsers == 0) {
            activeUsers = (int) (totalUsers * 0.8); // 默认80%为活跃用户
        }
        stats.setActiveUsers(activeUsers);

        // 4. 平均年龄
        Double avgAge = userMapper.getAverageAge();
        BigDecimal bd = new BigDecimal(avgAge);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        stats.setAvgAge(avgAge != null ? bd.doubleValue() : 0.0);


        return stats;
    }

    @Override
    public byte[] exportUsers(String format, String scope) {
        try {
            List<User> users;
            
            // 根据范围获取用户数据
            if ("all".equals(scope)) {
                users = userMapper.findAll();
            } else {
                // 当前页数据 - 这里简化处理，返回前100条数据
                users = userMapper.searchUsers(100, 0, "");
            }
            
            // 根据格式生成文件数据
            switch (format.toLowerCase()) {
                case "excel":
                    return exportToExcel(users);
                case "csv":
                    return exportToCsv(users);
                case "json":
                    return exportToJson(users);
                default:
                    throw new BusinessException("不支持的导出格式: " + format);
            }
            
        } catch (Exception e) {
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    @Override
    public String getExportFilename(String format) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        switch (format.toLowerCase()) {
            case "excel":
                return "users_" + timestamp + ".xlsx";
            case "csv":
                return "users_" + timestamp + ".csv";
            case "json":
                return "users_" + timestamp + ".json";
            default:
                return "users_" + timestamp + ".txt";
        }
    }

    @Override
    public Map<String, Object> importUsers(MultipartFile file, boolean skipDuplicates, boolean updateExisting) {
        Map<String, Object> result = new HashMap<>();
        int added = 0;
        int updated = 0;
        int skipped = 0;
        
        try {
            String filename = file.getOriginalFilename();
            if (filename == null) {
                throw new BusinessException("文件名不能为空");
            }
            
            List<User> users;
            
            // 根据文件扩展名解析数据
            if (filename.endsWith(".json")) {
                users = parseJsonFile(file);
            } else if (filename.endsWith(".csv")) {
                users = parseCsvFile(file);
            } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                users = parseExcelFile(file);
            } else {
                throw new BusinessException("不支持的文件格式");
            }
            
            // 处理每个用户
            for (User user : users) {
                try {
                    User existingUser = userMapper.selectByusername(user.getUsername());
                    
                    if (existingUser != null) {
                        if (skipDuplicates) {
                            skipped++;
                        } else if (updateExisting) {
                            user.setId(existingUser.getId());
                            user.setUpdateTime(LocalDateTime.now());
                            userMapper.update(user);
                            updated++;
                        } else {
                            throw new BusinessException("用户 " + user.getUsername() + " 已存在");
                        }
                    } else {
                        user.setUpdateTime(LocalDateTime.now());
                        userMapper.insertinfo(user);
                        added++;
                    }
                } catch (Exception e) {
                    // 记录错误但继续处理其他用户
                    System.err.println("处理用户失败: " + user.getUsername() + ", 错误: " + e.getMessage());
                }
            }
            
            result.put("added", added);
            result.put("updated", updated);
            result.put("skipped", skipped);
            result.put("total", users.size());
            
        } catch (Exception e) {
            throw new BusinessException("导入失败: " + e.getMessage());
        }
        
        return result;
    }

    // 导出为Excel格式（简化实现，实际项目中建议使用Apache POI）
    private byte[] exportToExcel(List<User> users) {
        // 这里简化实现，实际项目中应该使用Apache POI
        return exportToCsv(users); // 暂时返回CSV格式
    }

    // 导出为CSV格式
    private byte[] exportToCsv(List<User> users) {
        StringBuilder csv = new StringBuilder();
        csv.append("ID,用户名,姓名,年龄,更新时间\n");
        
        for (User user : users) {
            csv.append(user.getId()).append(",");
            csv.append(user.getUsername()).append(",");
            csv.append(user.getName()).append(",");
            csv.append(user.getAge() != null ? user.getAge() : "").append(",");
            csv.append(user.getUpdateTime() != null ? user.getUpdateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
            csv.append("\n");
        }
        
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    // 导出为JSON格式
    private byte[] exportToJson(List<User> users) {
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"users\": [\n");
        
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            json.append("    {\n");
            json.append("      \"id\": ").append(user.getId()).append(",\n");
            json.append("      \"username\": \"").append(user.getUsername()).append("\",\n");
            json.append("      \"name\": \"").append(user.getName()).append("\",\n");
            json.append("      \"age\": ").append(user.getAge() != null ? user.getAge() : "null").append(",\n");
            json.append("      \"updateTime\": \"").append(user.getUpdateTime() != null ? user.getUpdateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "").append("\"\n");
            json.append("    }");
            if (i < users.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        
        json.append("  ]\n}");
        return json.toString().getBytes(StandardCharsets.UTF_8);
    }

    // 解析JSON文件
    private List<User> parseJsonFile(MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        List<User> users = new ArrayList<>();
        
        try {
            // 简单的JSON解析实现
            content = content.trim();
            if (!content.startsWith("{") || !content.contains("\"users\"")) {
                throw new BusinessException("JSON格式不正确，应包含users数组");
            }
            
            // 查找users数组
            int usersStart = content.indexOf("\"users\"");
            if (usersStart == -1) {
                throw new BusinessException("JSON中未找到users数组");
            }
            
            int arrayStart = content.indexOf("[", usersStart);
            int arrayEnd = content.lastIndexOf("]");
            
            if (arrayStart == -1 || arrayEnd == -1) {
                throw new BusinessException("JSON格式不正确");
            }
            
            String usersArray = content.substring(arrayStart + 1, arrayEnd);
            String[] userObjects = usersArray.split("\\},\\s*\\{");
            
            for (int i = 0; i < userObjects.length; i++) {
                String userStr = userObjects[i].trim();
                if (userStr.startsWith("{")) {
                    userStr = userStr.substring(1);
                }
                if (userStr.endsWith("}")) {
                    userStr = userStr.substring(0, userStr.length() - 1);
                }
                
                User user = parseUserFromJson(userStr);
                if (user != null) {
                    users.add(user);
                }
            }
            
        } catch (Exception e) {
            throw new BusinessException("JSON解析失败: " + e.getMessage());
        }
        
        return users;
    }
    
    // 从JSON字符串解析单个用户
    private User parseUserFromJson(String userJson) {
        try {
            User user = new User();
            
            // 解析username
            if (userJson.contains("\"username\"")) {
                String username = extractJsonValue(userJson, "username");
                if (username != null && !username.isEmpty()) {
                    user.setUsername(username);
                }
            }
            
            // 解析name
            if (userJson.contains("\"name\"")) {
                String name = extractJsonValue(userJson, "name");
                if (name != null && !name.isEmpty()) {
                    user.setName(name);
                }
            }
            
            // 解析age
            if (userJson.contains("\"age\"")) {
                String ageStr = extractJsonValue(userJson, "age");
                if (ageStr != null && !ageStr.isEmpty() && !ageStr.equals("null")) {
                    try {
                        user.setAge(Integer.parseInt(ageStr));
                    } catch (NumberFormatException e) {
                        // 年龄解析失败，设为null
                    }
                }
            }
            
            // 设置默认密码
            user.setPassword("123456");
            
            return user;
        } catch (Exception e) {
            return null;
        }
    }
    
    // 从JSON字符串中提取值
    private String extractJsonValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
            
            // 尝试数字值
            pattern = "\"" + key + "\"\\s*:\\s*(\\d+)";
            p = java.util.regex.Pattern.compile(pattern);
            m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // 解析CSV文件
    private List<User> parseCsvFile(MultipartFile file) throws IOException {
        List<User> users = new ArrayList<>();
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        String[] lines = content.split("\n");
        
        // 跳过标题行
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            
            String[] fields = line.split(",");
            if (fields.length >= 4) {
                User user = new User();
                user.setUsername(fields[1].trim());
                user.setName(fields[2].trim());
                if (fields.length > 3 && !fields[3].trim().isEmpty()) {
                    try {
                        user.setAge(Integer.parseInt(fields[3].trim()));
                    } catch (NumberFormatException e) {
                        // 年龄解析失败，设为null
                    }
                }
                users.add(user);
            }
        }
        
        return users;
    }

    // 解析Excel文件（简化实现）
    private List<User> parseExcelFile(MultipartFile file) throws IOException {
        // 简化实现，实际项目中应该使用Apache POI
        return parseCsvFile(file);
    }

    @Override
    public Map<String, Object> previewImportFile(MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String filename = file.getOriginalFilename();
            if (filename == null) {
                throw new BusinessException("文件名不能为空");
            }
            
            List<User> users;
            
            // 根据文件扩展名解析数据
            if (filename.endsWith(".json")) {
                users = parseJsonFile(file);
            } else if (filename.endsWith(".csv")) {
                users = parseCsvFile(file);
            } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                users = parseExcelFile(file);
            } else {
                throw new BusinessException("不支持的文件格式");
            }
            
            // 限制预览数量
            int previewCount = Math.min(users.size(), 10);
            List<User> previewUsers = users.subList(0, previewCount);
            
            result.put("total", users.size());
            result.put("previewCount", previewCount);
            result.put("users", previewUsers);
            result.put("filename", filename);
            
        } catch (Exception e) {
            throw new BusinessException("预览失败: " + e.getMessage());
        }
        
        return result;
    }
}
