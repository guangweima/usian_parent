package com.usian;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients //扫描feign接口
@EnableDiscoveryClient//注册到eureka
public class SearchWebApp {
    public static void main(String[] args) {
        SpringApplication.run(SearchWebApp.class, args);
    }
}