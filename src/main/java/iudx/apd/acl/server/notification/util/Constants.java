package iudx.apd.acl.server.notification.util;

public class Constants {

    public static final String WITHDRAW_REQUEST = "UPDATE request SET status = 'WITHDRAWN' WHERE _id = $1::uuid AND expiry_at > NOW() RETURNING _id";
    public static final String GET_REQUEST = "SELECT * FROM request WHERE _id = $1::uuid";

    public static final String GET_ACTIVE_CONSUMER_POLICY = "SELECT * FROM policy WHERE user_emailid = $1 AND item_id = $2::uuid AND item_type = $3 AND expiry_at > now() AND status = 'ACTIVE'";
    public static final String GET_VALID_NOTIFICATION = "SELECT * FROM request WHERE user_id = $1::uuid AND item_id = $2::uuid AND item_type = $3 AND status = 'PENDING';";
    public static final String GET_RESOURCE_INFO_QUERY = "SELECT * FROM resource_entity WHERE _id = $1::uuid ";
    public static final String CREATE_NOTIFICATION_QUERY = "INSERT INTO request" +
            "(_id, user_id, item_id, item_type, owner_id, status, expiry_at, constraints)" +
            " VALUES ($1::uuid, $2::uuid, $3::uuid, $4, $5::uuid, 'PENDING', NULL, NULL) RETURNING _id;";

    public static final String INSERT_RESOURCE_IN_DB_QUERY = "INSERT INTO resource_entity(_id, provider_id, resource_group_id) " +
            "VALUES ($1::uuid, $2::uuid, $3::uuid) RETURNING _id;";



}
