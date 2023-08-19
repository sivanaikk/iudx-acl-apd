package iudx.apd.acl.server.notification.util;

public class Constants {

    public static final String WITHDRAW_REQUEST = "UPDATE request SET status = 'WITHDRAWN' WHERE _id = $1::uuid AND expiry_at > NOW() RETURNING _id";
    public static final String GET_REQUEST = "SELECT * FROM request WHERE _id = $1::uuid";


    public static final String GET_CONSUMER_NOTIFICATION_QUERY = "SELECT R._id AS \"requestId\", R.item_id AS \"itemId\", " +
   " R.item_type AS \"itemType\", R.status AS \"status\", R.expiry_at AS \"expiryAt\", "+
    "R.constraints AS \"constraints\", R.user_id AS \"consumerId\", "+
    "R.owner_id AS \"ownerId\", U.first_name AS \"ownerFirstName\", "+
    "U.last_name AS \"ownerLastName\", U.email_id AS \"ownerEmailId\" "+
    "FROM request AS R "+
    "INNER JOIN user_table AS U "+
    "ON U._id = R.user_id "+
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
