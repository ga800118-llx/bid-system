package com.bid.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bid.system.dto.LoginRequest;
import com.bid.system.dto.RegisterRequest;
import com.bid.system.entity.User;
import com.bid.system.mapper.UserMapper;
import com.bid.system.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {

    @Autowired
    private UserMapper userMapper;

    public String login(LoginRequest req) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, req.getUsername());
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        String md5Pwd = DigestUtils.md5DigestAsHex(req.getPassword().getBytes());
        if (!md5Pwd.equals(user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        return JwtUtil.generateToken(user.getUsername(), user.getRole());
    }

    public void register(RegisterRequest req) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, req.getUsername());
        Long count = userMapper.selectCount(wrapper);
        if (count > 0) {
            throw new RuntimeException("该邮箱已注册");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(DigestUtils.md5DigestAsHex(req.getPassword().getBytes()));
        user.setRole("user");
        userMapper.insert(user);
    }

    public User getUserInfo(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }
}