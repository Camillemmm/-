package com.example.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

public class JwtUtils {
    private static final String SECRET_STRING = "ZGFxaWFvMTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3OA==";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_STRING.getBytes());//HMAC-SHA 算法的密钥必须至少 256 位（32字节）
    //private static final SecretKey SECRET_KEY = Jwts.SIG.HS256.key().build();
    /**
     * 生成JWT令牌
     * @return
     */
    public static String generateJwt(Map<String,Object> claims){
        String token = Jwts.builder()
                .signWith(SECRET_KEY)//指定加密算法及密钥，
                .claims(claims)// 设置claims(令牌中携带的声明信息或有效载荷数据)
                .expiration(new Date(System.currentTimeMillis() + 12 * 3600 * 1000))//指定有效期
                .compact();//生成令牌
        return token;
    }
    /**
     * 解析JWT令牌
     * @param token JWT令牌
     * @return JWT第二部分负载 payload 中存储的内容
     */
    public static Claims parseJWT(String token){
        Claims claims = Jwts.parser()
                .verifyWith(SECRET_KEY)            // 使用verifyWith验证
                .build()                           // 构建解析器
                .parseSignedClaims(token)          // 解析签名claims
                .getPayload();                     // 获取payload
        return claims;
    }
}
