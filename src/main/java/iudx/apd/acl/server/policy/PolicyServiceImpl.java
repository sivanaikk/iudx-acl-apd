package iudx.apd.acl.server.policy;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.pgclient.PgPool;
import jakarta.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

public class PolicyServiceImpl implements PolicyService{
    private static final Logger LOG = LoggerFactory.getLogger(PolicyServiceImpl.class);

    @Override
    public Future<JsonObject> createPolicy(JsonObject request) {
        return null;
    }

    @Override
    public Future<JsonObject> deletePolicy(JsonObject policyList) {
        //        Promise promise = Promise.promise();
//        String query = "";
//        PgPool pgPool = PgPool.pool();
//        pgPool.
//                withConnection(connection -> {
//                    connection.query(query).collecting()
//                })
        return null;
    }

    @Override
    public Future<JsonObject> getPolicy(JsonObject request) {
        return null;
    }
}
