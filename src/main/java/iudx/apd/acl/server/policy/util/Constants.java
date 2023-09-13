package iudx.apd.acl.server.policy.util;

public class Constants {
  public static final int DB_RECONNECT_ATTEMPTS = 2;
  public static final long DB_RECONNECT_INTERVAL_MS = 10;
  public static final String CHECK_EXISTING_POLICY =
      "select _id,constraints from policy "
          + "where item_id =$1::UUID and owner_id = $2::UUID  and status = $3::status_type"
          + " and user_emailid = $4::text and expiry_at > now()";

  public static final String ENTITY_TABLE_CHECK =
      "Select _id,provider_id,item_type,resource_server_url from resource_entity where _id = ANY ($1::UUID[]);";
  public static final String INSERT_ENTITY_TABLE =
      "insert into resource_entity(_id,provider_id,resource_group_id,item_type,resource_server_url)"
          + " values ($1,$2,$3,$4,$5);";

  public static final String CREATE_POLICY_QUERY =
      "insert into policy (user_emailid, item_id, owner_id,expiry_at, constraints,status) "
          + "values ($1, $2, $3, $4, $5,'ACTIVE') returning *;";
  public static final String GET_POLICY_4_PROVIDER_QUERY =
      "SELECT P._id AS \"policyId\", P.item_id AS \"itemId\",\n"
          + "RE.item_type AS \"itemType\",\n"
          + "RE.resource_server_url AS \"resourceServerUrl\","
          + "P.user_emailid AS \"consumerEmailId\",\n"
          + "U.first_name AS \"consumerFirstName\",\n"
          + "U.last_name AS \"consumerLastName\", U._id AS \"consumerId\",\n"
          + "P.status AS \"status\", P.expiry_at AS \"expiryAt\",\n"
          + "P.constraints AS \"constraints\" FROM policy AS P \n"
          + "LEFT JOIN user_table AS U\n"
          + "ON P.user_emailid = U.email_id \n"
          + "INNER JOIN resource_entity AS RE\n"
          + "ON RE._id = P.item_id\n"
          + "AND P.owner_id = $1::uuid "
          + "AND RE.resource_server_url = $2;";
  public static final String GET_POLICY_4_CONSUMER_QUERY =
      "SELECT P._id AS \"policyId\", P.item_id AS \"itemId\",\n"
          + "RE.item_type AS \"itemType\",\n"
          + "RE.resource_server_url AS \"resourceServerUrl\",\n"
          + "P.owner_id AS \"ownerId\", U.first_name AS \"ownerFirstName\",\n"
          + "U.last_name AS \"ownerLastName\", U.email_id AS \"ownerEmailId\",\n"
          + "U._id AS \"ownerId\", P.status as \"status\", P.expiry_at AS \"expiryAt\",\n"
          + "P.constraints AS \"constraints\" \n"
          + "FROM policy AS P \n"
          + "INNER JOIN user_table AS U\n"
          + "ON P.owner_id = U._id \n"
          + "INNER JOIN resource_entity AS RE\n"
          + "ON RE._id = P.item_id\n"
          + "AND P.user_emailid = $1 "
          + "AND RE.resource_server_url = $2;";
  public static final String DELETE_POLICY_QUERY =
      "UPDATE policy SET status='DELETED' "
          + "WHERE _id = $1::uuid AND expiry_at > NOW() RETURNING _id";
  public static final String CHECK_IF_POLICY_PRESENT_QUERY =
      "SELECT p.owner_id, p.status, r.resource_server_url"
          + " FROM policy p"
          + " INNER JOIN resource_entity r ON p.item_id = r._id"
          + " WHERE p._id = $1;";
}
