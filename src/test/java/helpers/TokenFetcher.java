package helpers;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;

import static io.restassured.RestAssured.given;

public class TokenFetcher {

    private static final String PROJECT_KEY = "NgxUJkOH.5UPAx3RjXTPzdHIOyXpQcXgO5jbhVmm6xAN";
    private static final String PROJECT_ID = "16617b0a-d4b0-4a44-a5b3-d454688cb89c";
    private static final String BASE_URL = "https://" + PROJECT_ID + ":" + PROJECT_KEY + "@api.up42.com";

    public static String getToken() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        return
                given()
                        .spec(
                                new RequestSpecBuilder()
                                        .setBaseUri(BASE_URL)
                                        .setContentType(ContentType.URLENC)
                                        .build()
                        ).formParam("grant_type", "client_credentials")
                        .when()
                        .post("/oauth/token")
                        .prettyPeek()
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .extract().body().path("data.accessToken");
    }
}
