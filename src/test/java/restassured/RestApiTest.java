package restassured;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import static helpers.TokenFetcher.getToken;
import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class RestApiTest {

    private String workflowId;

    private RequestSpecification spec;

    private static final String PROJECT_ID = "16617b0a-d4b0-4a44-a5b3-d454688cb89c";
    private static final String BASE_URL = "https://api.up42.com/projects/" + PROJECT_ID;

    @BeforeMethod
    void setUp() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        spec = new RequestSpecBuilder()
                .setBaseUri(BASE_URL)
                .setContentType(ContentType.JSON)
                .addHeader("Authorization", "Bearer " + getToken())
                .build();
    }

    @Test
    void test() {
        workflowId = createWorkflow();
        configureWorkflow(workflowId);
        String jobId = createAndRunJob(workflowId);
        waitUntilJobSucceeded(jobId);
        deleteWorkflow(workflowId);
    }

    @AfterMethod
    void deleteWorkflow(ITestResult result) {
        if (ITestResult.FAILURE == result.getStatus()) {
            deleteWorkflow(workflowId);
        }
    }

    private String createWorkflow() {
        InputStream payload =
                getClass().getClassLoader().getResourceAsStream("payloads/create-workflow-payload.json");
        return
                given()
                        .spec(spec)
                        .body(payload)
                        .when()
                        .post("/workflows")
                        .prettyPeek()
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data.id", is(not(emptyString())))
                        .body("data.createdBy.id", is(PROJECT_ID))
                        .extract().path("data.id");
    }

    private void configureWorkflow(String id) {
        InputStream payload =
                getClass().getClassLoader().getResourceAsStream("payloads/configure-workflow-payload.json");
        given()
                .spec(spec)
                .body(payload)
                .when()
                .post("/workflows/{id}/tasks", id)
                .prettyPeek()
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data", hasSize(2));
    }

    private String createAndRunJob(String workflowId) {
        InputStream payload =
                getClass().getClassLoader().getResourceAsStream("payloads/create-job-payload.json");
        return
                given()
                        .spec(spec)
                        .body(payload)
                        .when()
                        .post("/workflows/{id}/jobs", workflowId)
                        .prettyPeek()
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data.id", is(not(emptyString())))
                        .body("data.status", is("NOT_STARTED"))
                        .body("data.workflowId", is(workflowId))
                        .extract().path("data.id");
    }

    private void waitUntilJobSucceeded(String jobId) {
        Awaitility.await()
                .atMost(2, TimeUnit.MINUTES)
                .pollDelay(5, TimeUnit.SECONDS).
                untilAsserted(
                        () -> assertThat("Job's status did not change as expected.",
                                getJobStatus(jobId),
                                equalToIgnoringCase("SUCCEEDED"))
                );
    }

    private String getJobStatus(String id) {
        return
                given()
                        .spec(spec)
                        .when()
                        .get("/jobs/{id}", id)
                        .prettyPeek()
                        .then()
                        .statusCode(HttpStatus.SC_OK).extract().path("data.status");
    }

    private void deleteWorkflow(String id) {
        given()
                .spec(spec)
                .when()
                .delete("/workflows/{id}", id)
                .prettyPeek()
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }
}
