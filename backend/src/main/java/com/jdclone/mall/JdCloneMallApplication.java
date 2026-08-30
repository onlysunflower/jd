package com.jdclone.mall;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.jdclone.mall.mapper")
@SpringBootApplication
@EnableScheduling
public class JdCloneMallApplication {
    public static void main(String[] args) {
        SpringApplication.run(JdCloneMallApplication.class, args);
    }
}
