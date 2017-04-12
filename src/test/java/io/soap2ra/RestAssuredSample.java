package io.soap2ra;

import io.restassured.RestAssured;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class RestAssuredSample {

    @BeforeClass
    public static void setUp() {

        RestAssured.baseURI = "https://httpbin.org";
        RestAssured.basePath = "";

    }

    @Test
    //TODO remove once not used anymore...
    public void sampleGetWithRestAssured() {
        given()
                .param("param1", "value1")
                .param("param1", "value2")
        .when()
                .get("/get")
        .then()
                .log().all()
                .time(lessThan(2L))
                //.body(contains("Bla"))
                .statusCode(not(isOneOf(501, 400)))
                .body("args.param1", hasSize(2))
                .body("args.param2", nullValue())
                .body("args.param1[0]", equalTo("value1"))
                .body("args.param1[1]", equalTo("value2"));
    }
}
