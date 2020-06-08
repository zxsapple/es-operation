package com.yundasys.es.operation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @author zhengxiaosu
 * @desc
 * @date 2020/6/2 16:41
 */
//@EnableApolloConfig
@EnableDiscoveryClient
@EnableFeignClients
@SpringBootApplication(scanBasePackages = {"com.yundasys"})
@Slf4j
public class EsOperationApplication {


    public static void main(String[] args) {
        SpringApplication.run(EsOperationApplication.class,args);
    }

}