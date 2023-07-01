package iudx.apd.acl.server.common;

import java.util.stream.Stream;

public enum ResponseUrn {
    SUCCESS_URN("urn:dx:rs:success", "Success"),
    INVALID_PARAM_URN("urn:dx:rs:invalidParameter", "Invalid parameter passed"),
    INVALID_PARAM_VALUE_URN("urn:dx:rs:invalidParameterValue", "Invalid parameter value passed"),

    INVALID_ATTR_VALUE_URN("urn:dx:rs:invalidAttributeValue", "Invalid attribute value"),

    INVALID_ATTR_PARAM_URN("urn:dx:rs:invalidAttributeParam", "Invalid attribute param"),

    INVALID_OPERATION_URN("urn:dx:rs:invalidOperation", "Invalid operation"),
    UNAUTHORIZED_ENDPOINT_URN(
            "urn:dx:rs:unauthorizedEndpoint", "Access to endpoint is not available"),
    UNAUTHORIZED_RESOURCE_URN(
            "urn,dx:rs:unauthorizedResource", "Access to resource is not available"),
    EXPIRED_TOKEN_URN("urn:dx:rs:expiredAuthorizationToken", "Token has expired"),
    MISSING_TOKEN_URN("urn:dx:rs:missingAuthorizationToken", "Token needed and not present"),
    INVALID_TOKEN_URN("urn:dx:rs:invalidAuthorizationToken", "Token is invalid"),
    RESOURCE_NOT_FOUND_URN("urn:dx:rs:resourceNotFound", "Document of given id does not exist"),
    LIMIT_EXCEED_URN(
            "urn:dx:rs:requestLimitExceeded", "Operation exceeds the default value of limit"),
    INVALID_ID_VALUE_URN("urn:dx:rs:invalidIdValue", "Invalid id"),
    INVALID_PAYLOAD_FORMAT_URN(
            "urn:dx:rs:invalidPayloadFormat", "Invalid json format in post request [schema mismatch]"),
    BAD_REQUEST_URN("urn:dx:rs:badRequest", "bad request parameter"),
    INVALID_HEADER_VALUE_URN("urn:dx:rs:invalidHeaderValue", "Invalid header value"),
    NOT_YET_IMPLEMENTED_URN("urn:dx:rs:general", "urn not yet implemented in backend verticle."),

    DB_ERROR_URN("urn:dx:rs:DatabaseError", "Database error");

    ResponseUrn(String urn,String message){
        this.urn = urn;
        this.message = message;
    }
    private final String urn;
    private final String message;
    public static ResponseUrn fromCode(final String urn)
    {
        return Stream.of(values())
                .filter(v -> v.urn.equalsIgnoreCase(urn))
                .findAny()
                .orElse(NOT_YET_IMPLEMENTED_URN);
    }

    public String getUrn() {
        return urn;
    }
    public String getMessage() {
        return message;
    }

    public String toString() {
        return "[" + urn + " : " + message + " ]";
    }
}
