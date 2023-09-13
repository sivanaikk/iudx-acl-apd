package iudx.apd.acl.server.apiserver.util;

import static iudx.apd.acl.server.apiserver.util.Constants.EMAIL_ID;
import static iudx.apd.acl.server.apiserver.util.Constants.FIRST_NAME;
import static iudx.apd.acl.server.apiserver.util.Constants.LAST_NAME;
import static iudx.apd.acl.server.apiserver.util.Constants.RS_SERVER_URL;
import static iudx.apd.acl.server.apiserver.util.Constants.USER_ID;
import static iudx.apd.acl.server.apiserver.util.Constants.USER_ROLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.apd.acl.server.Utility;
import iudx.apd.acl.server.validation.exceptions.DxRuntimeException;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@org.junit.jupiter.api.TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith({MockitoExtension.class, VertxExtension.class})
public class TestUser {
  User user;
  JsonObject userDetails;
  UUID userId;
  String role;
  String emailId;
  String firstName;
  String lastName;

  static Stream<Arguments> values4FailedCases() {
    return Stream.of(
        Arguments.of(false, new JsonObject()),
        Arguments.of(false, mock(User.class)),
        Arguments.of(
            false,
            new User(
                new JsonObject()
                    .put(USER_ID, Utility.generateRandomUuid())
                    .put(USER_ROLE, Role.CONSUMER)
                    .put(EMAIL_ID, null)
                    .put(RS_SERVER_URL, Utility.generateRandomString())
                    .put(LAST_NAME, Utility.generateRandomString()))));
  }

  @BeforeEach
  public void setUp(VertxTestContext vertxTestContext) {
    userId = Utility.generateRandomUuid();
    role = Role.CONSUMER.getRole();
    emailId = Utility.generateRandomEmailId();
    firstName = Utility.generateRandomString();
    lastName = Utility.generateRandomString();

    userDetails =
        new JsonObject()
            .put(USER_ID, userId)
            .put(USER_ROLE, role)
            .put(EMAIL_ID, emailId)
            .put(FIRST_NAME, firstName)
            .put(LAST_NAME, lastName);
    user = new User(userDetails);
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test toJsonMethod : Success")
  public void testToJsonMethod(VertxTestContext vertxTestContext) {
    JsonObject expected = user.toJson();
    assertEquals(emailId, expected.getString(EMAIL_ID));
    assertEquals(firstName, expected.getString(FIRST_NAME));
    assertEquals(lastName, expected.getString(LAST_NAME));
    assertEquals(userId.toString(), expected.getString(USER_ID));
    assertEquals(Role.fromString(role).toString(), expected.getString(USER_ROLE));
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test toJson method when role is null")
  public void testWithNullRole(VertxTestContext vertxTestContext) {
    userId = Utility.generateRandomUuid();
    role = null;
    emailId = Utility.generateRandomEmailId();
    firstName = Utility.generateRandomString();
    lastName = Utility.generateRandomString();

    JsonObject userDetails =
        new JsonObject()
            .put(USER_ID, userId)
            .put(USER_ROLE, role)
            .put(EMAIL_ID, emailId)
            .put(FIRST_NAME, firstName)
            .put(LAST_NAME, lastName);
    assertThrows(DxRuntimeException.class, () -> new User(userDetails));
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test toJson method when role is null")
  public void testWithNullEmail(VertxTestContext vertxTestContext) {
    userId = Utility.generateRandomUuid();
    role = Role.PROVIDER_DELEGATE.getRole();
    emailId = null;
    firstName = Utility.generateRandomString();
    lastName = Utility.generateRandomString();

    JsonObject userDetails =
        new JsonObject()
            .put(USER_ID, userId)
            .put(USER_ROLE, role)
            .put(EMAIL_ID, emailId)
            .put(FIRST_NAME, firstName)
            .put(LAST_NAME, lastName);
    User user = new User(userDetails);
    JsonObject expected = user.toJson();
    assertNull(expected.getString(EMAIL_ID));
    assertEquals(firstName, expected.getString(FIRST_NAME));
    assertEquals(lastName, expected.getString(LAST_NAME));
    assertEquals(userId.toString(), expected.getString(USER_ID));
    assertEquals(Role.fromString(role).toString(), expected.getString(USER_ROLE));
    vertxTestContext.completeNow();
  }

  @ParameterizedTest
  @MethodSource("values4FailedCases")
  @DisplayName("Test equals method : Failure")
  public void testEqualsMethodFailure(boolean expected, Object input, VertxTestContext context) {
    assertEquals(expected, user.equals(input));
    context.completeNow();
  }

  Stream<Arguments> values() {
    return Stream.of(
        Arguments.of(false, user),
        Arguments.of(
            false,
            new User(
                new JsonObject()
                    .put(USER_ID, this.userId)
                    .put(USER_ROLE, Role.CONSUMER.getRole())
                    .put(EMAIL_ID, this.emailId)
                    .put(FIRST_NAME, this.firstName)
                    .put(LAST_NAME, this.lastName))));
  }

  @Test
  @DisplayName("Test equals method ")
  public void testEqualsMethod(VertxTestContext context) {
    User someUser =
        new User(
            new JsonObject()
                .put(USER_ID, this.userId)
                .put(USER_ROLE, Role.CONSUMER.getRole())
                .put(EMAIL_ID, this.emailId)
                .put(FIRST_NAME, this.firstName)
                .put(LAST_NAME, this.lastName));
    assertTrue(user.equals(user));
    assertTrue(user.equals(someUser));
    assertEquals(user.hashCode(), user.hashCode());
    assertEquals(someUser.hashCode(), user.hashCode());
    context.completeNow();
  }
}
