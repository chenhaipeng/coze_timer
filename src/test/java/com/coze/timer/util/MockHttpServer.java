package com.coze.timer.util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * 用于测试的模拟HTTP服务器
 */
public class MockHttpServer {
    private HttpServer server;
    private int port;
    
    /**
     * 启动一个模拟的HTTP服务器
     * 
     * @param port 端口号
     * @param responseBody 响应内容
     * @param statusCode HTTP状态码
     * @throws IOException 如果启动服务器失败
     */
    public void start(int port, String responseBody, int statusCode) throws IOException {
        this.port = port;
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new MockHttpHandler(responseBody, statusCode));
        server.setExecutor(Executors.newFixedThreadPool(2));
        server.start();
        System.out.println("Mock HTTP Server started on port " + port);
    }
    
    /**
     * 停止HTTP服务器
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("Mock HTTP Server stopped");
        }
    }
    
    /**
     * 获取服务器URL
     */
    public String getUrl() {
        return "http://localhost:" + port;
    }
    
    /**
     * 处理HTTP请求的处理器
     */
    static class MockHttpHandler implements HttpHandler {
        private final String responseBody;
        private final int statusCode;
        
        public MockHttpHandler(String responseBody, int statusCode) {
            this.responseBody = responseBody;
            this.statusCode = statusCode;
        }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, responseBody.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBody.getBytes());
            }
        }
    }
} 