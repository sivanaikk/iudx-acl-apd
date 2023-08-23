package iudx.apd.acl.server.notification.util;

public class Constants {

    public static final String WITHDRAW_REQUEST = "UPDATE request SET status = 'WITHDRAWN' WHERE _id = $1::uuid AND expiry_at > NOW() OR expiry_at IS NULL RETURNING _id";
    public static final String GET_REQUEST = "SELECT * FROM request WHERE _id = $1::uuid";

    public static final String GET_ACTIVE_CONSUMER_POLICY = "SELECT * FROM policy WHERE user_emailid = $1 AND item_id = $2::uuid AND item_type = $3 AND expiry_at > now() AND status = 'ACTIVE'";
    public static final String GET_VALID_NOTIFICATION = "SELECT * FROM request WHERE user_id = $1::uuid AND item_id = $2::uuid AND item_type = $3 AND status = 'PENDING';";
    public static final String CREATE_NOTIFICATION_QUERY = "INSERT INTO request" +
            "(_id, user_id, item_id, item_type, owner_id, status, expiry_at, constraints)" +
            " VALUES ($1::uuid, $2::uuid, $3::uuid, $4, $5::uuid, 'PENDING', NULL, NULL) RETURNING _id;";


    public static final String REJECT_NOTIFICATION =  "UPDATE request SET status = 'REJECTED' WHERE _id = $1::uuid AND expiry_at > NOW() RETURNING _id";
    public static final String GET_CONSUMER_EMAIL_QUERY = "SELECT email_id FROM user_table WHERE _id = $1::uuid;";
    public static final String GET_EXISTING_POLICY_QUERY = "SELECT * FROM policy WHERE owner_id = $1::uuid AND status = 'ACTIVE' AND item_id = $2::uuid AND item_type = $3 AND expiry_at > now() AND user_emailid = $4";
    public static final String OWNERSHIP_CHECK_QUERY = "SELECT * FROM resource_entity WHERE _id = $1::uuid AND provider_id = $2::uuid";
    public static final String CREATE_POLICY_QUERY =   "INSERT INTO policy(_id, user_emailid, item_id, item_type, owner_id, status, expiry_at, constraints) VALUES ($1, $2, $3, $4, $5, $6, $7, $8) RETURNING _id;";
    public static final String INSERT_IN_APPROVED_ACCESS_REQUESTS_QUERY = "INSERT INTO approved_access_requests(_id, request_id, policy_id) VALUES ($1, $2, $3) RETURNING _id";
    public static final String APPROVE_REQUEST_QUERY = "UPDATE request SET status = 'GRANTED', expiry_at = $1, constraints = $2 WHERE _id = $3 AND owner_id = $4 RETURNING _id";
    public static final String INSERT_PROVIDER_INFO_QUERY = "INSERT INTO user_table (_id, email_id, first_name, last_name) VALUES ($1::uuid, $2, $3, $4) ON CONFLICT (_id) DO NOTHING;";
    public static final String INSERT_RESOURCE_INFO_QUERY = "INSERT INTO resource_entity (_id, provider_id, resource_group_id) VALUES ($1::uuid, $2::uuid, $3::uuid) ON CONFLICT (_id) DO NOTHING;";



    public static final String GET_CONSUMER_NOTIFICATION_QUERY = "SELECT R._id AS \"requestId\", R.item_id AS \"itemId\", " +
   " R.item_type AS \"itemType\", R.status AS \"status\", R.expiry_at AS \"expiryAt\", "+
    "R.constraints AS \"constraints\", R.user_id AS \"consumerId\", "+
    "R.owner_id AS \"ownerId\", U.first_name AS \"ownerFirstName\", "+
    "U.last_name AS \"ownerLastName\", U.email_id AS \"ownerEmailId\" "+
    "FROM request AS R "+
    "INNER JOIN user_table AS U "+
    "ON U._id = R.owner_id "+
    "WHERE R.user_id=$1::uuid; ";

    public static final String GET_PROVIDER_NOTIFICATION_QUERY = "SELECT R._id AS \"requestId\", R.item_id AS \"itemId\", " +
            "R.item_type AS \"itemType\", R.status AS \"status\", R.expiry_at AS \"expiryAt\", " +
            "R.constraints AS \"constraints\", R.user_id AS \"consumerId\", " +
            "R.owner_id AS \"ownerId\", U.first_name AS \"consumerFirstName\", " +
            "U.last_name AS \"consumerLastName\", U.email_id AS \"consumerEmailId\" " +
            "FROM request AS R " +
            "INNER JOIN user_table AS U " +
            "ON U._id = R.user_id " +
            "WHERE R.owner_id=$1::uuid; ";

}

