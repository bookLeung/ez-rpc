package com.yupi.yurpc.server;

import io.vertx.core.Vertx;

/**
 * Vert.x HTTP 服务器
 */
public class VertxHttpServer implements HttpServer {

    /**
     * 启动服务器
     *
     * @param port
     */
    @Override
    public void doStart(int port) {
        // 创建vertx实例
        Vertx vertx = Vertx.vertx();

        // 创建http服务器
        io.vertx.core.http.HttpServer server = vertx.createHttpServer();

        // 监听端口并处理请求
        server.requestHandler(new HttpServerHandler());

        // 启动http服务器并监听指定端口
        server.listen(port, result -> {
            if (result.succeeded()) {
                System.out.println("Vertx http server is now listening on port " + port);
            } else {
                System.out.println("Failed to start HTTP server: " + result.cause());
            }
        });
    }
}
