package io.github.giovibal.mqtt;

import io.vertx.core.*;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;

/**
 * Created by giovanni on 11/04/2014.
 * The Main Verticle
 */
public class MQTTBroker extends AbstractVerticle {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MQTTBroker(),
                new DeploymentOptions(),
                new Handler<AsyncResult<String>>() {
                    @Override
                    public void handle(AsyncResult<String> stringAsyncResult) {
                        if(stringAsyncResult.failed())
                            stringAsyncResult.cause().printStackTrace();
                        else {
                            System.out.println(stringAsyncResult.result());
                        }
                    }
                }
        );
    }

    @Override
    public void start() {
        try {
            JsonObject conf = config();
            int port = conf.getInteger("tcp_port", 1883);
            int wsPort = conf.getInteger("websocket_port", 11883);
            boolean wsEnabled = conf.getBoolean("websocket_enabled", true);
            String wsSubProtocols = conf.getString("websocket_subprotocols", "mqtt,mqttv3.1");
//            String[] wsSubProtocolsArr = wsSubProtocols.split(",");

            // MQTT over TCP
            NetServerOptions opt = new NetServerOptions()
                    .setTcpKeepAlive(true)
                    .setPort(port);
            NetServer netServer = vertx.createNetServer(opt);
            netServer.connectHandler(new Handler<NetSocket>() {
                @Override
                public void handle(final NetSocket netSocket) {
                    MQTTNetSocket mqttNetSocket = new MQTTNetSocket(vertx, netSocket);
                    mqttNetSocket.start();
                }
            }).listen();
            Container.logger().info("Startd MQTT TCP-Broker on port: "+ port);

            // MQTT over WebSocket
            if(wsEnabled) {
                HttpServerOptions httpOpt = new HttpServerOptions()
                        .setTcpKeepAlive(true)
                        .setMaxWebsocketFrameSize(1024)
                        .setWebsocketSubProtocol(wsSubProtocols)
                        .setPort(wsPort);

                final HttpServer http = vertx.createHttpServer(httpOpt);
                http.websocketHandler(new Handler<ServerWebSocket>() {
                    @Override
                    public void handle(ServerWebSocket serverWebSocket) {
                        MQTTWebSocket mqttWebSocket = new MQTTWebSocket(vertx, serverWebSocket);
                        mqttWebSocket.start();
                    }
                }).listen();
                Container.logger().info("Startd MQTT WebSocket-Broker on port: " + wsPort);
            }

//            final MQTTStoreManager store = new MQTTStoreManager(vertx, "");
//            // DEBUG
//            vertx.setPeriodic(10000, new Handler<Long>() {
//                @Override
//                public void handle(Long aLong) {
//                    container.logger().info("stats...");
//                    Set<String> clients = store.getClientIDs();
//                    for(String clientID : clients) {
//                        int subscriptions = store.getSubscriptionsByClientID(clientID).size();
//                        container.logger().info(clientID+" ----> "+ subscriptions);
//                    }
//                }
//            });
        } catch(Exception e ) {
            Container.logger().error(e.getMessage(), e);
        }

    }

    @Override
    public void stop() {
    }

}
