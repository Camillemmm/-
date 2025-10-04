package com.example;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@SpringBootTest
class MapperProxyTest {

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Test
    void testMapperProxy() {
        System.out.println("=== Mapper代理诊断 ===");

        Configuration configuration = sqlSessionFactory.getConfiguration();

        // 检查所有已注册的Mapper接口
        System.out.println("已注册的Mapper接口:");
        configuration.getMapperRegistry().getMappers().forEach(mapperType -> {
            System.out.println("  - " + mapperType.getName());
        });

        // 检查所有映射语句
        System.out.println("所有映射语句:");
        configuration.getMappedStatementNames().forEach(statement -> {
            System.out.println("  - " + statement);
        });

        // 特别检查UserMapper相关的语句
        System.out.println("UserMapper相关语句:");
        configuration.getMappedStatementNames().stream()
                .filter(statement -> statement.contains("UserMapper"))
                .forEach(statement -> {
                    System.out.println("  - " + statement);
                    try {
                        MappedStatement ms = configuration.getMappedStatement(statement);
                        System.out.println("    SQL: " + ms.getSqlSource().getBoundSql(null).getSql());
                    } catch (Exception e) {
                        System.out.println("    获取SQL失败: " + e.getMessage());
                    }
                });
    }

    @Test
    void exactFileCheck() throws Exception {
        System.out.println("=== 精确文件检查 ===");

        // 检查确切路径
        String exactPath = "com/example/mapper/UserMapper.xml";
        ClassPathResource resource = new ClassPathResource(exactPath);

        System.out.println("文件是否存在: " + resource.exists());
        if (resource.exists()) {
            System.out.println("文件路径: " + resource.getFile().getAbsolutePath());
            System.out.println("文件大小: " + resource.getFile().length() + " bytes");

            // 读取文件内容检查
            String content = new String(resource.getInputStream().readAllBytes());
            System.out.println("文件内容:");
            System.out.println(content);
        } else {
            System.out.println("文件未找到，搜索所有XML文件:");
            Resource[] allResources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:**/*.xml");
            for (Resource res : allResources) {
                System.out.println("找到: " + res.getURI());
            }
        }
    }
}
