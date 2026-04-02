package com.bid.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bid.system.entity.User;
import com.bid.system.mapper.UserMapper;
import com.bid.system.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {

    @Autowired
    private UserMapper userMapper;

    public User register(String username, String password) {
        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
        q.eq(User::getUsername, username);
        if (userMapper.selectCount(q) > 0) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(md5(password));
        user.setRole("user");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    public String login(String username, String password) {
        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
        q.eq(User::getUsername, username).eq(User::getPassword, md5(password));
        User user = userMapper.selectOne(q);
        if (user == null) return null;
        return JwtUtil.generateToken(user.getUsername(), user.getRole());
    }

    public List<User> list() {
        return userMapper.selectList(null);
    }

    public void updateRole(Long id, String role) {
        User user = userMapper.selectById(id);
        if (user == null) throw new RuntimeException("用户不存在");
        user.setRole(role);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    public void updatePassword(Long id, String password) {
        User user = userMapper.selectById(id);
        if (user == null) throw new RuntimeException("用户不存在");
        user.setPassword(md5(password));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    public User findByUsername(String username) {
        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
        q.eq(User::getUsername, username);
        return userMapper.selectOne(q);
    }

    private String md5(String input) {
        return DigestUtils.md5DigestAsHex(input.getBytes());
    }
}
