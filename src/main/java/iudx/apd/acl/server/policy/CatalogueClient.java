package iudx.apd.acl.server.policy;

import static iudx.apd.acl.server.apiserver.util.Constants.*;
import static iudx.apd.acl.server.common.HttpStatusCode.INTERNAL_SERVER_ERROR;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import iudx.apd.acl.server.apiserver.util.ResourceObj;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CatalogueClient implements CatalogueClientInterface {

  private static final Logger LOGGER = LogManager.getLogger(CatalogueClient.class);
  private final WebClient client;
  private final String catHost;
  private final Integer catPort;
  private final String catServerRelationShipPath;

  public CatalogueClient(JsonObject options) {
    WebClientOptions clientOptions =
        new WebClientOptions().setSsl(true).setVerifyHost(false).setTrustAll(true);

    this.client = WebClient.create(Vertx.vertx(), clientOptions);
    this.catHost = options.getString("catServerHost");
    this.catPort = options.getInteger("catServerPort");
    this.catServerRelationShipPath = options.getString("catServerRelationShipPath");
  }

  @Override
  public Future<List<ResourceObj>> fetchItems(Set<UUID> ids) {
    Promise<List<ResourceObj>> promise = Promise.promise();
    List<ResourceObj> resourceObjList = new ArrayList<>();
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
              promise.fail(INTERNAL_SERVER_ERROR.getDescription());
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
                  UUID id = UUID.fromString(resultJson.getString("id"));
                  UUID provider = UUID.fromString(resultJson.getString("provider"));
                  UUID resourceGroup =
                      resultJson.size() == 3
                          ? UUID.fromString(resultJson.getString("resourceGroup"))
                          : null;
                  ResourceObj resourceObj = new ResourceObj(id, provider, resourceGroup);
                  resourceObjList.add(resourceObj);
                }
                promise.complete(resourceObjList);
              } else {
                promise.fail(resultBody.getString(TITLE));
              }
            });
    return promise.future();
  }
}
