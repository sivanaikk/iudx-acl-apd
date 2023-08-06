package iudx.apd.acl.server.policy;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public interface CatalogueClientInterface {

  Future<Set<UUID>> fetchItem(Set<UUID> id);

}
