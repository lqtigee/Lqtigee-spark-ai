package com.lqtigee.sparkai.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RunWebSocketHandler runWebSocketHandler;

    public WebSocketConfig(RunWebSocketHandler runWebSocketHandler) {
        this.runWebSocketHandler = runWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(runWebSocketHandler, "/ws/runs").setAllowedOrigins("*");
    }
}
