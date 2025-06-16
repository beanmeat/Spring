package com.beanmeat.boot.conotroller;

import com.beanmeat.boot.entity.Beanmeat;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("beanmeat")
public class BeanmeatController {

    @Resource
    private Beanmeat beanmeat;

    @GetMapping("/index")
    public String beanmeat() {
        return "hello beanmeat";
    }

    @GetMapping("/getBeanmeat")
    public String getBeanmeat() {
        return beanmeat.toString();
    }
}
