import Entities.*;
import com.github.javafaker.Faker;
import io.restassured.RestAssured;
import io.restassured.filter.log.ErrorLoggingFilter;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.module.jsv.JsonSchemaValidator.*;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.Alphanumeric.class)
public class BookingTests {
    public static Faker faker;
    private static RequestSpecification request;
    private static Booking booking;
    private static BookingDates bookingDates;
    private static User user;
    private static String bookingId;
    private static String token;

    @BeforeAll
    public static void Setup() {
        RestAssured.baseURI = "https://restful-booker.herokuapp.com";
        faker = new Faker();
        user = new User(faker.name().username(),
                faker.name().firstName(),
                faker.name().lastName(),
                faker.internet().safeEmailAddress(),
                faker.internet().password(8, 10),
                faker.phoneNumber().toString());

        bookingDates = new BookingDates("2018-01-02", "2018-01-03");
        booking = new Booking(user.getFirstName(), user.getLastName(),
                (float) faker.number().randomDouble(2, 50, 100000),
                true, bookingDates,
                "");
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter(), new ErrorLoggingFilter());
    }

    @BeforeEach
    void setRequest() {
        request = given().config(RestAssured.config().logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails()))
                .contentType(ContentType.JSON)
                .auth().basic("admin", "password123");
    }

    @Test
    public void test00_createToken_returnOk() {
        Response response = request
                .when()
                .body("{\n" +
                        "    \"username\" : \"admin\",\n" +
                        "    \"password\" : \"password123\"\n" +
                        "}")
                .post("/auth")
                .then()
                .extract()
                .response();

        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.statusCode());
        token = response.body().as(Token.class).getToken();
    }

    @Test
    public void test01_getAllBookingsById_returnOk() {
        Response response = request
                .when()
                .get("/booking")
                .then()
                .extract()
                .response();

        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.statusCode());
    }

    @Test
    public void test02_createBooking_returnOk() {
        Response response = request
                .when()
                .body(booking)
                .post("/booking")
                .then()
                .body(matchesJsonSchemaInClasspath("createBookingRequestSchema.json"))
                .extract()
                .response();

        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.statusCode());
        bookingId = response.body().as(CreateResponse.class).getBookingid();
        Assertions.assertNotNull(bookingId);
    }

    @Test
    public void test03_getBooking_returnOk() {
        Response response = request
                .when()
                .get("/booking/" + bookingId)
                .then()
                .extract()
                .response();

        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.statusCode());
    }

    @Test
    public void test04_updateBooking_returnOk() {
        booking.setFirstname("Mark");
        Response response = request
                .when()
                .header("Cookie", "token=" + token)
                .body(booking)
                .put("/booking/" + bookingId)
                .then()
                .extract()
                .response();

        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.statusCode());
    }

    @Test
    public void test05_partialUpdateBooking_returnOk() {
        Response response = request
                .when()
                .header("Cookie", "token=" + token)
                .body("{\n" +
                        "    \"firstname\" : \"James\",\n" +
                        "    \"lastname\" : \"Brown\"\n" +
                        "}")
                .patch("/booking/" + bookingId)
                .then()
                .extract()
                .response();

        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.statusCode());
    }

    @Test
    public void test06_deleteBooking_returnOk() {
        Response response = request
                .when()
                .header("Cookie", "token=" + token)
                .delete("/booking/" + bookingId)
                .then()
                .extract()
                .response();

        Assertions.assertNotNull(response);
        Assertions.assertEquals(201, response.statusCode());
    }
}
