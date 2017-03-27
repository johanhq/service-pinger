package io.vertx.pinger.data;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class Service {
    private static final AtomicInteger COUNTER = new AtomicInteger();

    private final String id;
    private String name;
    private String url;
    private String status;
    private String lastCheck;


    public Service(String name, String url, String status, String lastCheck) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.url = url;
        this.status = status;
        this.lastCheck = lastCheck;
    }

    public Service(String name, String url) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.url = url;
        this.status = "";
        this.lastCheck = "";
    }

    public Service() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLastCheck() {
        return lastCheck;
    }

    public void setLastCheck(String lastCheck) {
        this.lastCheck = lastCheck;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
