package com.yundasys.es.operation.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author zhengxiaosu
 * @desc
 * @date 2019/3/15 15:35
 */
@Data
@Slf4j
@Component
@ConfigurationProperties(prefix = "elasticsearch.client")
public class RestHighLevelClientFactory implements FactoryBean<RestHighLevelClient>, InitializingBean, DisposableBean {
    /** client */
    private RestHighLevelClient client;

    /** 配置 start */
    private String address;

    private Integer connectTimeOut;
    private Integer socketTimeOut;
    private Integer connectionRequestTime;

    private Integer maxConnectTotalNum;
    private Integer maxConnectPerRoute;

    /** 配置 end */

    @Override
    public void destroy() throws Exception {
        try {
            client.close();
            log.warn("RestHighLevelClient destroy");
        } catch (Exception e) {
            log.error("RestHighLevelClient#close Had Exception: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public RestHighLevelClient getObject() throws Exception {
        return client;
    }

    @Override
    public Class<?> getObjectType() {
        return RestHighLevelClient.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        RestClientBuilder builder = RestClient.builder(httpHost());
        setConnectTimeOutConfig(builder);
        setMutiConnectConfig(builder);
        client = new RestHighLevelClient(builder);
        log.warn("init RestHighLevelClient 完成");
    }


    private HttpHost[] httpHost() {
        String[] adresses = address.split(",");
        HttpHost[] hosts = new HttpHost[adresses.length];
        for (int i = 0; i < adresses.length; i++) {
            String[] addressInfo = adresses[i].split(":");
            hosts[i] = new HttpHost(addressInfo[0], Integer.parseInt(addressInfo[1]), "http");
        }
        return hosts;
    }

    // 配置连接时间延时
    public void setConnectTimeOutConfig(RestClientBuilder builder) {
        builder.setRequestConfigCallback(requestConfigBuilder -> {
            requestConfigBuilder.setConnectTimeout(connectTimeOut);
            requestConfigBuilder.setSocketTimeout(socketTimeOut);
            requestConfigBuilder.setConnectionRequestTimeout(connectionRequestTime);
            return requestConfigBuilder;
        });
    }

    // 使用异步httpclient时设置并发连接数
    public void setMutiConnectConfig(RestClientBuilder builder) {
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            httpClientBuilder.setMaxConnTotal(maxConnectTotalNum);
            httpClientBuilder.setMaxConnPerRoute(maxConnectPerRoute);
            return httpClientBuilder;
        });
    }
}
