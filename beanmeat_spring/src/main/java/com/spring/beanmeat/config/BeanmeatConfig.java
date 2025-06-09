package com.spring.beanmeat.config;

import com.spring.beanmeat.entity.Beanmeat;
import com.spring.beanmeat.extend.BeanmeatLifeCycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("com.spring.beanmeat")
public class BeanmeatConfig {

    @Bean
    public Beanmeat beanmeat() {
        return new Beanmeat("1","beanmeat",19);
    }

    @Bean(initMethod = "initMethod", destroyMethod = "destroyMethod")
    public BeanmeatLifeCycle beanmeatInitBean() {
        return new BeanmeatLifeCycle();
    }

}
