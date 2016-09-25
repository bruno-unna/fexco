package com.fexco.test;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

/**
 * A verticle delivering real responses for eircode queries.
 * It queries the redis server (cache) and if it misses, queries the external
 * service before storing the result to redis (asynchronously, of course).
 */
public class ProxyService extends AbstractVerticle {

    private Logger logger = LoggerFactory.getLogger(ProxyService.class);
    private RedisClient redis;

    @Override
    public void start(Future<Void> future) throws Exception {
        final Integer port = config().getInteger("http.port", 8080);

        Router router = Router.router(vertx);
        router.get("/pcw/:apiKey/address/ie/:fragment").handler(this::handleRequest);

        // TODO get this configuration from another place
        RedisOptions redisOptions = new RedisOptions()
                .setHost("127.0.0.1");
        redis = RedisClient.create(vertx, redisOptions);
        // TODO make this info extraction more reliable
        redis.info(jsonObjectAsyncResult -> logger.info(jsonObjectAsyncResult.result().toString()));

        vertx
                .createHttpServer()
                .requestHandler(router::accept)
                .listen(port, result -> {
                    if (result.succeeded()) {
                        logger.info("Mock server started on port " + port);
                        future.complete();
                    } else {
                        logger.error("Couldn't start mock server on port " + port);
                        future.fail(result.cause());
                    }
                });
    }

    private void handleRequest(RoutingContext routingContext) {
        String apiKey = routingContext.request().getParam("apiKey");
        String fragment = routingContext.request().getParam("fragment");
        String format = routingContext.request().getParam("format");
        logger.info("in getAddress, apiKey=" + apiKey + ", fragment=" + fragment);

        routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE.toString(), HttpHeaderValues.APPLICATION_JSON);

        if (apiKey == null || fragment == null) {
            routingContext
                    .response()
                    .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                    .end(new JsonObject()
                            .put("code", HttpResponseStatus.BAD_REQUEST.code())
                            .put("message", HttpResponseStatus.BAD_REQUEST.reasonPhrase())
                            .encodePrettily());
        } else {
            routingContext
                    .response()
                    .setStatusCode(HttpResponseStatus.OK.code())
                    .end(Json.encodePrettily(getAddressesString(apiKey, fragment)));
        }
    }

    private String getAddressesString(String apiKey, String fragment) {
        return null;
    }

}
