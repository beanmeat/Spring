package com.spring.beanmeat.spel;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;

public class Evaluation {
    public static void main(String[] args) {
        SpelExpressionParser spelExpressionParser = new SpelExpressionParser();
        Expression expression = spelExpressionParser.parseExpression("'hello world'");
        String message = expression.getValue(String.class);
        System.out.println(message);
    }
}
