package iudx.apd.acl.server.apiserver.util;

public enum RequestType {
    POLICY("policy"),
    NOTIFICATION("notification");

    private final String request;
    RequestType(String request) {
        this.request = request;
    }

    public String getRequest() {
        return this.request;
    }
}
