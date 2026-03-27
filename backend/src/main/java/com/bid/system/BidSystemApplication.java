package com.bid.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.bid.system.mapper")
public class BidSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(BidSystemApplication.class, args);
    }
}