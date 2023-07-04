package iudx.apd.acl.server.validation;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.apd.acl.server.apiserver.util.RequestType;

import java.util.List;

public class ValidationHandlerFactory {

    public List<Validator> build(
            final Vertx vertx,
            final RequestType requestType,
            final MultiMap parameters,
            final MultiMap headers,
            final JsonObject body
            )
    {
        return null;
    }
}
