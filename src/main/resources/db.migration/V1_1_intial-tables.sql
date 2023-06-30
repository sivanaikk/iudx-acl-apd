-- we create the following extension to use gen_random_uuid
-- it is created on the default public schema so that all
-- schemas in the database may use it (if required).
create EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;

alter SCHEMA ${flyway:defaultSchema} OWNER TO ${flyway:user};

-- constants enum
-- item types
create type _item_type as ENUM
(
   'RESOURCE',
   'RESOURCE_GROUP'
);

-- status type
create type status_type as ENUM
(
   'ACTIVE',
   'DELETED'
);

-- request status type
create type access_request_status_type as ENUM
(
   'GRANTED',
   'PENDING',
   'REJECTED',
   'WITHDRAWN'
);

---
-- User Table
---
CREATE TABLE IF NOT EXISTS user_table
(
   _id uuid NOT NULL,
   email_id varchar NOT NULL,
   first_name varchar NOT NULL,
   last_name varchar NOT NULL,
   created_at timestamp without time zone NOT NULL,
   updated_at timestamp without time zone NOT NULL,
   CONSTRAINT user_pk PRIMARY KEY (_id)
);

ALTER TABLE user_table OWNER TO ${flyway:user};

---item name like surat
---
-- Resource/Resource Group Table
---
CREATE TABLE IF NOT EXISTS resource_entity
(
   _id uuid NOT NULL,
   cat_id varchar NOT NULL,
   provider_id uuid NOT NULL,
   resource_group_id uuid,
   created_at timestamp without time zone NOT NULL,
   updated_at timestamp without time zone NOT NULL,
   CONSTRAINT resource_pk PRIMARY KEY (_id),
   CONSTRAINT provider_id_fk FOREIGN KEY(provider_id) REFERENCES user_table(_id)
);

ALTER TABLE resource_entity OWNER TO ${flyway:user};

---
-- Policy Table
---
CREATE TABLE IF NOT EXISTS policy
(
   _id uuid DEFAULT uuid_generate_v4 () NOT NULL,
   user_id uuid NOT NULL,
   item_id uuid NOT NULL,
   item_type _item_type NOT NULL,
   owner_id uuid NOT NULL,
   status status_type NOT NULL,
   expiry_at timestamp without time zone NOT NULL,
   created_at timestamp without time zone NOT NULL,
   updated_at timestamp without time zone NOT NULL,
   constraints json NOT NULL,
   CONSTRAINT policy_pk PRIMARY KEY (_id),
   CONSTRAINT user_id_fk FOREIGN KEY(user_id) REFERENCES user_table(_id),
   CONSTRAINT owner_id_fk FOREIGN KEY(owner_id) REFERENCES user_table(_id),
   CONSTRAINT item_id_fk FOREIGN KEY(item_id) REFERENCES resource_entity(_id)
);

ALTER TABLE policy OWNER TO ${flyway:user};
---
-- Request Table
---
CREATE TABLE IF NOT EXISTS request
(
   _id uuid DEFAULT uuid_generate_v4 () NOT NULL,
   user_id uuid NOT NULL,
   item_id uuid NOT NULL,
   item_type _item_type NOT NULL,
   owner_id uuid NOT NULL,
   status access_request_status_type NOT NULL default 'PENDING',
   expiry_at timestamp without time zone NOT NULL,
   created_at timestamp without time zone NOT NULL,
   updated_at timestamp without time zone NOT NULL,
   constraints json NOT NULL,
   CONSTRAINT request_pk PRIMARY KEY (_id),
   CONSTRAINT user_id_fk FOREIGN KEY(user_id) REFERENCES user_table(_id),
   CONSTRAINT owner_id_fk FOREIGN KEY(owner_id) REFERENCES user_table(_id),
   CONSTRAINT item_id_fk FOREIGN KEY(item_id) REFERENCES resource_entity(_id)
);
ALTER TABLE request OWNER TO ${flyway:user};

---
-- Approved Table
---
CREATE TABLE IF NOT EXISTS approved_access_requests
(
  _id uuid DEFAULT uuid_generate_v4 () NOT NULL,
  request_id uuid NOT NULL,
  policy_id uuid NOT NULL,
  created_at timestamp without time zone NOT NULL,
  updated_at timestamp without time zone NOT NULL,
  CONSTRAINT approved_access_requests_pk PRIMARY KEY (_id),
  CONSTRAINT request_id_fk FOREIGN KEY(request_id) REFERENCES request(_id),
  CONSTRAINT policy_id_fk FOREIGN KEY(policy_id) REFERENCES policy(_id)
);
ALTER TABLE approved_access_requests OWNER to ${flyway:user};

---
-- Functions for audit[created,updated] on table/column
---

-- updated_at column function
create
or replace
   function update_modified () RETURNS trigger AS $$
begin NEW.updated_at = now ();
RETURN NEW;
END;
$$ language 'plpgsql';

-- created_at column function
create
or replace
   function update_created () RETURNS trigger AS $$
begin NEW.created_at = now ();
RETURN NEW;
END;
$$ language 'plpgsql';

---
-- Triggers
---

-- resource table
create trigger update_ua_created before insert on resource_entity for each row EXECUTE procedure update_created ();
create trigger update_ua_modified before insert or update on resource_entity for each row EXECUTE procedure update_modified ();

-- policy table
create trigger update_ua_created before insert on policy for each row EXECUTE procedure update_created ();
create trigger update_ua_modified before insert or update on policy for each row EXECUTE procedure update_modified ();

-- request table
create trigger update_ua_created before insert on request for each row EXECUTE procedure update_created ();
create trigger update_ua_modified before insert or update on request for each row EXECUTE procedure update_modified ();

-- approved_access_requests table
create trigger update_ua_created before insert on approved_access_requests for each row EXECUTE procedure update_created ();
create trigger update_ua_modified before insert or update on approved_access_requests for each row EXECUTE procedure update_modified ();
 ---
 -- grants
 ---

 GRANT USAGE ON SCHEMA ${flyway:defaultSchema} TO ${aclApdUser};

 GRANT SELECT,INSERT ON TABLE resource_entity TO ${aclApdUser};
 GRANT SELECT,INSERT,UPDATE ON TABLE policy TO ${aclApdUser};
 GRANT SELECT,INSERT,UPDATE ON TABLE request TO ${aclApdUser};
 GRANT SELECT,INSERT ON TABLE user_table TO ${aclApdUser};
 GRANT SELECT,INSERT ON TABLE approved_access_requests TO ${aclApdUser};
