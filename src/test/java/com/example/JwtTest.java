package com.example;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT的组成： （JWT令牌由三个部分组成，三个部分之间使用英文的点来分割）
 * - 第一部分：Header(头）， 记录令牌类型、签名算法等。 例如：{"alg":"HS256","type":"JWT"}
 * - 第二部分：Payload(有效载荷），携带一些自定义信息、默认信息等。 例如：{"id":"1","username":"Tom"}
 * - 第三部分：Signature(签名），防止Token被篡改、确保安全性。将header、payload，并加入指定秘钥，通过指定签名算法计算而来。
 */
public class JwtTest {
    private static final String SECRET_STRING = "ZGFxaWFvMTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3OA==";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_STRING.getBytes());//HMAC-SHA 算法的密钥必须至少 256 位（32字节）

    //private static final SecretKey SECRET_KEY = Jwts.SIG.HS256.key().build();
    @Test
    public void testGenJwt() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", 10);
        claims.put("username", "daqiao");

        //signWith:指定签名算法
        String jwt = Jwts.builder()
                .signWith(SECRET_KEY)//指定加密算法及密钥，
                .claims(claims)// 设置claims(令牌中携带的声明信息或有效载荷数据)
                .expiration(new Date(System.currentTimeMillis() + 12 * 3600 * 1000))//指定有效期
                .compact();//生成令牌

        System.out.println(jwt);
    }

    @Test
    public void testParseJwt() {
        String token="eyJhbGciOiJIUzM4NCJ9.eyJpZCI6MTAsInVzZXJuYW1lIjoiZGFxaWFvIiwiZXhwIjoxNzU5MzYzOTg1fQ.nP5WS9loF2MCjn3AGkpUUN1lwHvXwVkocV7tR8WYp43ZSSfIxiCOVm6iE_UPftYS";
        Claims claims = Jwts.parser()
                .verifyWith(SECRET_KEY)            // 使用verifyWith验证
                .build()                           // 构建解析器
                .parseSignedClaims(token)          // 解析签名claims
                .getPayload();                     // 获取payload
        System.out.println(claims);
    }
}
