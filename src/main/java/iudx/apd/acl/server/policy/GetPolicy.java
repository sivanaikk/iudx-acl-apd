package iudx.apd.acl.server.policy;

import static iudx.apd.acl.server.apiserver.util.Constants.*;
import static iudx.apd.acl.server.common.HttpStatusCode.BAD_REQUEST;
import static iudx.apd.acl.server.policy.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import iudx.apd.acl.server.apiserver.util.Role;
import iudx.apd.acl.server.apiserver.util.User;
import iudx.apd.acl.server.common.HttpStatusCode;
import iudx.apd.acl.server.common.ResponseUrn;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <b>CONSUMER</b> : A User for whom policy is made <br>
 * <b>PROVIDER</b> : Policy is created by the user who provides the resource, also known as owner
 * <br>
 * <b>PROVIDER DELEGATE</b> : A user who acts on behalf of provider, having certain privileges of
 * Provider <br>
 * <b>CONSUMER DELEGATE</b> : A user who acts on behalf of consumer, having certain privileges of
 * Consumer <br>
 * GetPolicy class is used to fetch policy related information like policy id, consumer details like
 * consumer id, first name, last name, email, resource related information, owner related
 * information like id, first name, last name, email<br>
 * Since delegate acts on behalf of the consumer, provider, while fetching the policies, the
 * delegate is either treated as a consumer or provider
 */
public class GetPolicy {
  private static final Logger LOG = LoggerFactory.getLogger(GetPolicy.class);
  private static final String FAILURE_MESSAGE = "Policy could not be fetched";
  private final PostgresService postgresService;
  private PgPool pool;

  public GetPolicy(PostgresService postgresService) {
    this.postgresService = postgresService;
  }

  public Future<JsonObject> initiateGetPolicy(User user) {
    Role role = user.getUserRole();
    switch (role) {
      case CONSUMER_DELEGATE:
      case CONSUMER:
        return getConsumerPolicy(user, GET_POLICY_4_CONSUMER_QUERY);
      case PROVIDER_DELEGATE:
      case PROVIDER:
        return getProviderPolicy(user, GET_POLICY_4_PROVIDER_QUERY);
      default:
        JsonObject response =
            new JsonObject()
                .put(TYPE, BAD_REQUEST.getValue())
                .put(TITLE, BAD_REQUEST.getUrn())
                .put(DETAIL, "Invalid role");
        return Future.failedFuture(response.encode());
    }
  }

  /**
   * Fetch policy details of the provider based on the ownerId and gets the information about
   * consumer like consumer first name, last name, id based on the consumer email-Id
   *
   * @param provider Object of User type
   * @param query Query to be executed
   * @return Policy details
   */
  public Future<JsonObject> getProviderPolicy(User provider, String query) {
    Promise<JsonObject> promise = Promise.promise();
    String ownerIdValue = provider.getUserId();
    LOG.trace(provider.toString());
    Tuple tuple = Tuple.of(ownerIdValue);
    JsonObject jsonObject =
        new JsonObject()
            .put("email", provider.getEmailId())
            .put(
                "name",
                new JsonObject()
                    .put("firstName", provider.getFirstName())
                    .put("lastName", provider.getLastName()))
            .put("id", provider.getUserId());
    JsonObject providerInfo = new JsonObject().put("provider", jsonObject);
    this.executeGetPolicy(tuple, query, providerInfo, Role.PROVIDER)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOG.info("success while executing GET provider policy");
                promise.complete(handler.result());
              } else {
                LOG.error("failure while executing GET provider policy");
                promise.fail(handler.cause().getMessage());
              }
            });
    return promise.future();
  }

  /**
   * Fetches policies related to the consumer based on the consumer's email-Id <br>
   * Also gets information related to the owner of the policy like first name, last name, email-Id
   * based on the ownerId
   *
   * @param consumer Object of User type
   * @param query Query to be executed
   * @return Policy details
   */
  public Future<JsonObject> getConsumerPolicy(User consumer, String query) {
    Promise<JsonObject> promise = Promise.promise();
    String emailId = consumer.getEmailId();
    LOG.trace(consumer.toString());
    Tuple tuple = Tuple.of(emailId);
    JsonObject jsonObject =
        new JsonObject()
            .put("email", consumer.getEmailId())
            .put(
                "name",
                new JsonObject()
                    .put("firstName", consumer.getFirstName())
                    .put("lastName", consumer.getLastName()))
            .put("id", consumer.getUserId());
    JsonObject consumerInfo = new JsonObject().put("consumer", jsonObject);

    this.executeGetPolicy(tuple, query, consumerInfo, Role.CONSUMER)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOG.info("success while executing GET consumer policy");
                promise.complete(handler.result());
              } else {
                LOG.error("Failure while executing GET consumer policy");
                promise.fail(handler.cause().getMessage());
              }
            });
    return promise.future();
  }

  /**
   * Executes the respective queries by using the vertx PgPool instance
   *
   * @param tuple Exchangeable values of query in the form of Vertx Tuple
   * @param query String query to be executed
   * @param information Information to be added in the response
   * @return the response as Future JsonObject type
   */
  private Future<JsonObject> executeGetPolicy(
      Tuple tuple, String query, JsonObject information, Role role) {
    Promise<JsonObject> promise = Promise.promise();
    Collector<Row, ?, List<JsonObject>> rowListCollector =
        Collectors.mapping(row -> row.toJson(), Collectors.toList());
    pool = postgresService.getPool();
    pool.withConnection(
            sqlConnection ->
                sqlConnection
                    .preparedQuery(query)
                    .collecting(rowListCollector)
                    .execute(tuple)
                    .map(rows -> rows.value()))
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                if (handler.result().size() > 0) {
                  for (JsonObject jsonObject : handler.result()) {
                    jsonObject.mergeIn(information).mergeIn(getInformation(jsonObject, role));
                  }
                  JsonObject result =
                      new JsonObject()
                          .put(TYPE, ResponseUrn.SUCCESS_URN.getUrn())
                          .put(TITLE, ResponseUrn.SUCCESS_URN.getMessage())
                          .put(RESULT, handler.result());

                  promise.complete(
                      new JsonObject()
                          .put(RESULT, result)
                          .put(STATUS_CODE, HttpStatusCode.SUCCESS.getValue()));
                } else {
                  JsonObject response =
                      new JsonObject()
                          .put(TYPE, HttpStatusCode.NOT_FOUND.getValue())
                          .put(TITLE, ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                          .put(DETAIL, "Policy not found");
                  LOG.error("No policy found!");
                  promise.fail(response.encode());
                }
              } else {
                JsonObject response =
                    new JsonObject()
                        .put(TYPE, HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                        .put(TITLE, ResponseUrn.DB_ERROR_URN.getUrn())
                        .put(DETAIL, FAILURE_MESSAGE + ", Failure while executing query");
                promise.fail(response.encode());
                LOG.error("Error response : {}", handler.cause().getMessage());
              }
            });
    return promise.future();
  }

  public JsonObject getInformation(JsonObject jsonObject, Role role) {
    if (role.equals(Role.CONSUMER)) {
      return getConsumerInformation(jsonObject);
    }
    return getProviderInformation(jsonObject);
  }

  public JsonObject getConsumerInformation(JsonObject jsonObject) {
    String ownerFirstName = jsonObject.getString("ownerFirstName");
    String ownerLastName = jsonObject.getString("ownerLastName");
    String ownerId = jsonObject.getString("ownerId");
    String ownerEmail = jsonObject.getString("ownerEmailId");
    JsonObject providerJson =
        new JsonObject()
            .put("email", ownerEmail)
            .put(
                "name",
                new JsonObject().put("firstName", ownerFirstName).put("lastName", ownerLastName))
            .put("id", ownerId);
    final JsonObject providerInfo = new JsonObject().put("provider", providerJson);
    jsonObject.remove("ownerFirstName");
    jsonObject.remove("ownerLastName");
    jsonObject.remove("ownerId");
    jsonObject.remove("ownerEmailId");
    return providerInfo;
  }

  public JsonObject getProviderInformation(JsonObject jsonObject) {
    String consumerFirstName = jsonObject.getString("consumerFirstName");
    String consumerLastName = jsonObject.getString("consumerLastName");
    String consumerId = jsonObject.getString("consumerId");
    String consumerEmail = jsonObject.getString("consumerEmailId");
    JsonObject consumerJson = new JsonObject().put("email", consumerEmail);
    // if the consumer is not present in the db then the response will only contain its email
    // address
    if (consumerFirstName != null) {
      consumerJson
          .put(
              "name",
              new JsonObject()
                  .put("firstName", consumerFirstName)
                  .put("lastName", consumerLastName))
          .put("id", consumerId);
    }
    final JsonObject consumerInfo = new JsonObject().put("consumer", consumerJson);
    jsonObject.remove("consumerFirstName");
    jsonObject.remove("consumerLastName");
    jsonObject.remove("consumerId");
    jsonObject.remove("consumerEmailId");
    return consumerInfo;
  }
}
