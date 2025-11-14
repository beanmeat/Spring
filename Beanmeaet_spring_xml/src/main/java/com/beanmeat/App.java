package com.beanmeat;

import com.beanmeat.entity.SimpleBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 最简单的Spring XML配置示例
 */
public class App {
    public static void main(String[] args) {
        // 使用ClassPathXmlApplicationContext加载XML配置
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("beans.xml");
        
        // 从容器中获取Bean
        SimpleBean simpleBean = context.getBean("simpleBean", SimpleBean.class);
        simpleBean.sayHello();
        
        // 关闭上下文
        context.close();
    }
}
