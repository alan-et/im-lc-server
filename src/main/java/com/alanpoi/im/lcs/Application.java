package com.alanpoi.im.lcs;

import org.apache.dubbo.config.spring.context.annotation.DubboComponentScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@DubboComponentScan(basePackages = "com.alanpoi.im.lcs")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
