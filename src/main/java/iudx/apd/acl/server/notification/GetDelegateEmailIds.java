package iudx.apd.acl.server.notification;

import static iudx.apd.acl.server.apiserver.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Fetches all the emails of the delegates of the user */
public class GetDelegateEmailIds {
  private static final Logger LOGGER = LogManager.getLogger(GetDelegateEmailIds.class);
  private final String clientId;
  private final String clientSecret;
  private final String authHost;
  private final String authBasePath;
  private final WebClient webClient;

  public GetDelegateEmailIds(JsonObject config, WebClient webClient) {
    this.clientId = config.getString("clientId");
    this.clientSecret = config.getString("clientSecret");
    this.authHost = config.getString("authHost");
    this.authBasePath = config.getString("dxAuthBasePath");
    this.webClient = webClient;
  }

  /**
   * Returns the email Ids of the delegates as Json array
   *
   * @param userId Id of the user to fetch the delegates of the provider/consumer
   * @param resourceServerUrl URL to which the resource belongs to
   * @param role Role of the delegator. <br>
   *     Can be either {<b>provider</b>, <b>consumer</b>}
   * @return Email ID(s) as Json array, if there are no delegates, or if something else goes wrong
   *     while fetching the emailIds it returns failure message with type Future
   */
  public Future<JsonArray> getEmails(String userId, String resourceServerUrl, String role) {
    Promise<JsonArray> promise = Promise.promise();

    webClient
        .get(authHost, authBasePath + REQUEST_EMAIL_IDS)
        .addQueryParam(USER_ID, userId)
        .addQueryParam(ROLE, role)
        .addQueryParam(RESOURCE_SERVER_URL, resourceServerUrl)
        .putHeader(CLIENT_ID, this.clientId)
        .putHeader(CLIENT_SECRET, this.clientSecret)
        .send(
            responseHandler -> {
              if (responseHandler.succeeded()) {
                JsonObject results =
                    responseHandler.result().bodyAsJsonObject().getJsonObject(RESULT);
                if (results == null || results.isEmpty()) {
                  promise.fail("Could not fetch emails");
                } else {
                  JsonArray emails = results.getJsonArray(DELEGATE_EMAILS);
                  boolean isDelegateEmailIdEmpty = emails.isEmpty();
                  if (isDelegateEmailIdEmpty) {
                    promise.fail("No delegates for the given provider");
                  } else {
                    LOGGER.info("Fetched delegate email Ids successfully");
                    promise.complete(emails);
                  }
                }
              } else {
                LOGGER.error(
                    "Something went wrong while fetching delegate email Ids {}",
                    responseHandler.cause().getMessage());
                promise.fail("Something went wrong while fetching email Ids");
              }
            });
    return promise.future();
  }
}
