package com.beanmeat.boot.component;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class AsyncComponent {

    @Resource
    @Lazy
    private AsyncComponent asyncComponent;

    public void commonMethod() {
        System.out.println(Thread.currentThread().getName());
        asyncComponent.asyncMethod();
        System.out.println("commonAsync");
    }

    @Async
    public void asyncMethod() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(Thread.currentThread().getName());
        System.out.println("asyncMethod");
    }
}
