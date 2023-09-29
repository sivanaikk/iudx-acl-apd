package iudx.apd.acl.server.apiserver.util;

import io.vertx.core.http.HttpMethod;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class Constants {

  // Header params
  public static final String HEADER_AUTHORIZATION = "Authorization";
  public static final String HEADER_TOKEN = "token";
  public static final String HEADER_HOST = "Host";
  public static final String HEADER_ACCEPT = "Accept";
  public static final String HEADER_CONTENT_LENGTH = "Content-Length";
  public static final String HEADER_CONTENT_TYPE = "Content-Type";
  public static final String HEADER_ORIGIN = "Origin";
  public static final String HEADER_REFERER = "Referer";
  public static final String HEADER_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
  public static final Set<String> ALLOWED_HEADERS =
      new HashSet<>(
          Arrays.asList(HEADER_AUTHORIZATION,
              HEADER_ACCEPT,
              HEADER_TOKEN,
              HEADER_CONTENT_LENGTH,
              HEADER_CONTENT_TYPE,
              HEADER_HOST,
              HEADER_ORIGIN,
              HEADER_REFERER,
              HEADER_ALLOW_ORIGIN));

  public static final Set<HttpMethod> ALLOWED_METHODS =
      new HashSet<>(
          Arrays.asList(
              HttpMethod.GET,
              HttpMethod.POST,
              HttpMethod.OPTIONS,
              HttpMethod.DELETE,
              HttpMethod.PATCH,
              HttpMethod.PUT));
  // request/response params
  public static final String ID = "id";
  public static final String CONTENT_TYPE = "content-type";
  public static final String AUTHORIZATION_KEY = "Authorization";
  public static final String CAT_SUCCESS_URN = "urn:dx:cat:Success";
  public static final String RESULTS = "results";
  public static final String TOTAL_HITS = "totalHits";
  public static final String APPLICATION_JSON = "application/json";
  public static final String ROUTE_STATIC_SPEC = "/apis/spec";
  public static final String ROUTE_DOC = "/apis";
  public static final String MIME_TEXT_HTML = "text/html";
  public static final String MSG_BAD_QUERY = "Bad query";
  public static final String TYPE = "type";
  public static final String TITLE = "title";
  public static final String STATUS_CODE = "statusCode";
  public static final String RESULT = "results";
  public static final String DETAIL = "detail";
  public static final String USER_ID = "userId";
  public static final String ROLE = "role";
  public static final String RESOURCE_SERVER_URL = "resourceServer";
  public static final String CLIENT_ID = "clientId";
  public static final String CLIENT_SECRET = "clientSecret";
  public static final String DELEGATE_EMAILS = "delegateEmails";
  public static final String EMAIL_OPTIONS = "emailOptions";
  public static final String API_ENDPOINT = "apiEndpoint";
  public static final String API_METHOD = "method";

  public static final String RELATIONSHIP_PATH = "/relationship";
  public static final String EPOCH_TIME = "epochTime";
  public static final String ISO_TIME = "isoTime";
  public static final String API = "api";
  public static final String RESPONSE_SIZE = "response_size";
  public static final String HTTP_METHOD = "httpMethod";

  public static final String USER = "user";
  public static final String BODY = "body";

  // endpoints
  public static final String POLICIES_API = "/policies";
  public static final String REQUEST_POLICY_API = "/policies/requests";
  public static final String REQUEST_EMAIL_IDS = "/delegations/emails";
  public static final String VERIFY_POLICY_API = "/verify";

  // validation
  public static final int POLICY_ID_MAX_LENGTH = 4000;
  public static final Pattern POLICY_ID_PATTERN =
      Pattern.compile("[0-9a-f]{8}\b-[0-9a-f]{4}\b-[0-9a-f]{4}\b-[0-9a-f]{4}\b-[0-9a-f]{12}$");

  //  //operation ids
  public static final String GET_POLICY_API = "get-auth-v1-policies";
  public static final String DELETE_POLICY_API = "delete-auth-v1-policies";
  public static final String CREATE_POLICY_API = "post-auth-v1-policies";
  public static final String GET_NOTIFICATIONS_API = "get-auth-v1-policies-requests";
  public static final String CREATE_NOTIFICATIONS_API = "post-auth-v1-policies-requests";
  public static final String UPDATE_NOTIFICATIONS_API = "put-auth-v1-policies-requests";
  public static final String DELETE_NOTIFICATIONS_API = "delete-auth-v1-policies-requests";
  public static final String VERIFY_API = "get-auth-v1-verify";

  public static final String RESOURCE_GROUP_TAG = "iudx:ResourceGroup";
  public static final String PROVIDER_TAG = "iudx:Provider";
  public static final String OWNER_ID = "ownerUserId";
  public static final String RESOURCE_TAG = "iudx:ResourceServer";
  public static final String KEY_RESOURCE_SERVER_URL = "resourceServerURL";
  public static final String RS_URL = "resourceServerRegURL";
  public static final String RESOURCE_SERVER_TAG = "iudx:ResourceServer";

  public static final String FIRST_NAME = "firstName";
  public static final String LAST_NAME = "lastName";
  public static final String EMAIL_ID = "emailId";
  public static final String RS_SERVER_URL = "resourceServerUrl";
  public static final String USER_ROLE = "userRole";


}
