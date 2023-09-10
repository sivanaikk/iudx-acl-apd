package iudx.apd.acl.server.notification.util;

public class Constants {

  public static final String WITHDRAWN_REQUEST_1 = "UPDATE request SET status = 'WITHDRAWN'";
  public static final String WITHDRAWN_REQUEST_2 = " WHERE _id = $1::uuid AND expiry_at > NOW()";
  public static final String WITHDRAWN_REQUEST_3 = " OR expiry_at IS NULL RETURNING _id";
  public static final String WITHDRAW_REQUEST =
          WITHDRAWN_REQUEST_1 + WITHDRAWN_REQUEST_2 + WITHDRAWN_REQUEST_3;
  public static final String GET_REQUEST = "SELECT * FROM request WHERE _id = $1::uuid";

  public static final String GET_ACTIVE_CONSUMER_POLICY_1 = "SELECT * FROM policy WHERE user_emailid = $1";
  public static final String GET_ACTIVE_CONSUMER_POLICY_2 = " AND item_id = $2::uuid";
  public static final String GET_ACTIVE_CONSUMER_POLICY_3 = " AND expiry_at > now() AND status = 'ACTIVE'";
  public static final String GET_ACTIVE_CONSUMER_POLICY =
          GET_ACTIVE_CONSUMER_POLICY_1 + GET_ACTIVE_CONSUMER_POLICY_2 + GET_ACTIVE_CONSUMER_POLICY_3;
  public static final String GET_VALID_NOTIFICATION =
          "SELECT * FROM request WHERE user_id = $1::uuid AND item_id = $2::uuid AND status = 'PENDING';";
  public static final String CREATE_NOTIFICATION_QUERY =
          "INSERT INTO request"
                  + "(_id, user_id, item_id,owner_id, status, expiry_at, constraints)"
                  + " VALUES ($1::uuid, $2::uuid, $3::uuid,$4::uuid, 'PENDING', NULL, NULL) RETURNING _id;";

  public static final String REJECT_NOTIFICATION =
      "UPDATE request SET status='REJECTED' WHERE _id=$1::uuid AND expiry_at>NOW() OR expiry_at IS NULL RETURNING _id";
  public static final String GET_CONSUMER_EMAIL_QUERY =
          "SELECT email_id FROM user_table WHERE _id = $1::uuid;";
  public static final String GET_EXISTING_POLICY_QUERY_1 = "SELECT * FROM policy WHERE owner_id = $1::uuid";
  public static final String GET_EXISTING_POLICY_QUERY_3 = " AND status = 'ACTIVE' AND item_id = $2::uuid";
  public static final String GET_EXISTING_POLICY_QUERY_7 = " AND user_emailid = $3";
  public static final String GET_EXISTING_POLICY_QUERY_6 = " AND expiry_at > now()";
  public static final String GET_EXISTING_POLICY_QUERY_4 = GET_EXISTING_POLICY_QUERY_6 + GET_EXISTING_POLICY_QUERY_7;
  public static final String GET_EXISTING_POLICY_QUERY =
          GET_EXISTING_POLICY_QUERY_1 + GET_EXISTING_POLICY_QUERY_3 + GET_EXISTING_POLICY_QUERY_4;
  public static final String OWNERSHIP_CHECK_QUERY =
          "SELECT * FROM resource_entity WHERE _id = $1::uuid AND provider_id = $2::uuid";
  public static final String CREATE_POLICY_QUERY_2 = " item_id, owner_id, status, expiry_at, constraints)";
  public static final String CREATE_POLICY_QUERY_1 = "INSERT INTO policy(_id, user_emailid," + CREATE_POLICY_QUERY_2;
  public static final String CREATE_POLICY_QUERY =
          CREATE_POLICY_QUERY_1 + " VALUES ($1, $2, $3, $4, $5, $6,$7) RETURNING _id;";
  public static final String INSERT_IN_APPROVED_ACCESS_REQUESTS_QUERY =
          "INSERT INTO approved_access_requests(_id, request_id, policy_id) VALUES ($1, $2, $3) RETURNING _id";
  public static final String APPROVE_REQUEST_QUERY_1 = "UPDATE request SET status = 'GRANTED', expiry_at = $1, ";
  public static final String APPROVE_REQUEST_QUERY =
          APPROVE_REQUEST_QUERY_1 + "constraints = $2 WHERE _id = $3 AND owner_id = $4 RETURNING _id";
  public static final String INSERT_USER_INFO_QUERY_1 = "INSERT INTO user_table (_id, email_id, first_name, last_name)";
  public static final String INSERT_USER_INFO_QUERY =
          INSERT_USER_INFO_QUERY_1 + " VALUES ($1::uuid, $2, $3, $4) ON CONFLICT (_id) DO NOTHING;";
  public static final String INSERT_RESOURCE_INFO_QUERY_3 = "(_id, provider_id, resource_group_id,"
          + "resource_server_url,item_type)";
  public static final String INSERT_RESOURCE_INFO_QUERY_2 = "INSERT INTO resource_entity ";
  public static final String INSERT_RESOURCE_INFO_QUERY_1 = INSERT_RESOURCE_INFO_QUERY_2 + INSERT_RESOURCE_INFO_QUERY_3;
  public static final String INSERT_RESOURCE_INFO_QUERY =
          INSERT_RESOURCE_INFO_QUERY_1 + " VALUES ($1::uuid, $2::uuid, $3::uuid,$4,$5) ON CONFLICT (_id) DO NOTHING;";

  public static final String GET_CONSUMER_NOTIFICATION_1 = "SELECT R._id AS \"requestId\", R.item_id AS \"itemId\", ";
  public static final String GET_CONSUMER_NOTIFICATION_13 = " AS \"status\", R.expiry_at AS \"expiryAt\", ";
  public static final String GET_CONSUMER_NOTIFICATION_14 = " R.status";
  public static final String GET_CONSUMER_NOTIFICATION_2 = GET_CONSUMER_NOTIFICATION_14 + GET_CONSUMER_NOTIFICATION_13;
  public static final String GET_CONSUMER_NOTIFICATION_15 = "R.constraints AS \"constraints\",";
  public static final String GET_CONSUMER_NOTIFICATION_16 = " R.user_id AS \"consumerId\", ";
  public static final String GET_CONSUMER_NOTIFICATION_3 = GET_CONSUMER_NOTIFICATION_15 + GET_CONSUMER_NOTIFICATION_16;
  public static final String GET_CONSUMER_NOTIFICATION_17 = "R.owner_id AS \"ownerId\",";
  public static final String GET_CONSUMER_NOTIFICATION_18 = " U.first_name AS \"ownerFirstName\", ";
  public static final String GET_CONSUMER_NOTIFICATION_4 = GET_CONSUMER_NOTIFICATION_17 + GET_CONSUMER_NOTIFICATION_18;
  public static final String GET_CONSUMER_NOTIFICATION_19 = "U.last_name AS \"ownerLastName\",";
  public static final String GET_CONSUMER_NOTIFICATION_20 = " U.email_id AS \"ownerEmailId\" ";
  public static final String GET_CONSUMER_NOTIFICATION_5 = GET_CONSUMER_NOTIFICATION_19 + GET_CONSUMER_NOTIFICATION_20;
  public static final String GET_CONSUMER_NOTIFICATION_6 = "FROM request AS R ";
  public static final String GET_CONSUMER_NOTIFICATION_7 = "INNER JOIN user_table AS U ";
  public static final String GET_CONSUMER_NOTIFICATION_8 = "ON U._id = R.owner_id ";
  public static final String GET_CONSUMER_NOTIFICATION_9 = "WHERE R.user_id=$1::uuid; ";
  public static final String GET_CONSUMER_NOTIFICATION_10 = GET_CONSUMER_NOTIFICATION_7
          + GET_CONSUMER_NOTIFICATION_8
          + GET_CONSUMER_NOTIFICATION_9;
  public static final String GET_CONSUMER_NOTIFICATION_11 = GET_CONSUMER_NOTIFICATION_4
          + GET_CONSUMER_NOTIFICATION_5
          + GET_CONSUMER_NOTIFICATION_6;
  public static final String GET_CONSUMER_NOTIFICATION_12 = GET_CONSUMER_NOTIFICATION_11
          + GET_CONSUMER_NOTIFICATION_10;
  public static final String GET_CONSUMER_NOTIFICATION_QUERY =
          GET_CONSUMER_NOTIFICATION_1 + GET_CONSUMER_NOTIFICATION_2 + GET_CONSUMER_NOTIFICATION_3
                  + GET_CONSUMER_NOTIFICATION_12;
  public static final String GET_PROVIDER_NOTIFICATION_1 = "SELECT R._id AS \"requestId\", R.item_id AS \"itemId\", ";
  public static final String GET_PROVIDER_NOTIFICATION_3 = " R.status AS \"status\", R.expiry_at AS \"expiryAt\", ";
  public static final String GET_PROVIDER_NOTIFICATION_5 = " R.user_id AS \"consumerId\", ";
  public static final String GET_PROVIDER_NOTIFICATION_6 = "R.constraints AS \"constraints\",";
  public static final String GET_PROVIDER_NOTIFICATION_4 = GET_PROVIDER_NOTIFICATION_6 + GET_PROVIDER_NOTIFICATION_5;
  public static final String GET_PROVIDER_NOTIFICATION_7 = "R.owner_id AS \"ownerId\", ";
  public static final String GET_PROVIDER_NOTIFICATION_8 = "U.first_name AS \"consumerFirstName\", ";
  public static final String GET_PROVIDER_NOTIFICATION_9 = "U.last_name AS \"consumerLastName\",";
  public static final String GET_PROVIDER_NOTIFICATION_10 = " U.email_id AS \"consumerEmailId\" ";
  public static final String GET_PROVIDER_NOTIFICATION_11 = "FROM request AS R ";
  public static final String GET_PROVIDER_NOTIFICATION_12 = "INNER JOIN user_table AS U ";
  public static final String GET_PROVIDER_NOTIFICATION_13 = "ON U._id = R.user_id ";
  public static final String GET_PROVIDER_NOTIFICATION_14 = "WHERE R.owner_id=$1::uuid; ";
  public static final String GET_PROVIDER_NOTIFICATION_QUERY =
          GET_PROVIDER_NOTIFICATION_1
                  + GET_PROVIDER_NOTIFICATION_3
                  + GET_PROVIDER_NOTIFICATION_4
                  + GET_PROVIDER_NOTIFICATION_7 + GET_PROVIDER_NOTIFICATION_8
                  + GET_PROVIDER_NOTIFICATION_9 + GET_PROVIDER_NOTIFICATION_10
                  + GET_PROVIDER_NOTIFICATION_11
                  + GET_PROVIDER_NOTIFICATION_12
                  + GET_PROVIDER_NOTIFICATION_13
                  + GET_PROVIDER_NOTIFICATION_14;

  public static final String HTML_EMAIL_BODY_1 = "<!DOCTYPE html> <html> <head> <title>Page Title</title>";
  public static final String HTML_EMAIL_BODY_2 = "</head> <body> <p>Hello!</p> <p>A consumer with details - <br>";
  public static final String HTML_EMAIL_BODY_4 = "First name : ${CONSUMER_FIRST_NAME}, <br>";
  public static final String HTML_EMAIL_BODY_5 = "Last name :  ${CONSUMER_LAST_NAME}, <br>";
  public static final String HTML_EMAIL_BODY_6 = "Email ID : ${CONSUMER_EMAIL_ID}, <br>";
  public static final String HTML_EMAIL_BODY_7 = "has requested access to one of your datasets.";
  public static final String HTML_EMAIL_BODY_9 = " to approve/reject this request.</p>";
  public static final String HTML_EMAIL_BODY_8 = " Please visit ${PUBLISHER_PANEL_URL}" + HTML_EMAIL_BODY_9;
  public static final String HTML_EMAIL_BODY_10 = "<footer> <p>Regards,<br> ${SENDER'S_NAME}</p>";
  public static final String HTML_EMAIL_BODY_11 = "</footer> </body> </html>";
  public static final String HTML_EMAIL_BODY =
                  HTML_EMAIL_BODY_1
                          + HTML_EMAIL_BODY_2
                          + HTML_EMAIL_BODY_4
                          + HTML_EMAIL_BODY_5
                          + HTML_EMAIL_BODY_6
                          + HTML_EMAIL_BODY_7
                          + HTML_EMAIL_BODY_8
                          + HTML_EMAIL_BODY_10
                          + HTML_EMAIL_BODY_11;


}
