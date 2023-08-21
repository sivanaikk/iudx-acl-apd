package iudx.apd.acl.server.notification.util;

public class Constants {

    public static final String WITHDRAW_REQUEST = "UPDATE request SET status = 'WITHDRAWN' WHERE _id = $1::uuid AND expiry_at > NOW() RETURNING _id";
    public static final String GET_REQUEST = "SELECT * FROM request WHERE _id = $1::uuid";
    public static final String REJECT_NOTIFICATION =  "UPDATE request SET status = 'REJECTED' WHERE _id = $1::uuid AND expiry_at > NOW() RETURNING _id";
    public static final String GET_CONSUMER_EMAIL_QUERY = "SELECT email_id FROM user_table WHERE _id = $1::uuid;";
    public static final String GET_EXISTING_POLICY_QUERY = "SELECT * FROM policy WHERE owner_id = $1::uuid AND status = 'ACTIVE' AND item_id = $2::uuid AND item_type = $3 AND expiry_at > now() AND user_emailid = $4";
    public static final String OWNERSHIP_CHECK_QUERY = "SELECT * FROM resource_entity WHERE _id = $1::uuid AND provider_id = $2::uuid";
    public static final String CREATE_POLICY_QUERY =   "INSERT INTO policy(_id, user_emailid, item_id, item_type, owner_id, status, expiry_at, constraints) VALUES ($1, $2, $3, $4, $5, $6, $7, $8) RETURNING _id;";
    public static final String INSERT_IN_APPROVED_ACCESS_REQUESTS_QUERY = "INSERT INTO approved_access_requests(_id, request_id, policy_id) VALUES ($1, $2, $3) RETURNING _id";
    public static final String APPROVE_REQUEST_QUERY = "UPDATE request SET status = 'GRANTED', expiry_at = $1, constraints = $2 WHERE _id = $3 AND owner_id = $4 RETURNING _id";


}