package iudx.apd.acl.server.authentication;

import static iudx.apd.acl.server.authentication.Constants.AUD;
import static iudx.apd.acl.server.authentication.Constants.IS_DELEGATE;
import static iudx.apd.acl.server.authentication.Constants.ROLE;
import static iudx.apd.acl.server.authentication.Constants.SEARCH_PATH;
import static iudx.apd.acl.server.authentication.Constants.USER_ID;
import static iudx.apd.acl.server.common.HttpStatusCode.INTERNAL_SERVER_ERROR;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
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

  public AuthClient(JsonObject config) {
    WebClientOptions clientOptions =
        new WebClientOptions().setSsl(false).setVerifyHost(false).setTrustAll(true);
    this.client = WebClient.create(Vertx.vertx(), clientOptions);
    this.authHost = config.getString("authHost");
    this.authServerSearchPath = config.getString("dxAuthBasePath") + SEARCH_PATH;
    this.clientId = config.getString("clientId");
    this.clientSecret = config.getString("clientSecret");
  }

  @Override
  public Future<User> fetchUserInfo(JsonObject jsonObject) {
    Promise<User> promise = Promise.promise();
    String userId = jsonObject.getString(USER_ID);
    String iudxRole = jsonObject.getString(ROLE).toLowerCase();
    String resourceServer = jsonObject.getString(AUD);
    boolean isDelegate = jsonObject.getBoolean(IS_DELEGATE);

    client
        .get(authHost, authServerSearchPath)
        .putHeader("clientId", this.clientId)
        .putHeader("clientSecret", this.clientSecret)
        .addQueryParam("role", iudxRole)
        .addQueryParam("userId", userId)
        .addQueryParam("resourceServer", resourceServer)
        .send()
        .onFailure(
            ar -> {
              LOGGER.error("fetchItem error : " + ar.getCause());
              promise.fail(INTERNAL_SERVER_ERROR.getDescription());
            })
        .onSuccess(
            authSuccessResponse -> {
              JsonObject authResult = authSuccessResponse.bodyAsJsonObject();
              if (authResult.getString("type").equals("urn:dx:as:Success")) {
                LOGGER.info("User found in auth.");
                JsonObject result = authResult.getJsonObject("results");
                JsonObject userObj = new JsonObject();
                userObj.put("userId", userId);
                userObj.put("userRole", iudxRole);
                userObj.put("emailId", result.getString("email"));
                userObj.put("firstName", result.getJsonObject("name").getString("firstName"));
                userObj.put("lastName", result.getJsonObject("name").getString("lastName"));
                userObj.put(AUD, resourceServer);
                //                userObj.put(IS_DELEGATE,isDelegate);
                User user = new User(userObj);
                promise.complete(user);
              } else {
                LOGGER.error("User not present in Auth.");
                promise.fail("User not present in Auth.");
              }
            });
    return promise.future();
  }
}
