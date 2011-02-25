package org.webbitserver.netty;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.webbitserver.*;

import java.util.Iterator;
import java.util.concurrent.Executor;

public class NettyHttpControl implements HttpControl {

    private final Iterator<HttpHandler> handlerIterator;
    private final Executor executor;
    private final ChannelHandlerContext ctx;
    private final NettyHttpRequest nettyHttpRequest;
    private final org.jboss.netty.handler.codec.http.HttpRequest httpRequest;
    private final DefaultHttpResponse defaultHttpResponse;
    private final Thread.UncaughtExceptionHandler exceptionHandler;
    private final Thread.UncaughtExceptionHandler ioExceptionHandler;

    private HttpRequest defaultRequest;
    private HttpResponse defaultResponse;
    private HttpControl defaultControl;

    public NettyHttpControl(Iterator<HttpHandler> handlerIterator,
                            Executor executor,
                            ChannelHandlerContext ctx,
                            NettyHttpRequest nettyHttpRequest,
                            NettyHttpResponse nettyHttpResponse,
                            org.jboss.netty.handler.codec.http.HttpRequest httpRequest,
                            DefaultHttpResponse defaultHttpResponse,
                            Thread.UncaughtExceptionHandler exceptionHandler,
                            Thread.UncaughtExceptionHandler ioExceptionHandler) {
        this.handlerIterator = handlerIterator;
        this.executor = executor;
        this.ctx = ctx;
        this.nettyHttpRequest = nettyHttpRequest;
        this.httpRequest = httpRequest;
        this.defaultHttpResponse = defaultHttpResponse;
        defaultRequest = nettyHttpRequest;
        defaultResponse = nettyHttpResponse;
        this.ioExceptionHandler = ioExceptionHandler;
        defaultControl = this;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void nextHandler() {
        nextHandler(defaultRequest, defaultResponse, defaultControl);
    }

    @Override
    public void nextHandler(HttpRequest request, HttpResponse response) {
        nextHandler(request, response, defaultControl);
    }

    @Override
    public void nextHandler(HttpRequest request, HttpResponse response, HttpControl control) {
        this.defaultRequest = request;
        this.defaultResponse = response;
        this.defaultControl = control;
        if (handlerIterator.hasNext()) {
            HttpHandler handler = handlerIterator.next();
            try {
                handler.handleHttpRequest(request, response, control);
            } catch (Throwable e) {
                response.error(e);
            }
        } else {
            response.status(404).end();
        }
    }

    @Override
    public NettyWebSocketConnection upgradeToWebSocketConnection(WebSocketHandler handler) {
        NettyWebSocketConnection webSocketConnection = createWebSocketConnection();
        new NettyWebSocketChannelHandler(executor, ctx, nettyHttpRequest, httpRequest,
                defaultHttpResponse, handler, webSocketConnection, exceptionHandler, ioExceptionHandler);
        return webSocketConnection;
    }

    @Override
    public NettyWebSocketConnection createWebSocketConnection() {
        return new NettyWebSocketConnection(executor, nettyHttpRequest, ctx);
    }

    @Override
    public EventSourceConnection upgradeToEventSourceConnection(EventSourceHandler handler) {
        NettyEventSourceConnection eventSourceConnection = new NettyEventSourceConnection(executor, nettyHttpRequest, ctx);
        new NettyEventSourceChannelHandler(executor, ctx, nettyHttpRequest, httpRequest,
                defaultHttpResponse, handler, eventSourceConnection, exceptionHandler, ioExceptionHandler);
        return eventSourceConnection;
    }

    @Override
    public Executor handlerExecutor() {
        return executor;
    }

    @Override
    public void execute(Runnable command) {
        handlerExecutor().execute(command);
    }
}
