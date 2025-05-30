package com.spring.beanmeat.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Beanmeat {

    private String id;

    private String name;

    private String alise;
}
