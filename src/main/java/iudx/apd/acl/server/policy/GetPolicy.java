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
 * <b>PROVIDER</b> : Policy is created by the user who provides the resource, also known as owner <br>
 * <b>PROVIDER DELEGATE</b> : A user who acts on behalf of provider, having certain privileges of Provider <br>
 * <b>CONSUMER DELEGATE</b> : A user who acts on behalf of consumer, having certain privileges of Consumer <br>
 * GetPolicy class is used to fetch policy related information like policy id,
 * consumer details like consumer id, first name, last name, email, resource related information,
 * owner related information like id, first name, last name, email<br>
 * Since delegate acts on behalf of the consumer, provider, while fetching the policies, the delegate is either treated as a consumer or provider
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
      {
        JsonObject response =
                new JsonObject()
                        .put(TYPE, BAD_REQUEST.getValue())
                        .put(TITLE, BAD_REQUEST.getUrn())
                        .put(DETAIL, "Invalid role");
        return Future.failedFuture(response.encode());
      }
    }
  }

  /**
   * Fetch policy details of the provider based on the owner_id and
   * gets the information about consumer like consumer first name, last name, id based on the consumer email-Id
   * @param provider Object of User type
   * @param query Query to be executed
   * @return Policy details
   */
  public Future<JsonObject> getProviderPolicy(User provider, String query) {
    Promise<JsonObject> promise = Promise.promise();
    String owner_id = provider.getUserId();
    LOG.trace(provider.toString());
    Tuple tuple = Tuple.of(owner_id);
    JsonObject information =
            new JsonObject()
                    .put("owner_email_id", provider.getEmailId())
                    .put("owner_first_name", provider.getFirstName())
                    .put("owner_last_name", provider.getLastName());
    this.executeGetPolicy(tuple, query, information)
            .onComplete(
                    handler -> {
                      if (handler.succeeded()) {
                        LOG.info("success while executing GET provider policy");
                        promise.complete(handler.result());
                      } else {
                        LOG.info("success while executing GET provider policy");
                        promise.fail(handler.cause().getMessage());
                      }
                    });
    return promise.future();
  }

  /**
   * Fetches policies related to the consumer based on the consumer's email-Id <br>
   * Also gets information related to the owner of the policy like first name, last name, email-Id  based on the owner_id
   * @param consumer Object of User type
   * @param query Query to be executed
   * @return Policy details
   */
  public Future<JsonObject> getConsumerPolicy(User consumer, String query) {
    Promise<JsonObject> promise = Promise.promise();
    String emailId = consumer.getEmailId();
    LOG.trace(consumer.toString());
    Tuple tuple = Tuple.of(emailId);
    JsonObject information =
            new JsonObject()
                    .put("consumer_id", consumer.getUserId())
                    .put("consumer_first_name", consumer.getFirstName())
                    .put("consumer_last_name", consumer.getLastName());
    this.executeGetPolicy(tuple, query, information)
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
   * @param tuple Exchangeable values of query in the form of Vertx Tuple
   * @param query String query to be executed
   * @param information Information to be added in the response
   * @return
   */
  private Future<JsonObject> executeGetPolicy(Tuple tuple, String query, JsonObject information) {
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
                        if(handler.result().size() > 0) {
                          for (JsonObject jsonObject : handler.result()) {
                            jsonObject.mergeIn(information);
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
                          LOG.debug("Success response : {}", handler.result());
                        }else {
                          JsonObject response = new JsonObject()
                                  .put(TYPE, HttpStatusCode.NOT_FOUND.getValue())
                                  .put(TITLE, ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                                  .put(DETAIL, "Policy not found");
                          LOG.error("No policy found!");
                          promise.fail(response.encode());
                        }
                      } else {
                        JsonObject response = new JsonObject()
                                .put(TYPE, HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                                .put(TITLE, ResponseUrn.SUCCESS_URN.getMessage())
                                .put(DETAIL, FAILURE_MESSAGE + ", Failure while executing query");
                        promise.fail(response.encode());
                        LOG.error("Error response : {}", handler.cause().getMessage());
                      }
                    });
    return promise.future();
  }
}