package iudx.apd.acl.server.notification.util;

public class Constants {
  public static final String WITHDRAW_REQUEST = "UPDATE request SET status = 'WITHDRAWN' WHERE _id = $1::uuid "
          + " AND (expiry_at > NOW() OR expiry_at IS NULL) RETURNING _id";
  public static final String GET_REQUEST = "SELECT * FROM request WHERE _id = $1::uuid";
  public static final String GET_REQUEST_WITH_ITEM_TYPE =
      "SELECT RE.resource_server_url, RE.item_type, R.* "
          + " FROM request AS R "
          + " INNER JOIN resource_entity AS RE "
          + " ON R.item_id = RE._id "
          + " WHERE R._id = $1::uuid;";

  public static final String GET_ACTIVE_CONSUMER_POLICY = "SELECT * FROM policy WHERE user_emailid = $1 "
          + "AND item_id = $2::uuid AND expiry_at > now() AND status = 'ACTIVE';";

  public static final String GET_VALID_NOTIFICATION =
          "SELECT * FROM request WHERE user_id = $1::uuid AND item_id = $2::uuid AND status = 'PENDING';";
  public static final String CREATE_NOTIFICATION_QUERY =
          "INSERT INTO request"
                  + "(user_id, item_id,owner_id, status, expiry_at, constraints)"
                  + " VALUES ($1::uuid, $2::uuid,$3::uuid, 'PENDING', NULL, NULL) RETURNING _id;";

  public static final String REJECT_NOTIFICATION =
          "UPDATE request SET status='REJECTED' WHERE "
          +
          "_id=$1::uuid AND expiry_at>NOW() OR expiry_at IS NULL RETURNING _id";

  public static final String GET_CONSUMER_EMAIL_QUERY =
          "SELECT email_id FROM user_table WHERE _id = $1::uuid;";
  public static final String GET_EXISTING_POLICY_QUERY = "SELECT * FROM policy WHERE owner_id = $1::uuid "
          + "AND status = 'ACTIVE' AND item_id = $2::uuid AND expiry_at > now() AND user_emailid = $3";

  public static final String OWNERSHIP_CHECK_QUERY =
          "SELECT * FROM resource_entity WHERE _id = $1::uuid AND provider_id = $2::uuid";
  public static final String CREATE_POLICY_QUERY = "INSERT INTO policy"
          + "(user_emailid, item_id, owner_id, status, expiry_at, constraints)"
          + " VALUES ($1, $2, $3, $4, $5,$6) RETURNING _id;";
  public static final String INSERT_IN_APPROVED_ACCESS_REQUESTS_QUERY =
          "INSERT INTO approved_access_requests(request_id, policy_id) VALUES ($1, $2) RETURNING _id";
  public static final String APPROVE_REQUEST_QUERY = "UPDATE request SET status = 'GRANTED', expiry_at = $1,"
          + "constraints = $2 WHERE _id = $3 AND owner_id = $4 RETURNING _id";
  public static final String INSERT_USER_INFO_QUERY = "INSERT INTO user_table (_id, email_id, first_name, last_name)"
          + " VALUES ($1::uuid, $2, $3, $4) ON CONFLICT (_id) DO NOTHING;";

  public static final String INSERT_RESOURCE_INFO_QUERY = "INSERT INTO resource_entity "
          + "(_id, provider_id, resource_group_id,resource_server_url,item_type)"
          + " VALUES ($1::uuid, $2::uuid, $3::uuid,$4,$5) ON CONFLICT (_id) DO NOTHING;";
  public static final String GET_PROVIDER_NOTIFICATION_QUERY =
      "SELECT R._id AS \"requestId\",\n"
          + "R.item_id AS \"itemId\", \n"
          + "RE.item_type AS \"itemType\",\n"
          + "RE.resource_server_url AS \"resourceServerUrl\",\n"
          + "R.status AS \"status\", R.expiry_at AS \"expiryAt\", \n"
          + "R.constraints AS \"constraints\", R.user_id AS \"consumerId\",\n"
          + "R.owner_id AS \"ownerId\", U.first_name AS \"consumerFirstName\", \n"
          + "U.last_name AS \"consumerLastName\", U.email_id AS \"consumerEmailId\" \n"
          + "FROM request AS R \n"
          + "INNER JOIN user_table AS U \n"
          + "ON U._id = R.user_id \n"
          + "INNER JOIN resource_entity AS RE\n"
          + "ON RE._id = R.item_id\n"
          + "WHERE R.owner_id=$1::uuid "
          + "AND RE.resource_server_url = $2;";
  public static final String HTML_EMAIL_BODY =
      "<!DOCTYPE html>\n"
          + "<html>\n"
          + "    <head> </head>\n"
          + "    <body>\n"
          + "        <p>Hello,</p>\n"
          + "        <p>\n"
          + "            ${CONSUMER_FIRST_NAME} ${CONSUMER_LAST_NAME}, with email ${CONSUMER_EMAIL_ID} has"
          + " requested access to one of your datasets. Please visit ${PUBLISHER_PANEL_URL} "
          + "to approve/reject this request.\n"
          + "        </p>\n"
          + "        <footer>\n"
          + "            <p>\n"
          + "                Regards,<br />\n"
          + "                ${SENDER'S_NAME}\n"
          + "            </p>\n"
          + "        </footer>\n"
          + "    </body>\n"
          + "</html>\n";
  public static String GET_CONSUMER_NOTIFICATION_QUERY =
      "SELECT R._id AS \"requestId\", R.item_id AS \"itemId\",  \n"
          + "RE.item_type AS \"itemType\",\n"
          + "RE.resource_server_url AS \"resourceServerUrl\",\n"
          + "R.status AS \"status\", R.expiry_at AS \"expiryAt\", \n"
          + "R.constraints AS \"constraints\",\n"
          + "R.user_id AS \"consumerId\", R.owner_id AS \"ownerId\",\n"
          + "U.first_name AS \"ownerFirstName\", \n"
          + "U.last_name AS \"ownerLastName\", U.email_id AS \"ownerEmailId\" \n"
          + "FROM request AS R \n"
          + "INNER JOIN user_table AS U \n"
          + "ON U._id = R.owner_id \n"
          + "INNER JOIN resource_entity AS RE\n"
          + "ON RE._id = R.item_id\n"
          + "WHERE R.user_id=$1::uuid "
          + "AND RE.resource_server_url = $2;";


}
