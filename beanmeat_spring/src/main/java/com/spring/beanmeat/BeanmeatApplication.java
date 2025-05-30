package com.spring.beanmeat;

import com.spring.beanmeat.config.BeanmeatConfig;
import com.spring.beanmeat.entity.Beanmeat;
import com.spring.beanmeat.extend.BeanmeatLifeCycle;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class BeanmeatApplication {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(BeanmeatConfig.class);
        BeanmeatLifeCycle beanmeatLifeCycle = applicationContext.getBean(BeanmeatLifeCycle.class);
        // 关闭上下文，触发销毁操作
        applicationContext.close();
    }
}
