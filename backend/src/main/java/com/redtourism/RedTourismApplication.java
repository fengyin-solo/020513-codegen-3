package com.redtourism;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.redtourism.mapper")
@EnableScheduling
public class RedTourismApplication {
    public static void main(String[] args) {
        SpringApplication.run(RedTourismApplication.class, args);
    }
}
