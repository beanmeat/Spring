package com.beanmeat.boot.conotroller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
public class InternationalizationController {

    @Autowired
    private MessageSource messageSource;

    /**
     * http://localhost:8080/zh/greet
     * http://localhost:8080/en/greet
     * @param lang
     * @return
     */
    @GetMapping("/{lang}/greet")
    public String greet(@PathVariable("lang") String lang) {
        Locale locale = new Locale(lang);
        return messageSource.getMessage("greeting", null, locale);
    }

}
