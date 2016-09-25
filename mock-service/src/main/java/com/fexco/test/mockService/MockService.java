package com.fexco.test.mockService;

import com.fexco.test.common.MicroServiceVerticle;
import com.fexco.test.mockService.model.EircodeAddress;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * a verticle generating mock responses for eircode queries
 */
public class MockService extends AbstractVerticle {

    private Logger logger = LoggerFactory.getLogger(MockService.class);
    private Random random = new Random(System.currentTimeMillis());

    @Override
    public void start(Future<Void> future) throws Exception {
        final Integer port = config().getInteger("http.port", 8080);

        Router router = Router.router(vertx);
        router.get("/pcw/:apiKey/address/ie/:fragment").handler(this::getAddresses);

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

    private void getAddresses(RoutingContext routingContext) {
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
                    .end(Json.encodePrettily(generateRandomAddresses()));
        }
    }

    private List<EircodeAddress> generateRandomAddresses() {
        return Stream
                .generate(this::randomAddress)
                .limit(10)
                .collect(Collectors.toList());
    }

    private EircodeAddress randomAddress() {
        return new EircodeAddress()
                .setAddressline1(String.valueOf(random.nextDouble()))
                .setAddressline2(String.valueOf(random.nextDouble()))
                .setSummaryline(String.valueOf(random.nextDouble()))
                .setOrganisation(String.valueOf(random.nextDouble()))
                .setStreet(String.valueOf(random.nextDouble()))
                .setPosttown(String.valueOf(random.nextDouble()))
                .setCounty(String.valueOf(random.nextDouble()))
                .setPostcode(String.valueOf(random.nextDouble()));
    }

}
