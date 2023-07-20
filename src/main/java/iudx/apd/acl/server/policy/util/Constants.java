package iudx.apd.acl.server.policy.util;

public class Constants {
  public static final int DB_RECONNECT_ATTEMPTS = 2;
  public static final long DB_RECONNECT_INTERVAL_MS = 10;
  public static final String DELETE_POLICY_QUERY =
          "UPDATE policy SET status='DELETED' WHERE _id = ANY ($1::uuid[])";
  private static final String COUNT_QUERY = "SELECT COUNT(*) FROM policy ";
  public static final String COUNT_OF_ACTIVE_POLICIES =
          COUNT_QUERY + "WHERE _id = ANY($1::uuid[]) AND owner_id=$2::uuid AND status='ACTIVE' AND expiry_at > NOW()";
  public static final String SELECT_QUERY_4_PROVIDER_QUERY =
          "SELECT P._id AS policy_id, P.owner_id AS owner_id, P.item_id AS item_id, P.item_type AS item_type, P.user_emailid AS consumer_email_id, P.owner_id AS owner_id, U.first_name AS consumer_first_name, U.last_name AS consumer_last_name, P.status AS status, P.expiry_at AS expiry_at, P.created_at AS created_at, P.updated_at AS updated_at";
  public static final String GET_POLICY_4_PROVIDER_QUERY =  SELECT_QUERY_4_PROVIDER_QUERY + " FROM policy AS P INNER JOIN user_table AS U ON P.owner_id = U._id AND P.owner_id = $1;";
  public static final String SELECT_QUERY_4_CONSUMER_QUERY = "SELECT P._id AS policy_id, P.user_emailid AS consumer_email_id, P.item_id AS item_id, P.item_type AS item_type, P.owner_id AS owner_id, U.first_name AS owner_first_name,U.last_name AS owner_last_name, U.email_id AS owner_email_id, P.status as status, P.expiry_at AS expiry_at, P.created_at AS created_at, P.updated_at AS updated_at";
  public static final String GET_POLICY_4_CONSUMER_QUERY = SELECT_QUERY_4_CONSUMER_QUERY + " FROM policy AS P INNER JOIN user_table AS U ON P.owner_id = U._id AND P.user_emailid = $1;";

}