This document contains the installation and configuration 
processes of the external modules of each Verticle in Data Exchange ACL-APD Server.
</br>
</br>
The server connects with various external dependencies namely :
- `PostgreSQL` : Used to store data related to
    - users
    - resources
    - policies
    - access requests
    - approved access requests
    - metering information
- `RabbitMQ` : Used to 
  - publish auditing related data to auditing server

The DX ACL-APD Server also connects with various DX dependencies like
- Authentication Authorization and Accounting (AAA) Server : used to download certificate for token decoding
- Catalogue Server: used to fetch the list of resource and provider related information
- Auditing Server: used to store metering related data

## Setting up PostgreSQL for DX ACL-APD Server
- To setup PostgreSQL refer the docker files available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/postgres)

**Note** : PostgreSQL database should be configured with a RBAC user having CRUD privileges
In order to connect to the appropriate Postgres database, required information like databaseIP, databasePort, etc., could be
updated in commonConfig module available at [config-example.json](example-config/config-dev.json)

```
 "commonConfig": {
    "databaseIP": "localhost",
    "databasePort": <database-port-number>,
    "databaseSchema" : "<database-schema>",
    "databaseName": "<database-name>",
    "databaseUserName": "<database-user-name>",
    "databasePassword": "<database-password>",
    "poolSize": <pool-size>
  }

```

#### Schemas for PostgreSQL tables in DX ACL-APD Server
- Refer Flyway schema [here](src/main/resources/db/migration)

1. To store consumer, provider related information
``` 
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

```
2. To store resource / resource group related information

```
CREATE TABLE IF NOT EXISTS resource_entity
(
   _id uuid NOT NULL,
   provider_id uuid NOT NULL,
   resource_group_id uuid,
   item_type _item_type NOT NULL,
   resource_server_url varchar NOT NULL,
   created_at timestamp without time zone NOT NULL,
   updated_at timestamp without time zone NOT NULL,
   CONSTRAINT resource_pk PRIMARY KEY (_id),
   CONSTRAINT provider_id_fk FOREIGN KEY(provider_id) REFERENCES user_table(_id)
);


```

3. To store policy related information

```
CREATE TABLE IF NOT EXISTS policy
(
   _id uuid DEFAULT uuid_generate_v4 () NOT NULL,
   user_emailid varchar NOT NULL,
   item_id uuid NOT NULL,
   owner_id uuid NOT NULL,
   status status_type NOT NULL,
   expiry_at timestamp without time zone NOT NULL,
   created_at timestamp without time zone NOT NULL,
   updated_at timestamp without time zone NOT NULL,
   constraints json NOT NULL,
   CONSTRAINT policy_pk PRIMARY KEY (_id),
   CONSTRAINT owner_id_fk FOREIGN KEY(owner_id) REFERENCES user_table(_id),
   CONSTRAINT item_id_fk FOREIGN KEY(item_id) REFERENCES resource_entity(_id)
);
```

4. To store information related to notifications or access requests

```
CREATE TABLE IF NOT EXISTS request
(
   _id uuid DEFAULT uuid_generate_v4 () NOT NULL,
   user_id uuid NOT NULL,
   item_id uuid NOT NULL,
   owner_id uuid NOT NULL,
   status access_request_status_type NOT NULL default 'PENDING',
   expiry_at timestamp without time zone,
   created_at timestamp without time zone NOT NULL,
   updated_at timestamp without time zone NOT NULL,
   constraints json,
   CONSTRAINT request_pk PRIMARY KEY (_id),
   CONSTRAINT user_id_fk FOREIGN KEY(user_id) REFERENCES user_table(_id),
   CONSTRAINT owner_id_fk FOREIGN KEY(owner_id) REFERENCES user_table(_id),
   CONSTRAINT item_id_fk FOREIGN KEY(item_id) REFERENCES resource_entity(_id)
);
```

5. To store information related to approved notifications or access requests

```
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

```

## Setting up RabbitMQ for DX ACL-APD Server
- To setup RMQ refer the docker files available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/databroker) 
In order to connect to the appropriate RabbitMQ instance, required information such as dataBrokerIP, dataBrokerPort etc.
should be updated in AuditingVerticle module available at [config-example.json](example-config/config-dev.json)

**AuditingVerticle**
``` 
{
      "id": "iudx.apd.acl.server.auditing.AuditingVerticle",
      "isWorkerVerticle": false,
      "verticleInstances": <num-of-verticle-instances>,
      "dataBrokerIP": "<databroker-ip>",
      "dataBrokerPort": <databroker-port-number>,
      "dataBrokerVhost": "<vHost-name>",
      "dataBrokerUserName": "<username-for-rmq>",
      "dataBrokerPassword": "<password-for-rmq>",
      "dataBrokerManagementPort":  <management-port-number>,
      "connectionTimeout": <time-in-milliseconds>,
      "requestedHeartbeat": <time-in-seconds>,
      "handshakeTimeout": <time-in-milliseconds>,
      "requestedChannelMax": <num-of-max-channels>,
      "networkRecoveryInterval": <time-in-milliseconds>,
      "automaticRecoveryEnabled": "true",
      "prodVhost": "<prod-vHost-name>",
      "internalVhost": "<internal-vHost-name>",
      "externalVhost": "<external-vHost-name>"
    }
```

## Connecting with DX Catalogue Server
In order to connect to the DX Catalogue server, required information such as 
catServerHost, catServerPort, dxCatalogueBasePath etc., should be updated in commonConfig module
available at [config-example.json](example-config/config-dev.json)

```
  "commonConfig": {
    "dxApiBasePath": "<base-path-for-acl-apd-server>",
    "dxCatalogueBasePath": "<catalogue-base-path>",
    "catServerHost": "<catalogue-server-host>",
    "catServerPort": <catalogue-server-port-number>
  }
```

## Connecting with DX AAA Server
In order to connect with DX Authentication server, required information like authServerHost, authPort, dxAuthBasePath  etc., should 
be updated in commonConfig, AuthenticationVerticle module available at [config-example.json](example-config/config-dev.json)

```
  "commonConfig": {
    "dxAuthBasePath": "<auth-server-base-path>",
    "authPort": <auth-server-port>,
    "authHost": "<auth-server-host>",
    "clientId": "<acl-apd-trustee-client-id>",
    "clientSecret": "<acl-apd-trustee-client-secret>"
  }

```
**AuthenticationVerticle**
``` 
 {
      "id": "iudx.apd.acl.server.authentication.AuthenticationVerticle",
      "isWorkerVerticle": false,
      "verticleInstances": <num-of-verticle-instances>,
      "issuer": "<issuer-url>",
      "apdURL": "<acl-apd-url>",
      "authServerHost": "<auth-server-url>",
      "jwtIgnoreExpiry": <true|false>
    }
```

## Setting up Policy, Notification Verticle

In order to connect with Vert.x SMTP email service [link](https://vertx.io/docs/vertx-mail-client/java/) in Notification 
module, Policy verticle, information like default expiry days, emailHostName, emailPassword etc,m should
be updated in PolicyVerticle, NotificationVerticle and ApiServerVerticle modules available at
[config-example.json](example-config/config-dev.json)

**ApiServerVerticle**
``` 
{
      "id": "iudx.apd.acl.server.apiserver.ApiServerVerticle",
      "isWorkerVerticle": false,
      "verticleInstances": <num-of-verticle-instances>,
      "ssl": true,
      "httpPort": <http-port-number>
    }
```

**PolicyVerticle**
``` 
{
      "id": "iudx.apd.acl.server.policy.PolicyVerticle",
      "isWorkerVerticle": false,
      "verticleInstances": <num-of-verticle-instances>,
      "defaultExpiryDays": <num-of-days-to-expiry-a-policy>,
      "domain": "<domain-name>"
    }
```

**NotificationVerticle**
``` 
{
      "id": "iudx.apd.acl.server.notification.NotificationVerticle",
      "isWorkerVerticle": false,
      "verticleInstances": <num-of-verticle-instances>,
      "domain": "<domain-name>",
      "emailHostName": "<email-host-name>",
      "emailPort": <email-port>,
      "emailUserName": "<email-user-name>",
      "emailPassword": "<email-password>",
      "emailSender": "<email-sender>",
      "emailSupport": [
        "dummy@email.com"
      ],
      "publisherPanelUrl": "<panel-url>",
      "notifyByEmail": <true|false>,
      "senderName": "<email-sender-name>"
    }
```