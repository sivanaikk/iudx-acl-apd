package iudx.apd.acl.server.policy.util;

public class Constants {
  public static final int DB_RECONNECT_ATTEMPTS = 2;
  public static final long DB_RECONNECT_INTERVAL_MS = 10;
  public static final String CHECK_EXISTING_POLICY_SELECT_QUERY =
      "select _id,constraints from policy ";
  public static final String CHECK_EXISTING_POLICY_1 = "where item_id =$1::UUID";
  public static final String CHECK_EXISTING_POLICY =
      CHECK_EXISTING_POLICY_SELECT_QUERY
          + CHECK_EXISTING_POLICY_1
          + " and owner_id = $2::UUID "
          + " and status = $3::status_type and user_emailid = $4::text and expiry_at > now() ";
  public static final String ENTITY_TABLE_CHECK =
      "Select _id,provider_id,item_type from resource_entity where _id = ANY ($1::UUID[]);";
  public static final String INSERT_ENTITY_TABLE =
      "insert into resource_entity(_id,provider_id,resource_group_id,item_type,resource_server_url)"
        + " values ($1,$2,$3,$4,$5);";

  public static final String CREATE_POLICY_QUERY =
      "insert into policy (user_emailid, item_id, owner_id,expiry_at, constraints,status) "
          + "values ($1, $2, $3, $4, $5,'ACTIVE') returning *;";
  public static final String PROVIDER_QUERY_1 =
      "SELECT P._id AS \"policyId\", P.item_id AS \"itemId\",";
  public static final String PROVIDER_QUERY_2 = " P.user_emailid AS";
  public static final String PROVIDER_QUERY_3 = " \"consumerEmailId\", U.first_name AS ";
  public static final String PROVIDER_QUERY_4 = "\"consumerFirstName\", U.last_name AS ";
  public static final String PROVIDER_QUERY_5 = "\"consumerLastName\", U._id AS \"consumerId\",";
  public static final String PROVIDER_QUERY_6 =
      " P.status AS \"status\", P.expiry_at AS \"expiryAt\",";
  public static final String PROVIDER_QUERY_7 = " P.constraints AS \"constraints\"";
  public static final String PROVIDER_QUERY_9 = PROVIDER_QUERY_6 + PROVIDER_QUERY_7;
  public static final String PROVIDER_QUERY_8 =
      PROVIDER_QUERY_4 + PROVIDER_QUERY_5 + PROVIDER_QUERY_9;
  public static final String SELECT_QUERY_4_PROVIDER_QUERY =
      PROVIDER_QUERY_1 + PROVIDER_QUERY_2 + PROVIDER_QUERY_3 + PROVIDER_QUERY_8;
  public static final String GET_POLICY_4_PROVIDER_QUERY =
      SELECT_QUERY_4_PROVIDER_QUERY
          + " FROM policy AS P INNER JOIN user_table AS U ON P.user_emailid = U.email_id AND P.owner_id = $1;";
  public static final String CONSUMER_QUERY_1 =
      "SELECT P._id AS \"policyId\", P.item_id AS \"itemId\",";
  public static final String CONSUMER_QUERY_2 = " P.owner_id AS \"ownerId\",";
  public static final String CONSUMER_QUERY_3 = " U.first_name AS \"ownerFirstName\",";
  public static final String CONSUMER_QUERY_4 =
      "U.last_name AS \"ownerLastName\", U.email_id AS \"ownerEmailId\",";
  public static final String CONSUMER_QUERY_5 = " U._id AS \"ownerId\", P.status as \"status\",";
  public static final String CONSUMER_QUERY_6 =
      " P.expiry_at AS \"expiryAt\", P.constraints AS \"constraints\"";
  public static final String CONSUMER_QUERY_7 =
      CONSUMER_QUERY_4 + CONSUMER_QUERY_5 + CONSUMER_QUERY_6;
  public static final String SELECT_QUERY_4_CONSUMER_QUERY =
      CONSUMER_QUERY_1 + CONSUMER_QUERY_2 + CONSUMER_QUERY_3 + CONSUMER_QUERY_7;
  public static final String GET_POLICY_4_CONSUMER_QUERY =
      SELECT_QUERY_4_CONSUMER_QUERY
          + " FROM policy AS P INNER JOIN user_table AS U ON P.owner_id = U._id AND P.user_emailid = $1;";
  public static final String CHECK_IF_POLICY_PRESENT_QUERY = "SELECT * FROM policy WHERE _id = $1";
  public static final String DELETE_POLICY_QUERY_1 = "UPDATE policy SET status='DELETED'";
  public static final String DELETE_POLICY_QUERY_2 = " WHERE _id = $1::uuid AND expiry_at > NOW() ";
  public static final String DELETE_POLICY_QUERY =
      DELETE_POLICY_QUERY_1 + DELETE_POLICY_QUERY_2 + "RETURNING _id";
}
