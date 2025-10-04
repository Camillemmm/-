package com.example.exception;

import com.example.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    //捕获所有Exception异常
    @ExceptionHandler
    public Result handleException(Exception e){
        log.error("程序出错啦~", e);
        return Result.error("出错啦, 请联系管理员~");
    }
    @ExceptionHandler
    public Result handleException(RuntimeException e){
        log.error("程序出错啦~", e);
        return Result.error(e.getMessage());
    }
    /**
     * 声明异常处理的方法 - BusinessException
     */
    @ExceptionHandler(BusinessException.class)
    public Result handleBuinessException(BusinessException businessException) {
        log.error("save保存用户信息出错", businessException);
        return Result.error(businessException.getMessage());
    }
    @ExceptionHandler(MyBatisSystemException.class)
    public Result handleMyBatisSystemException(MyBatisSystemException e){
        log.error("数据库异常", e);
        return Result.error("数据库异常！请稍后重试");
    }
}
