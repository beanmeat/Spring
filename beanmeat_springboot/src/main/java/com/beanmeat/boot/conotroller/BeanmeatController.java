package com.beanmeat.boot.conotroller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("beanmeat")
public class BeanmeatController {

    @GetMapping("/index")
    public String beanmeat() {
        return "hello beanmeat";
    }
}
