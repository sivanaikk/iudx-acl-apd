package iudx.apd.acl.server.policy;

public class Constants {
  public static final String DELETE_POLICY_QUERY =
      "UPDATE $0 SET status='DELETED' WHERE _id = ANY ($1::uuid[])";

  private static final String COUNT_QUERY = "SELECT COUNT(*) FROM $0 ";
  public static final String COUNT_OF_ACTIVE_POLICIES =
      COUNT_QUERY + "WHERE _id = ANY($1::uuid[]) AND owner_id='$2'::uuid AND status='ACTIVE' AND expiry_at > NOW()";
  public static final String POLICY_TABLE_NAME = "policy";

  public static final int DB_RECONNECT_ATTEMPTS = 2;
  public static final long DB_RECONNECT_INTERVAL_MS = 10;

}
