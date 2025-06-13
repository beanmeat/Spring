package com.beanmeat.boot.conotroller;

import com.beanmeat.boot.component.BeanmeatEvent;
import com.beanmeat.boot.listener.BeanmeatEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("publish")
public class EventPublishController {

    @Autowired
    private ApplicationEventPublisher publisher;

    @GetMapping("/event")
    public String beanmeat(String message) {
        BeanmeatEvent beanmeatEvent = new BeanmeatEvent(this, message);
        publisher.publishEvent(beanmeatEvent);
        return "event has published";
    }
}
