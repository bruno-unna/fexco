package com.fexco.test.mockService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.ServerSocket;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 * Test for the mock eircom/postcode service.
 */
@RunWith(VertxUnitRunner.class)
public class MockServiceTest {
    private Vertx vertx;
    private int port;

    @Before
    public void setUp(TestContext context) throws Exception {
        vertx = Vertx.vertx();

        // get a random (unused) port for the test
        ServerSocket socket = new ServerSocket(0);
        port = socket.getLocalPort();
        socket.close();

        DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("http.port", port));
        vertx.deployVerticle(MockService.class.getName(), options, context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) throws Exception {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testSimpleRequestResponse(TestContext context) throws Exception {
        final Async async = context.async();


        vertx.createHttpClient().getNow(port, "localhost", "/", response -> {
            response.handler(body -> {
                context.assertTrue(body.toString().contains("Hello"));
                async.complete();
            });
        });
    }

}