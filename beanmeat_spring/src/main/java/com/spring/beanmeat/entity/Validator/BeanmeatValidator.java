package com.spring.beanmeat.entity.Validator;

import com.spring.beanmeat.entity.Beanmeat;
import org.springframework.validation.*;

public class BeanmeatValidator implements Validator {
   public static void main(String[] args) {
        Beanmeat person= new Beanmeat();
        person.setName("");
        person.setAge(111);

        BeanmeatValidator validator = new BeanmeatValidator();

        DataBinder dataBinder = new DataBinder(person);
        dataBinder.setValidator(validator);
        dataBinder.validate();

        BindingResult result = dataBinder.getBindingResult();
        System.out.println("results:"+ result);
    }


    @Override
    public boolean supports(Class<?> clazz) {
        return Beanmeat.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        ValidationUtils.rejectIfEmpty(errors,"name","name.empty");
        Beanmeat p = (Beanmeat)target;
        if(p.getAge() < 0){
            errors.rejectValue("age","negativevalue");
        } else if(p.getAge() > 110){
            errors.rejectValue("age","too.darn.old");
        }
    }
}
