package com.beanmeat.boot.config;

import com.beanmeat.boot.entity.Beanmeat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;

@PropertySource({"classpath:beanmeat/beanmeat.properties","classpath:beanmeat/beanmeat.yml"})
@Configuration
public class PropertySourceConfig {

    @Value("${beanmeat.name}")
    private String name;

    @Value("${beanmeat.alise}")
    private String alise;

    @Bean("propertiesBeanmeat")
    public Beanmeat propertiesBeanmeat() {
        return new Beanmeat(null, name, null);
    }

    @Primary
    @Bean("ymlBeanmeat")
    public Beanmeat ymlBeanmeat() {
        return new Beanmeat(null, alise, null);
    }


    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("beanmeat/beanmeat.yml"));
        configurer.setProperties(yaml.getObject());
        return configurer;
    }
}
