package com.example.pojo;

import lombok.Data;

@Data
public class ChangePassword {
    private Integer userId;
    private String currentPassword;
    private String newPassword;
}
