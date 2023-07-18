package iudx.apd.acl.server.policy.util;

public class Constants {
  public static final int DB_RECONNECT_ATTEMPTS = 2;
  public static final long DB_RECONNECT_INTERVAL_MS = 10;

  public static final String CHECK_EXISTING_APD_POLICY =
      "select _id from policy where item_id =$1::UUID and item_type = $2::_item_type and owner_id = $3::UUID "
          + " and status = $4::status_type and user_emailid = $5::text and expiry_at > now() ";

  public static final String CHECK_OWNERSHIP =
      "select provider_id from resource_entity where _id = ANY ($1::UUID[]);";
  public static final String CHECK_RESOURCE_ENTITY_TABLE =
      "Select _id from resource_entity where _id = ANY ($1::UUID[]);";

  public static final String CREATE_POLICY_QUERY =
      "insert into policy (user_emailid, item_id, owner_id,item_type,expiry_at, constraints,status) "
          + "values ($1, $2, $3, $4, $5, $6,'ACTIVE')";

  public static final String RETRIEVE_POLICY_CREATED =
      "select _id,user_emailid,item_id,owner_id,expiry_at from policy where user_emailid= ANY ($1::TEXT[]) and item_id= ANY "
          + "($2::UUID[]) and owner_id=$3::UUID;";
}
