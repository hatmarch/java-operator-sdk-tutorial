package org.quarkus.operatortutorial.demoapp;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class DemoLoggerTest {

    @Test
    public void test1() {
        // given()
        //   .when().get("/hello-resteasy")
        //   .then()
        //      .statusCode(200)
        //      .body(is("Hello RESTEasy"));
    }

}