package io.vertx.pinger.http;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.pinger.data.Service;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.ServerSocket;

@RunWith(VertxUnitRunner.class)
public class ServerTest {

    private Vertx vertx;
    private int port;

    @Before
    public void setUp(TestContext context) throws IOException {
        vertx = Vertx.vertx();
        ServerSocket socket = new ServerSocket(0);
        port = socket.getLocalPort();
        socket.close();
        JsonObject testConfig = new JsonObject().put("http.port", port).put("data.file", "target/test-classes/service.json");
        DeploymentOptions options = new DeploymentOptions()
                .setConfig(testConfig);
        vertx.deployVerticle(Server.class.getName(), options, context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }


    @Test
    public void checkThatTheIndexPageIsServed(TestContext context) {
        Async async = context.async();
        vertx.createHttpClient().getNow(port, "localhost", "/", response -> {
            context.assertEquals(response.statusCode(), 200);
            context.assertEquals(response.headers().get("content-type"), "text/html");
            response.bodyHandler(body -> {
                context.assertTrue(body.toString().contains("<title>Service pinger</title>"));
                async.complete();
            });
        });
    }

    @Test
    public void checkThatWeCanAdd(TestContext context) {
        Async async = context.async();
        final String json = Json.encodePrettily(new Service("Postkodlotteriet", "http://www.postkodlotteriet.se"));
        final String length = Integer.toString(json.length());
        vertx.createHttpClient().post(port, "localhost", "/service")
                .putHeader("content-type", "application/json")
                .putHeader("content-length", length)
                .handler(response -> {
                    context.assertEquals(response.statusCode(), 201);
                    context.assertTrue(response.headers().get("content-type").contains("application/json"));
                    response.bodyHandler(body -> {
                        final Service service = Json.decodeValue(body.toString(), Service.class);
                        context.assertEquals(service.getName(), "Postkodlotteriet");
                        context.assertEquals(service.getUrl(), "http://www.postkodlotteriet.se");
                        context.assertNotNull(service.getId());
                        async.complete();
                    });
                })
                .write(json)
                .end();
    }
}
