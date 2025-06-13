package com.beanmeat.boot.component;

import org.springframework.context.ApplicationEvent;

public class BeanmeatEvent extends ApplicationEvent {
    private String message;

    public BeanmeatEvent(Object source,String message) {
        super(source);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
