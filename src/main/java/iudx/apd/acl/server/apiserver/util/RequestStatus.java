package iudx.apd.acl.server.apiserver.util;

import iudx.apd.acl.server.validation.exceptions.DxRuntimeException;

import java.util.stream.Stream;

import static iudx.apd.acl.server.common.ResponseUrn.RESOURCE_NOT_FOUND_URN;

public enum RequestStatus {
  REJECTED("rejected"),
  PENDING("pending"),
  GRANTED("granted"),
  WITHDRAWN("withdrawn");
  private final String status;

  RequestStatus(String value) {
    status = value;
  }

  public static RequestStatus fromString(String requestStatus) {
    return Stream.of(values())
        .filter(element -> element.getRequestStatus().equalsIgnoreCase(requestStatus))
        .findAny()
        .orElseThrow(() -> new DxRuntimeException(404, RESOURCE_NOT_FOUND_URN));
  }

  public String getRequestStatus() {
    return status;
  }
}
