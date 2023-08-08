package iudx.apd.acl.server.notification.util;

public class Constants {

    public static final String WITHDRAW_REQUEST = "UPDATE request SET status = 'WITHDRAWN' WHERE _id = $1::uuid AND expiry_at > NOW() RETURNING _id";
    public static final String GET_REQUEST = "SELECT * FROM request WHERE _id = $1::uuid";
}
