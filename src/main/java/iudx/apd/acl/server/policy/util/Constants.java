package iudx.apd.acl.server.policy.util;

public class Constants {
  public static final int DB_RECONNECT_ATTEMPTS = 2;
  public static final long DB_RECONNECT_INTERVAL_MS = 10;
  public static final String CHECK_EXISTING_POLICY =
          "select _id,constraints from policy where item_id =$1::UUID and item_type = $2::_item_type and owner_id = $3::UUID "
                  + " and status = $4::status_type and user_emailid = $5::text and expiry_at > now() ";
  public static final String ENTITY_TABLE_CHECK =
      "Select _id,provider_id from resource_entity where _id = ANY ($1::UUID[]);";
  public static final String INSERT_ENTITY_TABLE =
    "insert into resource_entity(_id,provider_id,resource_group_id) values ($1,$2,$3);";

  public static final String CREATE_POLICY_QUERY =
          "insert into policy (user_emailid, item_id, owner_id,item_type,expiry_at, constraints,status) "
                  + "values ($1, $2, $3, $4, $5, $6,'ACTIVE') returning *;";
  public static final String SELECT_QUERY_4_PROVIDER_QUERY =
          "SELECT P._id AS \"policyId\", P.item_id AS \"itemId\", P.item_type AS \"itemType\", P.user_emailid AS \"consumerEmailId\", U.first_name AS \"consumerFirstName\", U.last_name AS \"consumerLastName\", U._id AS \"consumerId\", P.status AS \"status\", P.expiry_at AS \"expiryAt\", P.constraints AS \"constraints\"";
  public static final String GET_POLICY_4_PROVIDER_QUERY =  SELECT_QUERY_4_PROVIDER_QUERY + " FROM policy AS P INNER JOIN user_table AS U ON P.user_emailid = U.email_id AND P.owner_id = $1;";
  public static final String SELECT_QUERY_4_CONSUMER_QUERY = "SELECT P._id AS \"policyId\", P.item_id AS \"itemId\", P.item_type AS \"itemType\", P.owner_id AS \"ownerId\", U.first_name AS \"ownerFirstName\",U.last_name AS \"ownerLastName\", U.email_id AS \"ownerEmailId\", U._id AS \"ownerId\", P.status as \"status\", P.expiry_at AS \"expiryAt\", P.constraints AS \"constraints\"";
  public static final String GET_POLICY_4_CONSUMER_QUERY = SELECT_QUERY_4_CONSUMER_QUERY + " FROM policy AS P INNER JOIN user_table AS U ON P.owner_id = U._id AND P.user_emailid = $1;";
  public static final String CHECK_IF_POLICY_PRESENT_QUERY = "SELECT * FROM policy WHERE _id = $1";
  public static final String DELETE_POLICY_QUERY =
          "UPDATE policy SET status='DELETED' WHERE _id = $1::uuid AND expiry_at > NOW() " +
                  "RETURNING _id";
}

