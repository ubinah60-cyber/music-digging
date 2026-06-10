package com.example.musicdigging.controller;

import com.example.musicdigging.domain.User;
import com.example.musicdigging.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/users")
    public List<User> users() {
        return userService.findAll();
    }
}