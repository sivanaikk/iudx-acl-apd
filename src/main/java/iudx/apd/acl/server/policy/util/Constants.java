package iudx.apd.acl.server.policy.util;

public class Constants {
  public static final int DB_RECONNECT_ATTEMPTS = 2;
  public static final long DB_RECONNECT_INTERVAL_MS = 10;

  public static final String CHECK_EXISTING_POLICY =
      "select _id from policy where item_id =$1::UUID and item_type = $2::_item_type and owner_id = $3::UUID "
          + " and status = $4::status_type and user_emailid = $5::text and expiry_at > now() ";
  public static final String ENTITY_TABLE_CHECK =
      "Select _id,provider_id from resource_entity where _id = ANY ($1::UUID[]);";

  public static final String CREATE_POLICY_QUERY =
      "insert into policy (user_emailid, item_id, owner_id,item_type,expiry_at, constraints,status) "
          + "values ($1, $2, $3, $4, $5, $6,'ACTIVE') returning *;";
}
