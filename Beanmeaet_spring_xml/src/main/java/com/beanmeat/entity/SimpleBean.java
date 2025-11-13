package com.beanmeat.entity;

/**
 * 最简单的Bean类，用于Spring XML配置示例
 */
public class SimpleBean {
    
    private String name = "SimpleBean";
    
    public SimpleBean() {
        System.out.println("SimpleBean 构造函数被调用");
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void sayHello() {
        System.out.println("Hello from " + name);
    }
}

