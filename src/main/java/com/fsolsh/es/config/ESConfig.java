package com.fsolsh.es.config;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;

/**
 * ES配置与客户端初始化
 */
@Configuration
public class ESConfig extends AbstractElasticsearchConfiguration {

    //ES服务器地址
    @Value("${es.rest.server.endpoint}")
    private String restServerEndpoint;

    /**
     * Rest查询客户端
     *
     * @return
     */
    @Bean
    @Override
    public RestHighLevelClient elasticsearchClient() {
        final ClientConfiguration clientConfiguration = ClientConfiguration.builder().connectedTo(restServerEndpoint).build();
        return RestClients.create(clientConfiguration).rest();
    }
}
