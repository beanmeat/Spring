package com.beanmeat.boot.listener;

import com.beanmeat.boot.component.BeanmeatEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class BeanmeatEventListener {

    // @Async
    // @EventListener
    @EventListener(condition = "#event.message.startsWith('Spring')")
    public void handleEvent(BeanmeatEvent event) {
        // 如果返回值是事件类型，Spring会自动将返回值作为新的事件发布
        System.out.println("Handling event: " + event.getMessage());
    }

    @EventListener
    public void onContextRefreshed(ContextRefreshedEvent event) {
        System.out.println("Context refreshed");
    }
}
