package iudx.apd.acl.server.authentication;

import static iudx.apd.acl.server.apiserver.util.Constants.EMAIL_ID;
import static iudx.apd.acl.server.apiserver.util.Constants.FIRST_NAME;
import static iudx.apd.acl.server.apiserver.util.Constants.LAST_NAME;
import static iudx.apd.acl.server.apiserver.util.Constants.RS_SERVER_URL;
import static iudx.apd.acl.server.apiserver.util.Constants.USER_ROLE;
import static iudx.apd.acl.server.authentication.Constants.AUD;
import static iudx.apd.acl.server.authentication.Constants.ROLE;
import static iudx.apd.acl.server.authentication.Constants.SEARCH_PATH;
import static iudx.apd.acl.server.authentication.Constants.USER_ID;
import static iudx.apd.acl.server.common.HttpStatusCode.INTERNAL_SERVER_ERROR;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import iudx.apd.acl.server.apiserver.util.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AuthClient implements AuthClientInterface {

  private static final Logger LOGGER = LogManager.getLogger(AuthClient.class);
  private final WebClient client;
  private final String authHost;
  private final String authServerSearchPath;
  private final String clientId;
  private final String clientSecret;
  private final int authPort;

  public AuthClient(JsonObject config, WebClient webClient) {
    this.client = webClient;
    this.authHost = config.getString("authHost");
    this.authServerSearchPath = config.getString("dxAuthBasePath") + SEARCH_PATH;
    this.clientId = config.getString("clientId");
    this.clientSecret = config.getString("clientSecret");
    this.authPort = config.getInteger("authPort");
  }

  @Override
  public Future<User> fetchUserInfo(JsonObject jsonObject) {
    final Promise<User> promise = Promise.promise();
    String userId = jsonObject.getString(USER_ID);
    String iudxRole = jsonObject.getString(ROLE).toLowerCase();
    String resourceServer = jsonObject.getString(AUD);

    LOGGER.debug("JsonObject params : {}", jsonObject.encodePrettily());
    LOGGER.debug("authHost : {}", authHost);
    LOGGER.debug("authServerSearchPath : {}", authServerSearchPath);
    LOGGER.debug("authPort: {}", authPort);

    Future<HttpResponse<Buffer>> responseFuture =
        client
            .get(authPort, authHost, authServerSearchPath)
            .putHeader("clientId", this.clientId)
            .putHeader("clientSecret", this.clientSecret)
            .addQueryParam("role", iudxRole)
            .addQueryParam("userId", userId)
            .addQueryParam("resourceServer", resourceServer)
            .send();
    responseFuture.onComplete(
        authHandler -> {
          if (authHandler.succeeded()) {
            JsonObject authResult = authHandler.result().bodyAsJsonObject();
            LOGGER.debug("authResult : {}", authResult.encodePrettily());
            if (authResult.getString("type").equals("urn:dx:as:Success")) {
              LOGGER.info("User found in auth.");
              JsonObject result = authResult.getJsonObject("results");
              JsonObject userObj = new JsonObject();
              userObj.put(USER_ID, userId);
              userObj.put(USER_ROLE, iudxRole);
              userObj.put(EMAIL_ID, result.getString("email"));
              userObj.put(FIRST_NAME, result.getJsonObject("name").getString("firstName"));
              userObj.put(LAST_NAME, result.getJsonObject("name").getString("lastName"));
              userObj.put(RS_SERVER_URL, resourceServer);

              boolean checkIfUserInfoIsInvalid =
                  result.getString("email") == null
                      || result.getJsonObject("name") == null
                      || result.getJsonObject("name").getString("firstName") == null
                      || result.getJsonObject("name").getString("lastName") == null;
              if (checkIfUserInfoIsInvalid) {
                LOGGER.error("Some user info from Auth is null");
                LOGGER.error("Result from auth is {}", result.encode());
                promise.fail("User information is invalid");
              } else {
                //                userObj.put(IS_DELEGATE,isDelegate);
                User user = new User(userObj);
                promise.complete(user);
              }

            } else {
              LOGGER.error("User not present in Auth.");
              promise.fail("User not present in Auth.");
            }
          } else {
            LOGGER.error("fetchItem error : " + authHandler.cause().getMessage());
            promise.fail(INTERNAL_SERVER_ERROR.getDescription());
          }
        });
    return promise.future();
  }
}
