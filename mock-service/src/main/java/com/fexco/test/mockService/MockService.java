package com.fexco.test.mockService;

import com.fexco.test.common.MicroServiceVerticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * a verticle generating mock responses for eircode queries
 */
public class MockService extends AbstractVerticle {

    private Logger logger = LoggerFactory.getLogger(MockService.class);

    @Override
    public void start(Future<Void> future) throws Exception {
        final Integer port = config().getInteger("http.port", 8080);
        vertx
                .createHttpServer()
                .requestHandler(request -> {
                    request.response().end("Hello kitty");
                })
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

}
