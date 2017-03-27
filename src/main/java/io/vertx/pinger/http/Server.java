package io.vertx.pinger.http;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.pinger.data.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class Server extends AbstractVerticle {
    // Store our product
    private Map<String, Service> products = new LinkedHashMap<>();
    private EventBus eb;
    private String fileName;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");


    @Override
    public void start(Future<Void> fut) {

        // Create a router object.
        Router router = Router.router(vertx);
        router.route("/").handler(StaticHandler.create("assets"));
        router.route("/service*").handler(BodyHandler.create());
        router.get("/service").handler(this::getAll);
        router.post("/service").handler(this::addOne);
        router.delete("/service/:id").handler(this::deleteOne);

        // Adding events to the bus
        eb = vertx.eventBus();

        eb.consumer("service.add", message -> {
            Service service = Json.decodeValue((String) message.body(),
                    Service.class);
            products.put(service.getId(), service);
            System.out.println("I have received a message: " + service.getId());
        });

        eb.consumer("service.save", message -> {
            // Write a file
            JsonObject saveData = new JsonObject();
            saveData.put("services",new JsonArray(
                    Json.encodePrettily(products.values())
            ));
            vertx.fileSystem().writeFile(fileName, Buffer.buffer(Json.encodePrettily(saveData)), result -> {
                if (!result.succeeded()) {
                    System.err.println("Oh oh ..." + result.cause());
                }
            });

        });

        eb.consumer("service.update", message -> {
            JsonObject jsonMessage = new JsonObject(message.body().toString());
            Service service = products.get(jsonMessage.getString("id"));
            String status = jsonMessage.getInteger("status") == 200 ? "OK":"FAIL";
            LocalDateTime timePoint = LocalDateTime.now();
            service.setLastCheck(timePoint.format(formatter));
            service.setStatus(status);
            eb.send("service.save", "Saving to file");
        });

        // Reading services from file
        fileName = config().getString("data.file", "target/classes/service.json");
        vertx.fileSystem().readFile(fileName, result -> {
            if (result.succeeded()) {
                JsonObject services = new JsonObject(result.result().toString());
                JsonArray array = services.getJsonArray("services");
                for(Object item : array)
                {
                    JsonObject service = (JsonObject) item;
                    eb.send("service.add", service.toString());
                }
            } else {
                System.err.println("Oh oh ..." + result.cause());
            }
        });

        // Setting up perodic pinger
        HttpClient httpClient = vertx.createHttpClient();
        vertx.setPeriodic(60*1000, id -> {
            products.forEach((serviceId, value) -> {
                Service service = value;
                String url = service.getUrl().replaceAll("^(http|https)://","");
                httpClient.getNow(80, url, "/", response -> {
                    JsonObject result = new JsonObject();
                    result.put("id", service.getId());
                    result.put("status", response.statusCode());
                    eb.send("service.update", result.toString());
                });
            });
        });

        // Create the HTTP server and pass the "accept" method to the request handler.
        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(config().getInteger("http.port", 8080),
                        result -> {
                            if (result.succeeded()) {
                                fut.complete();
                            } else {
                                fut.fail(result.cause());
                            }
                        });
    }

    private void getAll(RoutingContext routingContext) {
        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(new JsonObject().put(
                        "services",new JsonArray(
                                Json.encodePrettily(products.values())
                        )
                )));
    }

    private void addOne(RoutingContext routingContext) {
        final Service service = Json.decodeValue(routingContext.getBodyAsString(),
                Service.class);
        products.put(service.getId(), service);
        eb.send("service.save", "Saving to file");
        routingContext.response()
                .setStatusCode(201)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(service));
    }

    private void deleteOne(RoutingContext routingContext) {
        String id = routingContext.request().getParam("id");
        if (id == null) {
            routingContext.response().setStatusCode(400).end();
        } else {
            products.remove(id);
            eb.send("service.save", "Saving to file");
        }
        routingContext.response().setStatusCode(204).end();
    }
}
