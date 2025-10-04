package com.example.filter;

import com.example.utils.CurrentHolder;
import com.example.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.util.StringUtils;
import java.io.IOException;
@Slf4j
@WebFilter(urlPatterns = "/*")
public class TokenFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        //1. 获取请求url。
        String url = request.getRequestURI().toString();
        log.info("TokenFilter请求URL: {}", url);
        //2.只对 /users 开头的API进行认证，其他全部放行
        if (!url.startsWith("/users")) {
            log.info("放行非/users请求: {}", url);
            filterChain.doFilter(request, response);
            return;
        }
        //3.只有 /users 开头的请求需要token验证-获取请求头中的令牌（token）。
        log.info("验证/users请求: {}", url);
        String jwt = request.getHeader("token");
        System.out.println(jwt);
        //4. 判断令牌是否存在，如果不存在，返回错误结果（未登录）。
        if (!StringUtils.hasLength(jwt)) {
            log.info("empty jwt");
            response.setStatus(HttpStatus.SC_UNAUTHORIZED);
            return;
        }
        //5. 解析token，如果解析失败，返回错误结果（未登录）。
        try {
            Claims claims = JwtUtils.parseJWT(jwt);
            Integer userId = Integer.valueOf(claims.get("id").toString());
            CurrentHolder.setCurrentId(userId);
            log.info("token验证成功");
        } catch (Exception e) {
            log.info("wrong jwt");
            response.setStatus(HttpStatus.SC_UNAUTHORIZED);
        }
        //6.放行
        filterChain.doFilter(request, response);
        //7. 清空当前线程绑定的id
        CurrentHolder.remove();
    }
    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
