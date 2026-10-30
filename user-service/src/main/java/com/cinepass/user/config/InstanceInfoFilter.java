package com.cinepass.user.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InstanceInfoFilter implements Filter {

    @Value("${server.port:8081}")
    private String port;

    @Value("${spring.application.name:user-service}")
    private String serviceName;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (response instanceof HttpServletResponse httpResponse) {
            httpResponse.setHeader("X-Service-Port", port);
            httpResponse.setHeader("X-Service-Name", serviceName);
        }
        chain.doFilter(request, response);
    }
}
