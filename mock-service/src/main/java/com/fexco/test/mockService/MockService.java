package com.fexco.test.mockService;

import com.fexco.test.common.MicroServiceVerticle;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * a verticle generating mock responses for eircode queries
 */
public class MockService extends MicroServiceVerticle {

    /**
     * The address on which the data are sent.
     */
    public static final String ADDRESS = "market";

    @Override
    public void start(Future<Void> future) throws Exception {
        vertx
                .createHttpServer()
                .requestHandler(request -> {
                    request.response().end("Hello kitty");
                })
                .listen(8080, result -> {
                    if (result.succeeded()) {
                        future.complete();
                    } else {
                        future.fail(result.cause());
                    }
                });
    }

    /**
     * This method is called when the verticle is deployed.
     */
    @Override
    public void start() {
        super.start();

        // Deploy service verticle without configuration.
//        vertx.deployVerticle(RestQuoteAPIVerticle.class.getName(), new DeploymentOptions().setConfig(config()));

        final Integer port = config().getInteger("http.port", 8080);
        publishHttpEndpoint("quotes", "localhost", port, ar -> {
            if (ar.failed()) {
                ar.cause().printStackTrace();
            } else {
                System.out.println("Quotes (Rest endpoint) service published : " + ar.succeeded());
            }
        });
    }
}
