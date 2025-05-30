package com.spring.beanmeat.extend;

import com.spring.beanmeat.entity.Beanmeat;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class BeanmeatPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if(bean instanceof Beanmeat){
            // 如果当前Bean的类型是Beanmeat，就把这个对象name的属性赋值为postProcessorName
            ((Beanmeat) bean).setName("postProcessorName");
        }
        return bean;
    }
}
