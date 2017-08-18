package org.metaworks.springboot.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;

/**
 * Created by uengine on 2017. 8. 3..
 */
@Configuration
public class GlobalRepositoryRestConfigurer extends RepositoryRestConfigurerAdapter {
    /**
     * Jpa Rest 진입 전의 CORS 필터. OPTIONS (헤더 요청) 을 통화시키는데 주 목적이 있다.
     *
     * @param config
     */
    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
        config.getCorsRegistry()
                .addMapping("/**")
                .allowedOrigins("*")
                .maxAge(3600)
                .allowedMethods("POST", "GET", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("access_token", "Content-Type", "x-requested-with", "origin", "accept",
                        "authorization", "Location");
    }
}
