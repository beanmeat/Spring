package com.beanmeat.boot.config;

import com.beanmeat.boot.filter.BeanmeatFilter;
import com.beanmeat.boot.interceptor.BeanmeatInterceptor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class BeanmeatConfig implements WebMvcConfigurer {


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 添加拦截器，并指定执行顺序，也可以通过将拦截器声明成 bean 对象，然后通过 @Order 注解或者实现 Order 接口指定执行顺序
        registry.addInterceptor(new BeanmeatInterceptor()).addPathPatterns("/beanmeat/*").order(1);
    }


    @Bean// 这样配置可以指定过滤器的执行顺序
    public FilterRegistrationBean<BeanmeatFilter> beanmeatFilter() {
        FilterRegistrationBean<BeanmeatFilter> filter = new FilterRegistrationBean<>();
        filter.setFilter(new BeanmeatFilter());
        filter.addUrlPatterns("/beanmeat/*");
        filter.setOrder(1);
        return filter;
    }
}
