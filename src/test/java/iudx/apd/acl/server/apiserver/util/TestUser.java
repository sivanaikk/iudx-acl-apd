package iudx.apd.acl.server.apiserver.util;

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
                    .put("userId", Utility.generateRandomUuid())
                    .put("userRole", Role.CONSUMER)
                    .put("emailId", null)
                    .put("firstName", Utility.generateRandomString())
                    .put("lastName", Utility.generateRandomString()))));
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
            .put("userId", userId)
            .put("userRole", role)
            .put("emailId", emailId)
            .put("firstName", firstName)
            .put("lastName", lastName);
    user = new User(userDetails);
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test toJsonMethod : Success")
  public void testToJsonMethod(VertxTestContext vertxTestContext) {
    JsonObject expected = user.toJson();
    assertEquals(emailId, expected.getString("emailId"));
    assertEquals(firstName, expected.getString("firstName"));
    assertEquals(lastName, expected.getString("lastName"));
    assertEquals(userId.toString(), expected.getString("userId"));
    assertEquals(Role.fromString(role).toString(), expected.getString("userRole"));
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
            .put("userId", userId)
            .put("userRole", role)
            .put("emailId", emailId)
            .put("firstName", firstName)
            .put("lastName", lastName);
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
            .put("userId", userId)
            .put("userRole", role)
            .put("emailId", emailId)
            .put("firstName", firstName)
            .put("lastName", lastName);
    User user = new User(userDetails);
    JsonObject expected = user.toJson();
    assertNull(expected.getString("emailId"));
    assertEquals(firstName, expected.getString("firstName"));
    assertEquals(lastName, expected.getString("lastName"));
    assertEquals(userId.toString(), expected.getString("userId"));
    assertEquals(Role.fromString(role).toString(), expected.getString("userRole"));
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
                    .put("userId", this.userId)
                    .put("userRole", Role.CONSUMER.getRole())
                    .put("emailId", this.emailId)
                    .put("firstName", this.firstName)
                    .put("lastName", this.lastName))));
  }

  @Test
  @DisplayName("Test equals method ")
  public void testEqualsMethod(VertxTestContext context) {
    User someUser =
        new User(
            new JsonObject()
                .put("userId", this.userId)
                .put("userRole", Role.CONSUMER.getRole())
                .put("emailId", this.emailId)
                .put("firstName", this.firstName)
                .put("lastName", this.lastName));
    assertTrue(user.equals(user));
    assertTrue(user.equals(someUser));
    assertEquals(user.hashCode(), user.hashCode());
    assertEquals(someUser.hashCode(), user.hashCode());
    context.completeNow();
  }
}
