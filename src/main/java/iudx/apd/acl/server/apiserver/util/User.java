package iudx.apd.acl.server.apiserver.util;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Objects;

/**
 * User class is used to initialize information about the user like id, role, email-Id etc., <br>
 * User class can store information about Consumer, Provider. <br>
 * If the request is made from Provider Delegate, Consumer delegate, it stores information about the
 * Provider, Consumer whom the user is delegate of
 */
@DataObject(generateConverter = true)
public class User {
  private final String userId;
  private final Role userRole;
  private final String emailId;
  private final String firstName;
  private final String lastName;

  //  private final boolean isDelegate;

  private final String effectiveResourceServerUrl;

  public User(JsonObject userDetails) {
    this.userId = userDetails.getString("userId");
    this.userRole = Role.fromString(userDetails.getString("userRole"));
    this.emailId = userDetails.getString("emailId");
    this.firstName = userDetails.getString("firstName");
    this.lastName = userDetails.getString("lastName");
    this.effectiveResourceServerUrl = userDetails.getString("aud");
    //    this.isDelegate = userDetails.getBoolean("isDelegate");

    /* Converts JsonObject to User class object or dataObject conversion [Deserialization] */
    UserConverter.fromJson(userDetails, this);
  }

  /**
   * Converts Data object or User class object to json object [Serialization]
   *
   * @return JsonObject
   */
  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    UserConverter.toJson(this, jsonObject);
    return jsonObject;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public Role getUserRole() {
    return userRole;
  }

  public String getEmailId() {
    return emailId;
  }

  public String getUserId() {
    return userId;
  }

  //  public boolean isDelegate() {
  //    return isDelegate;
  //  }

  public String getEffectiveResourceServerUrl() {
    return effectiveResourceServerUrl;
  }

  public String toString() {
    return "User details :: \nuserId - "
        + userId
        + ",\n emailId - "
        + emailId
        + ",\n userRole - "
        + userRole
        + ",\n firstName - "
        + firstName
        + ",\n lastName - "
        + lastName
        + ",\n resourceServerUrl - "
        + effectiveResourceServerUrl
        //  + ",\n isDelegate "
        //  + isDelegate
        ;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof User)) {
      return false;
    }
    User user = (User) o;
    return Objects.equals(userId, user.userId)
        && userRole == user.userRole
        && Objects.equals(emailId, user.emailId)
        && Objects.equals(firstName, user.firstName)
        && Objects.equals(lastName, user.lastName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, userRole, emailId, firstName, lastName);
  }
}
