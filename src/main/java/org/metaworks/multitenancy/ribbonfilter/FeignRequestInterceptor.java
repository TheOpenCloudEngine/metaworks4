package org.metaworks.multitenancy.ribbonfilter;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.jmnarloch.spring.cloud.ribbon.support.RibbonFilterContextHolder;
import org.metaworks.common.ApplicationContextRegistry;
import org.metaworks.multitenancy.tenantawarefilter.TokenContext;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.uengine.util.StringUtils;

import java.util.Arrays;
import java.util.List;

public class FeignRequestInterceptor implements RequestInterceptor {

    public FeignRequestInterceptor() {

    }

    private void updateRibbonFilterContextHolder() {
        ApplicationContext applicationContext = ApplicationContextRegistry.getApplicationContext();
        Environment environment = applicationContext.getBean(Environment.class);
        String[] activeProfiles = environment.getActiveProfiles();

        //필터 초기화
        RibbonFilterContextHolder.getCurrentContext().remove("profile");

        String[] list = new String[]{"local", "dev", "stg", "prod"};
        List<String> filterProfiles = Arrays.asList(list);

        //자신의 액티브 프로파일을 필터에 추가
        for (String activeProfile : activeProfiles) {
            if (filterProfiles.contains(activeProfile)) {
                RibbonFilterContextHolder.getCurrentContext()
                        .add("profile", activeProfile);
            }
        }
    }

    public void apply(RequestTemplate template) {
        //필터 업데이트
        this.updateRibbonFilterContextHolder();

        //오리지널 요청에 토큰이 있다면 토큰 전달
        String token = TokenContext.getThreadLocalInstance().getToken();
        if (!StringUtils.isEmpty(token)) {
            template.header("access_token", token);
        }
    }
}
