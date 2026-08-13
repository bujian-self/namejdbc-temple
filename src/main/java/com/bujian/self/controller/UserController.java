package com.bujian.self.controller;

import org.springframework.web.bind.annotation.*;

import com.bujian.self.dto.User;
import com.bujian.self.dto.UserQueryRequest;
import com.bujian.self.service.UserService;

import java.util.List;

import com.feiniaojin.gracefulresponse.data.ResponseData;
import com.feiniaojin.gracefulresponse.advice.GracefulResponse;

@RestController
@RequestMapping("/api/users")
@GracefulResponse
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 安全查询用户接口
     * 
     * @param request 查询请求
     * @return 查询响应
     */
    @PostMapping("/search")
    public ResponseData<List<User>> searchUsers(@RequestBody UserQueryRequest request) {
        return ResponseData.ok(userService.safeSearch(request));
    }

    /**
     * 简化版查询接口（使用 GET 请求）
     */
    @GetMapping("/search")
    public ResponseData<List<User>> searchUsersGet(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge) {
        
        UserQueryRequest request = new UserQueryRequest();
        request.setName(name);
        request.setMinAge(minAge);
        request.setMaxAge(maxAge);
        
        return ResponseData.ok(userService.safeSearch(request));
    }
}
