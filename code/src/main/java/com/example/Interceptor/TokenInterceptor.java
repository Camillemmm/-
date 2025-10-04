package com.example.Interceptor;

import com.example.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
@Slf4j
@Component
public class TokenInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1. 获取请求url。
        String url = request.getRequestURL().toString();
        log.info("TokenInterceptor请求URL: {}", url);
        // 2.只对 /users 开头的API进行认证，其他全部放行
        if (!url.startsWith("/users")) {
            log.info("放行非/users请求: {}", url);
            return true;
        }
        //3. 获取请求头中的令牌（token）。
        log.info("验证/users请求: {}", url);
        String jwt = request.getHeader("token");
        System.out.println(jwt);
        //4. 判断令牌是否存在，如果不存在，返回错误结果（未登录）。
        if(!StringUtils.hasLength(jwt)){ //jwt为空
            log.info("empty jwt");
            response.setStatus(HttpStatus.SC_UNAUTHORIZED);
            return false;
        }
        //5. 解析token，如果解析失败，返回错误结果（未登录）。
        try {
            JwtUtils.parseJWT(jwt);
            log.info("token验证成功");
        } catch (Exception e) {
            log.info("wrong jwt");
            response.setStatus(HttpStatus.SC_UNAUTHORIZED);
            return false;
        }
        //6. 放行。
        log.info("令牌合法, 放行");
        return true;
    }
}