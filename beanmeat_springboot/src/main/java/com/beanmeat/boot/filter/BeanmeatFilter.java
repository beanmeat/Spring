package com.beanmeat.boot.filter;

import jakarta.servlet.*;

import java.io.IOException;


public class BeanmeatFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("BeanmeatFilter init...");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        System.out.println("BeanmeatFilter doFilter request...");
        filterChain.doFilter(servletRequest, servletResponse);
        System.out.println("BeanmeatFilter doFilter response...");
    }

    @Override
    public void destroy() {
        System.out.println("BeanmeatFilter destroy...");
    }
}
