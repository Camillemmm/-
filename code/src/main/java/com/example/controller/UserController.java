package com.example.controller;

import com.example.anno.LogOperation;
import com.example.pojo.ChangePassword;
import com.example.pojo.Result;
import com.example.pojo.StatsDTO;
import com.example.pojo.User;
import com.example.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.parser.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.awt.print.Pageable;
import java.io.FileNotFoundException;
import java.util.List;

/**
 * 用户信息控制器controller
 */
@Slf4j
@RequestMapping("/users")
@RestController
// 表示该类为控制器，处理请求和响应  @RestController = @Controller + @ResponseBody（将controller方法返回值作为HTTP响应体返回客户端，如果是对象或集合会自动返回json）
public class UserController {
    /*@RequestMapping("/list")
    public List<User> list() throws FileNotFoundException {
        //1.加载读取user.txt文件 获取用户数据
        //绝对路径不推荐，变换路径时，需要修改代码
        //InputStream in =new FileInputStream(new File("D:\\Owninfo\\HM_JavaWeb+AI+SpingBoot\\web-ai-project\\spring-web-01\\src\\main\\resources\\user.txt"));
        InputStream in =this.getClass().getClassLoader().getResourceAsStream("user.txt");
        ArrayList<String> lines = IoUtil.readLines(in, StandardCharsets.UTF_8,new ArrayList<>());

        //2.解析用户信息，封装为user对象 -> list集合
        List<User> userList = lines.stream().map(line->{
            String[] paths= line.split(",");
            Integer id = Integer.parseInt(paths[0]);
            String username = paths[1];
            String password = paths[2];
            String name = paths[3];
            Integer age = Integer.parseInt(paths[4]);
            LocalDateTime updateTime = LocalDateTime.parse(paths[5], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return (User)new User(id,username,password,name,age,updateTime);
        }).collect(Collectors.toList());//toList()jdk16以后能用

        //3.返回用户信息(json)
        return userList;//会自动将list对象转换为json格式
    }*/

    //当存在多个相同类型的Bean注入时
    //@Qualifier("userServiceImpl2")// 当有多个实现类，且需要注入时，使用@Qualifier指定注入的bean的id
    //@Resource(name = "userServiceImpl2") // 当有多个实现类，且需要注入时，使用@Resource指定注入的bean的id

    //private Userservice userservice = new UserServiceImpl();//还存在耦合问题->解耦 使用注解@Controller

    //方式一：构造器注入 - 代码冗余，但显式依赖关系，提高了代码的安全性。代码繁琐、如果构造参数过多，可能会导致构造函数臃肿
    /*private final Userservice userservice;//构造器注入，final表示该成员变量不允许被修改
    @Autowired  //若构造器参数个数大于1，则必须使用@Autowired注解，否则报错，若只有1个参数，则可以省略@Autowired注解
    public UserController(Userservice userservice) {
        this.userservice = userservice;
    }*/

    //方法二：setter注入 -保持了类的封装性，依赖关系更清晰。需要额外编写setter方法，增加了代码量。
    /*private Userservice userservice;
    @Autowired
    public void setUserservice(Userservice userservice) {
        this.userservice = userservice;
    }*/

    //方式三：属性注入 -代码简洁 但隐藏了类之间的依赖关系、可能会破坏类的封装性
    @Autowired //注入UserServiceImpl对象 应用程序运行时，会自动查询该类型bean对象，并赋值给UserDao成员变量
    private UserService userservice;

    /**
     * 查询用户列表
     */
    @GetMapping("/list")
    public Result list() {
        log.info("查询全部用户数据");
        //1.调用service层，获取用户数据
        List<User> userList = userservice.findAll();

        //2.返回信息
        return Result.success(userList);
    }

    /**
     * 批量删除用户信息
     * @param ids
     * @return
     */
    @DeleteMapping
    public Result delete(@RequestParam List<Integer> ids){
        log.info("根据id删除用户：{}",ids);
        userservice.deleteByIds(ids);
        return Result.success();
    }

    /**
     * 新增用户
     * @param user
     * @return
     */
    @PostMapping
    public Result save(@RequestBody User user){
        log.info("新增用户：{}",user);
        //try{
            userservice.save(user);
            return Result.success();
        //}catch(RuntimeException e){
           // log.error(e.getMessage());
           // return Result.error(e.getMessage());
       // }
    }

    /**
     * 根据id查询用户信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id){
        log.info("根据id查询用户：{}",id);
        User user=userservice.getById(id);
        return Result.success(user);
    }

    /**
     * 条件查询用户信息（姓名/用户名）
     * @param page
     * @param pagesize
     * @param keyword
     * @return
     */
    @GetMapping
    @LogOperation
    public Result listUsers(@RequestParam int page,
                            @RequestParam int pagesize,
                            @RequestParam(required = false) String keyword) {
        log.info("条件查询：{}",keyword);
        List<User> users = userservice.searchUsers(page,pagesize,keyword);
        return Result.success(users);
    }

    /**
     * 更新用户信息
     * @param user
     * @return
     */
    @PutMapping
    public Result updateUser(@RequestBody User user){
        log.info("修改用户信息：{}",user);
        userservice.update(user);
        return Result.success();
    }

    /**
     * 修改密码
     * @param request
     * @return
     */
    @PutMapping("/change-password")
    public Result updaePassword(@RequestBody ChangePassword request){
        //try {
            log.info("修改密码：{}",request);
            userservice.changePassword(request.getUserId(), request.getCurrentPassword(), request.getNewPassword());
            return Result.success("密码修改成功");
        //} catch (RuntimeException e) {
           // return Result.error(e.getMessage());
        //}
    }

    @GetMapping("/stats")
    public Result getStats() {
        try {
            StatsDTO stats = userservice.getUserStats();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取统计信息失败", e);
            return Result.error("获取统计信息失败");
        }
    }

    /**
     * 导出用户数据
     * @param format 导出格式 (excel, csv, json)
     * @param scope 导出范围 (all, current)
     * @return
     */
    @GetMapping("/export")
    public ResponseEntity<Resource> exportUsers(@RequestParam String format,
                                                @RequestParam String scope) {
        log.info("导出用户数据 - 格式: {}, 范围: {}", format, scope);
        
        try {
            // 调用Service层获取导出数据
            byte[] fileData = userservice.exportUsers(format, scope);
            String filename = userservice.getExportFilename(format);
            
            // 创建文件资源
            ByteArrayResource resource = new ByteArrayResource(fileData);
            
            // 设置响应头
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
                    .filename(filename, java.nio.charset.StandardCharsets.UTF_8)
                    .build());
            headers.setContentLength(fileData.length);
            
            log.info("导出成功 - 文件名: {}, 文件大小: {} bytes", filename, fileData.length);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("导出用户数据失败", e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ByteArrayResource(("导出失败: " + e.getMessage()).getBytes()));
        }
    }

    /**
     * 导入用户数据
     * @param file 上传的文件
     * @param skipDuplicates 是否跳过重复用户
     * @param updateExisting 是否更新已存在用户
     * @return
     */
    @PostMapping("/import")
    public Result importUsers(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                              @RequestParam(defaultValue = "false") boolean skipDuplicates,
                              @RequestParam(defaultValue = "true") boolean updateExisting) {
        log.info("导入用户数据 - 文件名: {}, 跳过重复: {}, 更新已存在: {}", 
                file.getOriginalFilename(), skipDuplicates, updateExisting);
        
        try {
            // 验证文件
            if (file.isEmpty()) {
                return Result.error("请选择要导入的文件");
            }
            
            // 验证文件格式
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls") 
                    && !filename.endsWith(".csv") && !filename.endsWith(".json"))) {
                return Result.error("不支持的文件格式，请选择Excel、CSV或JSON文件");
            }
            
            // 调用Service层处理导入
            var result = userservice.importUsers(file, skipDuplicates, updateExisting);
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("导入用户数据失败", e);
            return Result.error("导入失败: " + e.getMessage());
        }
    }

    /**
     * 预览导入文件
     * @param file 上传的文件
     * @return 预览数据
     */
    @PostMapping("/import/preview")
    public Result previewImportFile(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        log.info("预览导入文件 - 文件名: {}", file.getOriginalFilename());
        
        try {
            // 验证文件
            if (file.isEmpty()) {
                return Result.error("请选择要预览的文件");
            }
            
            // 验证文件格式
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls") && !filename.endsWith(".csv") && !filename.endsWith(".json"))){
                return Result.error("不支持的文件格式，请选择Excel、CSV或JSON文件");
            }
            
            // 调用Service层处理预览
            var result = userservice.previewImportFile(file);
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("预览文件失败", e);
            return Result.error("预览失败: " + e.getMessage());
        }
    }
}
