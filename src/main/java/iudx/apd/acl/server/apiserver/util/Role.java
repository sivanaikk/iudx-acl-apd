package iudx.apd.acl.server.apiserver.util;

import static iudx.apd.acl.server.common.ResponseUrn.ROLE_NOT_FOUND;

import iudx.apd.acl.server.validation.exceptions.DxRuntimeException;

import java.util.stream.Stream;

public enum Role {
    PROVIDER("provider"),
    CONSUMER_DELEGATE("consumerDelegate"),
    PROVIDER_DELEGATE("providerDelegate"),
    CONSUMER("consumer");

    private final String role;

    Role(String value) {
        role = value;
    }

    public static Role fromString(String roleValue)
    {
        return Stream
                .of(values())
                .filter(element -> element.getRole().equalsIgnoreCase(roleValue))
                .findAny()
                .orElseThrow(() -> new DxRuntimeException(404, ROLE_NOT_FOUND));
    }

    public String getRole() {
        return role;
    }
}
