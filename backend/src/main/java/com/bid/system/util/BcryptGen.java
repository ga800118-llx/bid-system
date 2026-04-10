package com.bid.system.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptGen {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        for (String pwd : args) {
            String sep = new String(new char[]{32,61,62,32});
            System.out.println(pwd + sep + encoder.encode(pwd));
        }
    }
}