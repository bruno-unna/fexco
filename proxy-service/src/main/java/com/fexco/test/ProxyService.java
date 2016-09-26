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
        String redisHost = System.getenv("REDIS_HOST");
        if (redisHost == null) {
            redisHost = config().getString("redis.host", "redis");
        }
        logger.info("Using redis host " + redisHost);

        Integer redisPort = Integer.getInteger("REDIS_PORT");
        if (redisPort == null) {
            redisPort = config().getInteger("redis.port", 6379);
        }
        logger.info("Using redis port " + redisPort);

        Integer httpPort = Integer.getInteger("HTTP_PORT");
        if (httpPort == null) {
            httpPort = config().getInteger("http.port", 8080);
        }
        logger.info("Using http port " + httpPort);

        RedisOptions redisOptions = new RedisOptions()
                .setHost(redisHost)
                .setPort(redisPort);
        redis = RedisClient.create(vertx, redisOptions);

        // if all was ok with redis, let's inform the user (via log)
        redis.info(jsonObjectAsyncResult -> {
            if (jsonObjectAsyncResult.succeeded()) {
                logger.info("Connection with redis was successful");
                logger.info(jsonObjectAsyncResult.result().toString());
            } else {
                logger.fatal("Error querying redis", jsonObjectAsyncResult.cause());
            }
        });

        Router router = Router.router(vertx);
        router.get("/pcw/:apiKey/address/ie/:fragment").handler(this::handleRequest);

        vertx
                .createHttpServer()
                .requestHandler(router::accept)
                .listen(httpPort, result -> {
                    if (result.succeeded()) {
                        logger.info("Proxy server started");
                        future.complete();
                    } else {
                        logger.error("Couldn't start proxy server");
                        future.fail(result.cause());
                    }
                });
    }

    /**
     * This method is HTTP-related, and is responsible for handling the
     * requests received from the outer world, validating them and routing
     * them.
     *
     * @param routingContext routing context as provided by vertx-web
     */
    private void handleRequest(RoutingContext routingContext) {
        String apiKey = routingContext.request().getParam("apiKey");
        String fragment = routingContext.request().getParam("fragment");
        String format = routingContext.request().getParam("format");
        logger.info("received a request, apiKey=" + apiKey + ", fragment=" + fragment);

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

    /**
     * Looks for the required fragment in redis and returns it if found; otherwise
     * queries the proxy-ed service, stores the result in redis and returns it.
     *
     * @param apiKey authentication token from the client
     * @param fragment string for which the search is performed
     * @return result of the search, either fresh or cached
     */
    private String getAddressesString(String apiKey, String fragment) {
        return null;
    }

}
