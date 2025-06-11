package com.beanmeat.boot.conotroller;

import com.beanmeat.boot.component.AsyncComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AsyncController {

    @Autowired
    private AsyncComponent asyncComponent;

    @GetMapping("/async")
    public String index() {
        System.out.println(Thread.currentThread().getName());
        asyncComponent.commonMethod();
        return "async finished";
    }
}

