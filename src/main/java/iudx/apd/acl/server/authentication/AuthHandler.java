package iudx.apd.acl.server.authentication;

import static iudx.apd.acl.server.apiserver.util.Constants.API_ENDPOINT;
import static iudx.apd.acl.server.apiserver.util.Constants.API_METHOD;
import static iudx.apd.acl.server.apiserver.util.Constants.APPLICATION_JSON;
import static iudx.apd.acl.server.apiserver.util.Constants.AUTHORIZATION_KEY;
import static iudx.apd.acl.server.apiserver.util.Constants.CONTENT_TYPE;
import static iudx.apd.acl.server.apiserver.util.Constants.DETAIL;
import static iudx.apd.acl.server.apiserver.util.Constants.EMAIL_ID;
import static iudx.apd.acl.server.apiserver.util.Constants.FIRST_NAME;
import static iudx.apd.acl.server.apiserver.util.Constants.HEADER_TOKEN;
import static iudx.apd.acl.server.apiserver.util.Constants.LAST_NAME;
import static iudx.apd.acl.server.apiserver.util.Constants.RS_SERVER_URL;
import static iudx.apd.acl.server.apiserver.util.Constants.TITLE;
import static iudx.apd.acl.server.apiserver.util.Constants.TYPE;
import static iudx.apd.acl.server.apiserver.util.Constants.USER_ROLE;
import static iudx.apd.acl.server.apiserver.util.Constants.VERIFY_POLICY_API;
import static iudx.apd.acl.server.authentication.Constants.AUD;
import static iudx.apd.acl.server.authentication.Constants.GET_USER;
import static iudx.apd.acl.server.authentication.Constants.INSERT_USER_TABLE;
import static iudx.apd.acl.server.authentication.Constants.ROLE;
import static iudx.apd.acl.server.authentication.Constants.USER_ID;
import static iudx.apd.acl.server.common.ResponseUrn.INVALID_TOKEN_URN;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import iudx.apd.acl.server.apiserver.util.User;
import iudx.apd.acl.server.common.Api;
import iudx.apd.acl.server.common.HttpStatusCode;
import iudx.apd.acl.server.common.ResponseUrn;
import iudx.apd.acl.server.policy.PostgresService;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AuthHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(AuthHandler.class);
  static AuthenticationService authenticator;
  static Api api;
  private static AuthClient authClient;
  private static PostgresService pgService;
  private HttpServerRequest request;

  public static AuthHandler create(
      Api apis,
      AuthenticationService authenticationService,
      AuthClient client,
      PostgresService postgresService) {
    authenticator = authenticationService;
    api = apis;
    authClient = client;
    pgService = postgresService;
    return new AuthHandler();
  }

  @Override
  public void handle(RoutingContext context) {
    request = context.request();

    String token = request.headers().get(AUTHORIZATION_KEY);
    final String method = context.request().method().toString();
    final String path = getNormalizedPath(request.path());

    JsonObject authInfo =
        new JsonObject().put(API_ENDPOINT, path).put(HEADER_TOKEN, token).put(API_METHOD, method);

    if (path.equals(VERIFY_POLICY_API)) {
      if (token.trim().split(" ").length == 2) {
        token = token.trim().split(" ")[1];
        authInfo.put(HEADER_TOKEN, token);
        Future<Void> verifyFuture = authenticator.tokenIntrospectForVerify(authInfo);
        verifyFuture.onComplete(
            verifyHandler -> {
              if (verifyHandler.succeeded()) {
                LOGGER.info("User Verified Successfully.");
                context.next();
              } else if (verifyHandler.failed()) {
                LOGGER.error("User Verification Failed. " + verifyHandler.cause().getMessage());
                processAuthFailure(context, verifyHandler.cause().getMessage());
              }
            });
      } else {
        processAuthFailure(context, "invalid token");
      }
    } else {
      checkIfAuth(authInfo)
          .onSuccess(
              userObj -> {
                LOGGER.info("User Verified Successfully.");
                context.put("user", userObj);
                context.next();
              })
          .onFailure(
              fail -> {
                LOGGER.error("User Verification Failed. " + fail.getMessage());
                processAuthFailure(context, fail.getMessage());
              });
    }
  }

  Future<User> checkIfAuth(JsonObject authenticationInfo) {
    Promise<User> promise = Promise.promise();
    Future<JsonObject> tokenIntrospect = authenticator.tokenIntrospect(authenticationInfo);

    Future<User> getUserInfo = tokenIntrospect.compose(this::getUserInfo);
    getUserInfo.onSuccess(promise::complete).onFailure(promise::fail);
    return promise.future();
  }

  private Future<User> getUserInfo(JsonObject jsonObject) {
    LOGGER.info("Getting User Info.");
    Promise<User> promise = Promise.promise();
    Tuple tuple = Tuple.of(UUID.fromString(jsonObject.getString("userId")));
    UserContainer userContainer = new UserContainer();
    pgService
        .getPool()
        .withConnection(
            sqlConnection ->
                sqlConnection
                    .preparedQuery(GET_USER)
                    .execute(tuple)
                    .onFailure(
                        existingIdFailureHandler -> {
                          LOGGER.error(
                              "checkIfUserExist db fail {}",
                              existingIdFailureHandler.getLocalizedMessage());
                        }))
        .onSuccess(
            rows -> {
              if (rows != null && rows.size() > 0) {
                LOGGER.info("User found in db.");
                Row row = rows.iterator().next();
                JsonObject result = row.toJson(); // Get the single row
                JsonObject userObj = new JsonObject();
                userObj.put(USER_ID, jsonObject.getString(USER_ID));
                userObj.put(USER_ROLE, jsonObject.getString(ROLE));
                userObj.put(EMAIL_ID, result.getString("email_id"));
                userObj.put(FIRST_NAME, result.getString("first_name"));
                userObj.put(LAST_NAME, result.getString("last_name"));
                userObj.put(RS_SERVER_URL, jsonObject.getString(AUD));
                //
                // userObj.put(IS_DELEGATE,jsonObject.getBoolean(IS_DELEGATE));

                User user = new User(userObj);
                promise.complete(user);
              } else {
                LOGGER.info("Getting user from Auth");
                Future<User> getUserFromAuth = authClient.fetchUserInfo(jsonObject);
                Future<Void> insertIntoDb =
                    getUserFromAuth.compose(
                        userObj -> {
                          userContainer.user = userObj;
                          return insertUserIntoDb(userContainer.user);
                        });
                insertIntoDb
                    .onSuccess(
                        successHandler -> {
                          promise.complete(userContainer.user);
                        })
                    .onFailure(promise::fail);
              }
            });
    return promise.future();
  }

  private Future<Void> insertUserIntoDb(User user) {
    Promise<Void> promise = Promise.promise();
    Tuple tuple =
        Tuple.of(user.getUserId(), user.getEmailId(), user.getFirstName(), user.getLastName());

    pgService
        .getPool()
        .withConnection(
            sqlConnection ->
                sqlConnection
                    .preparedQuery(INSERT_USER_TABLE)
                    .execute(tuple)
                    .onFailure(
                        existingIdFailureHandler -> {
                          LOGGER.error(
                              "insertUserIntoDb db fail {}",
                              existingIdFailureHandler.getLocalizedMessage());
                          promise.fail(existingIdFailureHandler.getLocalizedMessage());
                        })
                    .onSuccess(
                        successHandler -> {
                          LOGGER.info("User inserted in db successfully.");
                          promise.complete();
                        }));
    return promise.future();
  }

  /**
   * get normalized path without id as path param.
   *
   * @param url complete path from request
   * @return path without id.
   */
  private String getNormalizedPath(String url) {
    LOGGER.debug("URL: " + url);
    String[] urlsToMatch = {api.getPoliciesUrl(), api.getRequestPoliciesUrl(), api.getVerifyUrl()};

    for (String apiUrl : urlsToMatch) {
      if (url.matches(apiUrl)) {
        return apiUrl;
      }
    }

    return null;
  }

  private void processAuthFailure(RoutingContext ctx, String failureMessage) {
    ResponseUrn responseUrn = INVALID_TOKEN_URN;
    HttpStatusCode statusCode = HttpStatusCode.getByValue(401);
    if (failureMessage.equalsIgnoreCase("User information is invalid")) {
      responseUrn = ResponseUrn.INTERNAL_SERVER_ERROR;
      statusCode = HttpStatusCode.INTERNAL_SERVER_ERROR;
    }
    LOGGER.error("Error : Authentication Failure");
    ctx.response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(statusCode.getValue())
        .end(generateResponse(responseUrn, statusCode).toString());
  }

  private JsonObject generateResponse(ResponseUrn urn, HttpStatusCode statusCode) {
    return new JsonObject()
        .put(TYPE, urn.getUrn())
        .put(TITLE, statusCode.getDescription())
        .put(DETAIL, statusCode.getDescription());
  }

  static final class UserContainer {
    User user;
  }
}
