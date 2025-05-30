package com.spring.beanmeat.extend;

import com.spring.beanmeat.entity.Beanmeat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.logging.Logger;

public class BeanmeatLifeCycle implements InitializingBean, ApplicationContextAware, DisposableBean {

    Logger logger = Logger.getLogger(BeanmeatLifeCycle.class.getName());

    @Autowired
    private Beanmeat beanmeat;

    public BeanmeatLifeCycle() {
        System.out.println("BeanmeatLifeCycle Constructor");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Beanmeat bean = applicationContext.getBean(Beanmeat.class);
        System.out.println("BeanmeatLifeCycle implements ApplicationContextAware setApplicationContext; Beanmeat:" + bean);
    }

    @PostConstruct
    public void postConstruct() {
        System.out.println("BeanmeatLifeCycle Annotation PostConstruct");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("BeanmeatLifeCycle implements InitializingBean afterPropertiesSet");
    }

    public void initMethod() throws Exception {
        System.out.println("BeanmeatLifeCycle initMethod");
    }

    @PreDestroy
    public void preDestroy() throws Exception {
        System.out.println("BeanmeatLifeCycle preDestroy");
    }

    @Override
    public void destroy() throws Exception {
        System.out.println("BeanmeatLifeCycle implements DisposableBean destroy");
    }

    public void destroyMethod() throws Exception {
        System.out.println("BeanmeatLifeCycle destroyMethod");
    }
}
