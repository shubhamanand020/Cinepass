package com.cinepass.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    @Order(1)
    public RouterFunction<ServerResponse> userRoute() {
        return route("user-service")
                .route(path("/api/users", "/api/users/**", "/api/auth/**"), http())
                .filter(lb("user-service"))
                .build();
    }

    @Bean
    @Order(2)
    public RouterFunction<ServerResponse> movieRoute() {
        return route("movie-service")
                .route(path("/api/movies", "/api/movies/**"), http())
                .filter(lb("movie-service"))
                .build();
    }

    @Bean
    @Order(3)
    public RouterFunction<ServerResponse> bookingAvailabilityRoute() {
        return route("booking-availability")
                .route(path("/api/shows/*/availability"), http())
                .filter(lb("booking-service"))
                .build();
    }

    @Bean
    @Order(4)
    public RouterFunction<ServerResponse> showRoute() {
        return route("show-service")
                .route(path("/api/screens", "/api/screens/**", "/api/shows", "/api/shows/**"), http())
                .filter(lb("show-service"))
                .build();
    }

    @Bean
    @Order(5)
    public RouterFunction<ServerResponse> bookingRoute() {
        return route("booking-service")
                .route(path("/api/bookings", "/api/bookings/**"), http())
                .filter(lb("booking-service"))
                .build();
    }
}

