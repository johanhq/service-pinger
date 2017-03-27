package io.vertx.pinger.http;

import com.jayway.restassured.RestAssured;
import io.vertx.pinger.data.Service;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.delete;
import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class MyRestIT {

    @BeforeClass
    public static void configureRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = Integer.getInteger("http.port", 8080);
    }

    @AfterClass
    public static void unconfigureRestAssured() {
        RestAssured.reset();
    }

    @Test
    public void checkThatWeCanRetrieveProducts() {
        // Get the list of bottles, ensure it's a success and extract the first id.
        final String id = get("/service").then()
                .assertThat()
                .statusCode(200)
                .extract()
                .jsonPath().getString("id");
    }

    @Test
    public void checkWeCanAddAndDeleteAProduct() {
        // Create a new bottle and retrieve the result (as a Service instance).
        Service service = given()
                .body("{\"name\":\"Postkod\", \"url\":\"http://www.postkodlotteriet.se\"}").request().post("/service").thenReturn().as(Service.class);
        assertThat(service.getName()).isEqualToIgnoringCase("Postkod");
        assertThat(service.getUrl()).isEqualToIgnoringCase("http://www.postkodlotteriet.se");
        assertThat(service.getId()).isNotEmpty();
        // Delete the bottle
        delete("/service/" + service.getId()).then().assertThat().statusCode(204);
    }
}