package iudx.apd.acl.server.policy;

import static iudx.apd.acl.server.apiserver.util.Constants.*;
import static iudx.apd.acl.server.policy.util.Constants.INSERT_ENTITY_TABLE;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class CatalogueClient implements CatalogueClientInterface {

  private static final Logger LOGGER = LogManager.getLogger(CatalogueClient.class);

  private final PgPool pool;
  private final WebClient client;
  private final String catHost;
  private final Integer catPort;
  private final String catServerRelationShipPath;

  public CatalogueClient(PgPool pool, JsonObject options) {

    this.pool = pool;
    WebClientOptions clientOptions =
        new WebClientOptions().setSsl(true).setVerifyHost(false).setTrustAll(true);

    this.client = WebClient.create(Vertx.vertx(), clientOptions);
    this.catHost = options.getString("catServerHost");
    this.catPort = options.getInteger("catServerPort");
    this.catServerRelationShipPath = options.getString("catServerRelationShipPath");
  }

  @Override
  public Future<Set<UUID>> fetchItem(Set<UUID> ids) {
    Promise<Set<UUID>> promise = Promise.promise();
    List<Tuple> batch = new ArrayList<>();
    HashSet<UUID> providerIdSet = new HashSet<>();

    String tempIds = "[" + Arrays.toString(ids.toArray()) + "]";
    client
        .get(catPort, catHost, catServerRelationShipPath)
        .addQueryParam("property", "[id]")
        .addQueryParam("value", tempIds)
        .addQueryParam("filter", "[id,resourceGroup,provider]")
        .send()
        .onFailure(
            ar -> {
              LOGGER.error("fetchItem error : " + ar.getCause());
              promise.fail(INTERNAL_ERROR);
            })
        .onSuccess(
            catSuccessResponse -> {
              JsonObject resultBody = catSuccessResponse.bodyAsJsonObject();
              if (resultBody.getString(TYPE).equals(CAT_SUCCESS_URN)) {
                List<JsonObject> resultJsonList =
                    resultBody.getJsonArray(RESULTS).stream()
                        .map(obj -> (JsonObject) obj)
                        .collect(Collectors.toList());

                if (resultJsonList.size() != ids.size()) {
                  LOGGER.error("FAIL TO GET RESULT FROM CAT");
                  promise.fail("Id/Ids does not present in CAT");
                  return;
                }
                for (JsonObject resultJson : resultJsonList) {
                  String id = resultJson.getString("id");
                  String provider = resultJson.getString("provider");
                  String resourceGroup =
                      resultJson.size() == 3 ? resultJson.getString("resourceGroup") : null;
                  batch.add(
                      Tuple.of(
                          UUID.fromString(id),
                          UUID.fromString(provider),
                          resourceGroup != null ? UUID.fromString(resourceGroup) : null));
                }

                pool.withConnection(
                    sqlConnection ->
                        sqlConnection
                            .preparedQuery(INSERT_ENTITY_TABLE)
                            .executeBatch(batch)
                            .onFailure(
                                dbHandler -> {
                                  LOGGER.error(
                                      "checkUserExist db fail " + dbHandler.getLocalizedMessage());
                                })
                            .onSuccess(
                                dbSuccessHandler -> {
                                  for (RowSet<Row> rowSet = dbSuccessHandler;
                                      rowSet != null;
                                      rowSet = rowSet.next()) {
                                    rowSet.forEach(
                                        row -> {
                                          providerIdSet.add(row.getUUID("provider_id"));
                                        });
                                  }
                                  promise.complete(providerIdSet);
                                }));
              } else {
                LOGGER.error("failed fetchItem: " + resultBody);
                promise.fail(resultBody.getString(TITLE));
              }
            });
    return promise.future();
  }
}
