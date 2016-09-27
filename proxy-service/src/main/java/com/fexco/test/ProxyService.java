package com.fexco.test;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

/**
 * A verticle delivering real responses for eircode queries.
 * It queries the redis server (cache) and if it misses, queries the external
 * service before storing the result to redis (asynchronously, of course).
 */
public class ProxyService extends AbstractVerticle {

    private Logger logger = LoggerFactory.getLogger(ProxyService.class);
    private RedisClient redis;
    private HttpClient externalHttpClient;

    @Override
    public void start(Future<Void> future) throws Exception {

        // fist we need operational parameters, either from
        // environment or from vertx configuration file
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

        // a connection with a redis server is needed by this service
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

        // a http client for the external service will be needed too
        externalHttpClient = vertx.createHttpClient();

        // create the routes that are recognised by the service
        // and send requests to appropriate handler/catalog
        Router router = Router.router(vertx);
        router.get("/pcw/:apiKey/address/ie/:fragment").handler((routingContext) ->
                handleRequest(routingContext, AddressCatalog.eirCode));
        router.get("/pcw/:apiKey/address/uk/:fragment").handler((routingContext) ->
                handleRequest(routingContext, AddressCatalog.premise));

        // finally, create the http server, using the created router
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
     * requests received from the outer world, validating and routing
     * them.
     *
     * @param routingContext routing context as provided by vertx-web
     */
    private void handleRequest(RoutingContext routingContext, AddressCatalog catalog) {
        String apiKey = routingContext.request().getParam("apiKey");
        String fragment = routingContext.request().getParam("fragment");
        String format = routingContext.request().getParam("format");
        logger.info("received a request, apiKey=" + apiKey + ", fragment=" + fragment);

        routingContext.response().putHeader(CONTENT_TYPE.toString(), APPLICATION_JSON);

        if (apiKey == null || fragment == null) {
            routingContext
                    .response()
                    .setStatusCode(BAD_REQUEST.code())
                    .end(new JsonObject()
                            .put("code", BAD_REQUEST.code())
                            .put("message", BAD_REQUEST.reasonPhrase())
                            .encodePrettily());
        } else {
            sendResponseTo(routingContext.response(), apiKey, catalog, fragment);
        }
    }

    /**
     * Look for the required fragment in redis. If not found, query the proxy-ed service and store
     * the result in redis. In any case, send the response through the given response object.
     *
     * @param response response object to use when sending the response
     * @param apiKey   authentication token from the client
     * @param catalog  catalog upon which the query is to be performed
     * @param fragment string for which the search is performed  @return result of the search,
     *                 either fresh or cached
     */
    private void sendResponseTo(HttpServerResponse response, String apiKey, AddressCatalog catalog, String fragment) {
        redis.get(catalog.getPrefix() + ":" + fragment, stringAsyncResult -> {
            if (stringAsyncResult.failed()) {
                response
                        .setStatusCode(INTERNAL_SERVER_ERROR.code())
                        .end(new JsonObject()
                                .put("code", INTERNAL_SERVER_ERROR.code())
                                .put("message", INTERNAL_SERVER_ERROR.reasonPhrase())
                                .encodePrettily());
            } else {
                String redisResult = stringAsyncResult.result();
                if (redisResult != null) {
                    // found it in redis, just return it
                    logger.info("Fragment [" + fragment + "] has been found in redis, returning it");
                    response
                            .setStatusCode(OK.code())
                            .end(redisResult);
                } else {
                    logger.info("Fragment [" + fragment + "] has NOT been found in redis, querying it");
                    externalHttpClient.getNow(80, "ws.postcoder.com", "/pcw/PCW45-12345-12345-1234X/address/ie/D02X285?format=json",
                            httpClientResponse -> {
                                logger.info("Reply from postcoder has been received");
                                if (httpClientResponse.statusCode() != OK.code()) {
                                    logger.info("But reply from postcoder is not ok");
                                    response.setStatusCode(INTERNAL_SERVER_ERROR.code())
                                            .end(httpClientResponse.statusMessage());
                                } else {
                                    logger.info("And reply from postcoder is ok");
                                    httpClientResponse.bodyHandler(buffer -> {
                                        redis.set(catalog.getPrefix() + ":" + fragment, buffer.toString(), voidAsyncResult -> {
                                            logger.info("Stored fragment [" + fragment + "] in redis, with value [" + buffer.toString() + "]");
                                        });
                                        response.setStatusCode(OK.code())
                                                .end(buffer);
                                    });
                                }
                            });
                }
            }
        });

    }

}
