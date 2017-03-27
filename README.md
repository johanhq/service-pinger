# Service pinger
This is a small program that can ping webpages on http and tell you if they are up. Built in vertex.io and backbone.

## Step one, install vertx with brew
```
brew install vert.x
```

## Step two run some tests

```
mvn clean verify
```

## Step three build it

```
mvn clean package
```

## Step four start the server

```
java -jar target/service-pinger-1.0-SNAPSHOT-fat.jar
```

And se the magic at http://localhost:8080