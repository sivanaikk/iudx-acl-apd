package iudx.apd.acl.server.policy.utility;

public class Constants {
    public static final String DELETE_POLICY_QUERY = "UPDATE $0 SET status='DELETED' WHERE _id IN ($1)";
    public static final String AMOUNT_OF_ACTIVE_POLICIES_OF_OWNER_QUERY = "SELECT COUNT(*) FROM $0 WHERE _id IN ($1) AND owner_id='$2' AND status='ACTIVE' AND expiry_at > NOW()";
    public static final String POLICY_TABLE_NAME = "policy";


}
