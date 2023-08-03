package iudx.apd.acl.server.notification.util;

public class Constants {

    public static final String WITHDRAW_REQUEST = "UPDATE request SET status = 'WITHDRAWN' WHERE _id = ANY ($1::uuid[]) RETURNING _id";
    private static final String SELECT_QUERY_4_WITHDRAWN_REQUEST = "SELECT R._id AS \"requestId\", R.item_id AS \"itemId\", R.item_type AS \"itemType\", R.user_id AS \"consumerId\", R.owner_id AS \"ownerId\", U.email_id AS \"ownerEmailId\", U.first_name AS \"ownerFirstName\", U.last_name AS \"ownerLastName\", R.status AS status, R.constraints AS \"constraints\"";
    public static final String GET_WITHDRAWN_REQUEST = SELECT_QUERY_4_WITHDRAWN_REQUEST + "FROM request AS R INNER JOIN user_table AS U ON R.owner_id = U._id AND R._id = ANY ($1::uuid[])";
    public static final String OWNERSHIP_CHECK_QUERY = "SELECT _id AS \"requestId\" FROM request WHERE _id = ANY ($1::uuid[]) AND user_id <> $2::uuid";
    public static final String GET_WITHDRAWN_REQUESTS_QUERY = "SELECT _id AS \"requestId\" FROM request WHERE status ='WITHDRAWN' AND _id = ANY ($1::uuid[]) AND user_id = $2::uuid";
    public static final String GET_ID_NOT_FOUND_QUERY = "SELECT V._id AS \"requestId\" FROM (VALUES  (($1)::uuid)) AS V(_id) WHERE NOT EXISTS (SELECT R._id AS requestId FROM request AS R WHERE R._id = V._id);";

}
