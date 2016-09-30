package com.fexco.test.mockService;

import com.fexco.test.mockService.model.EircodeAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.ServerSocket;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 * Test for the mock Eircode/Postcode service.
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

        vertx.createHttpClient()
                .getNow(port, "localhost", "/pcw/PCW45-12345-12345-1234X/address/ie/D02X285?lines=3&format=json", response -> {
                    context.assertEquals(HttpResponseStatus.OK.code(), response.statusCode());
                    response.bodyHandler(body -> {
                        final EircodeAddress[] addresses = Json.decodeValue(body.toString(), EircodeAddress[].class);
                        context.assertEquals(10, addresses.length);
                        async.complete();
                    });
                });
    }

}
