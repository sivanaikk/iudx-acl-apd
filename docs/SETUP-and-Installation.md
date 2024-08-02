![IUDX](./iudx.png)

# Setup and Installation Guide
This document contains the installation and configuration
processes of the external modules of each Verticle in Data Exchange ACL-APD Server.

## Configuration
In order to setup PostgreSQL, RabbitMQ, Email Service, connect with DX Catalogue Server, DX AAA Server, appropriate information
could be updated in configs
### Modules

#### Api Server Verticle

| Key Name          | Value Datatype | Value Example | Description                                                                                          |
|:------------------|:--------------:|:--------------|:-----------------------------------------------------------------------------------------------------|
| isWorkerVerticle  |    boolean     | false         | To check if worker verticle needs to be deployed for blocking operations                             |
| verticleInstances |    integer     | 1             | Number of instances required for verticles                                                           |
| ssl               |    boolean     | true          | To create a encrypted link between the browser and server to keep the information private and secure |
| httpPort          |    integer     | 8443          | Port for running the instance DX ACL-APD Server                                                      |

### Other Configuration

| Key Name                         | Value Datatype | Value Example                        | Description                                                                                                                  |
|:---------------------------------|:--------------:|:-------------------------------------|:-----------------------------------------------------------------------------------------------------------------------------|
| version                          |     Float      | 1.0                                  | config version                                                                                                               |
| zookeepers                       |     Array      | zookeeper                            | zookeeper configuration to deploy clustered vert.x instance                                                                  |
| clusterId                        |     String     | iudx-acl-apd-cluster                 | cluster id to deploy clustered vert.x instance                                                                               |
| commonConfig.dxApiBasePath       |     String     | /dx/apd/acl/v1                       | API base path for DX ACL-APD. Reference : [link](https://swagger.io/docs/specification/2-0/api-host-and-base-path/)          |
| commonConfig.dxCatalogueBasePath |     String     | /iudx/cat/v1                         | API base path for DX Catalogue server. Reference : [link](https://swagger.io/docs/specification/2-0/api-host-and-base-path/) |
| commonConfig.dxAuthBasePath      |     String     | /auth/v1                             | API base path for DX AAA server. Reference : [link](https://swagger.io/docs/specification/2-0/api-host-and-base-path/)       |
| commonConfig.catServerHost       |     String     | api.cat-test.iudx.io                 | Host name of DX Catalogue server for fetching the information of resources, resource groups                                  |
| commonConfig.catServerPort       |    integer     | 443                                  | Port number to access HTTPS APIs of Catalogue Server                                                                         |
| commonConfig.authHost            |     String     | authvertx.iudx.io                    | Host name of Auth Server                                                                                                     |
| commonConfig.authPort            |    integer     | 443                                  | Port number to access HTTPS APIs of Auth server Default                                                                      |
| commonConfig.databaseIP          |     String     | localhost                            | Database IP address                                                                                                          |
| commonConfig.databasePort        |    integer     | 5433                                 | Port number                                                                                                                  |
| commonConfig.databaseSchema      |     String     | acl_apd_schema                       | Database schema                                                                                                              |
| commonConfig.databaseName        |     String     | acl_apd                              | Database name                                                                                                                |
| commonConfig.databaseUserName    |     String     | dbUserName                           | Database user name                                                                                                           |
| commonConfig.databasePassword    |     String     | dbPassword                           | Password for DB                                                                                                              |
| commonConfig.clientId            |      UUID      | b806432c-e510-4233-a4ff-316af67b6df8 | APD trustee client ID                                                                                                        |
| commonConfig.clientSecret        |      UUID      | 87d05695-1911-44f6-a1bc-d04422df6209 | APD trustee client secret                                                                                                    |
| commonConfig.poolSize            |    integer     | 25                                   | Pool size for postgres client                                                                                                |
| commonConfig.apdURL              |     String     | acl-apd.iudx.io                      | DX ACL-APD URL to validate audience field                                                                                    |
| host                             |     String     | acl-apd.iudx.io                      | Host URL                                                                                                                     |

### Policy Verticle

| Key Name          | Value Datatype | Value Example | Description                                                              |
|:------------------|:--------------:|:--------------|:-------------------------------------------------------------------------|
| isWorkerVerticle  |    boolean     | false         | To check if worker verticle needs to be deployed for blocking operations |
| verticleInstances |    integer     | 1             | Number of instances required for verticles                               |
| defaultExpiryDays |    integer     | 12            | Default number of days to expire a policy                                |
| domain            |     String     | iudx.io       | Domain for which DX ACL-APD Server is deployed                           |

### Notification Verticle

| Key Name          | Value Datatype | Value Example                   | Description                                                               |
|:------------------|:--------------:|:--------------------------------|:--------------------------------------------------------------------------|
| isWorkerVerticle  |    boolean     | false                           | To check if worker verticle needs to be deployed for blocking operations  |
| verticleInstances |    integer     | 1                               | Number of instances required for verticles                                |
| domain            |     String     | iudx.io                         | Domain for which DX ACL-APD Server is deployed                            |
| emailHostName     |     String     | email-smtp-some-service.com     | Host for sending an email whenever an notification is created by consumer |
| emailPort         |    integer     | 2587                            | Email port number for SMTP Service                                        |
| emailUserName     |     String     | emailUserName                   | Username                                                                  |
| emailPassword     |     String     | emailPassword                   | Password                                                                  |
| emailSender       |     String     | email@sender                    | Sender of the email (from)                                                |
| emailSupport      |     Array      | dummy@email.org, test@email.net | An array of emails added as support email                                 |
| publisherPanelUrl |     String     | https://something.com           | Provider panel in DX                                                      |
| notifyByEmail     |    boolean     | true                            | Checks if email notification is needed                                    |
| senderName        |    String      | IUDX                            | Name of the sender                                                        |


### Authentication Verticle

| Key Name          | Value Datatype | Value Example     | Description                                                              |
|:------------------|:--------------:|:------------------|:-------------------------------------------------------------------------|
| isWorkerVerticle  |    boolean     | false             | To check if worker verticle needs to be deployed for blocking operations |
| verticleInstances |    integer     | 1                 | Number of instances required for verticles                               |
| issuer            |     String     | cos.iudx.io       | To authenticate the issuer in the token                                  |
| authServerHost    |     String     | authvertx.iudx.io | DX AAA Server host name                                                  |
| jwtIgnoreExpiry   |    boolean     | false             | Set to true while using the server locally to allow expired tokens       |

### Auditing Verticle

| Key Name                 | Value Datatype | Value Example | Description                                                                                            |
|:-------------------------|:--------------:|:--------------|:-------------------------------------------------------------------------------------------------------|
| isWorkerVerticle         |    boolean     | false         | To check if worker verticle needs to be deployed for blocking operations                               |
| verticleInstances        |    integer     | 1             | Number of instances required for verticles                                                             |
| dataBrokerIP             |     String     | localhost     | RMQ IP address                                                                                         |
| dataBrokerPort           |    integer     | 24568         | RMQ port number                                                                                        |
| dataBrokerVhost          |     String     | vHostName     | Vhost being used to send Audit information Default                                                     |
| dataBrokerUserName       |     String     | rmqUserName   | User name for RMQ                                                                                      |
| dataBrokerPassword       |     String     | rmqPassword   | Password for RMQ                                                                                       |
| dataBrokerManagementPort |    integer     | 28041         | Port on which RMQ Management plugin is running                                                         |
| connectionTimeout        |    integer     | 6000          | Setting connection timeout as part of RabbitMQ config options to set up webclient                      |
| requestedHeartbeat       |    integer     | 60            | Defines after what period of time the peer TCP connection should be considered unreachable by RabbitMQ |
| handshakeTimeout         |    integer     | 6000          | To increase or decrease the default connection time out                                                |
| requestedChannelMax      |    integer     | 5             | Tells no more that 5 (or given number) could be opened up on a connection at the same time             |
| networkRecoveryInterval  |    integer     | 500           | Interval to restart the connection between rabbitmq node and clients                                   |


## Dependencies
### External
| Software Name     | Purpose                                                                                                                        | 
| :---------------- |:-------------------------------------------------------------------------------------------------------------------------------|
| PostgreSQL      | For storing information related to policy, access Request based CRUD operations, approved access requests, resources and users |
| RabbitMQ    | To publish auditing related data to auditing server via RMQ exchange                                                           |

Find the installation of the above along with the configurations to modify the database url, port, and associated credentials
in the appropriate sections [here](SETUP.md)

### Other Dependencies
| Software Name     | Purpose                                                               | 
| :---------------- |:----------------------------------------------------------------------|
| Authentication Authorization and Accounting (AAA) Server      | used to download certificate for JWT token decoding, to get user info |
| Catalogue Server    | used to fetch the list of resource and provider related information   |

### Prerequisites
### Keycloak registration for DX ACL-APD as trustee and APD
- The trustee user must be registered on Keycloak as a user
    - This can be done via the keycloak admin panel, or by using Data Exchange (DX) UI
    - The trustee user need not have any roles beforehand
- The COS admin user must call the create APD API : [Link to the API](https://authorization.iudx.org.in/apis#tag/Access-Policy-Domain-(APD)-APIs/operation/post-auth-v1-apd)
  with the name as the name of the APD, owner as email address of trustee (same as whatever is registered on Keycloak)
  and URL as the domain of the APD
- Once the APD has been successfully registered, the trustee user will gain the trustee role
  scoped to that particular APD.
    - They can verify this by calling the list user roles API : [Link to the API](https://authorization.iudx.org.in/apis#tag/User-APIs/operation/get-auth-v1-user-roles)
- The trustee can get client credentials to be used in APD Operations by calling the
  get default client credentials API : [Link to the API](https://authorization.iudx.org.in/apis#tag/User-APIs/operation/get-auth-v1-user-clientcredentials)

#### RabbitMQ
- To setup RMQ refer the docker files available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/databroker)

##### vHost table
| vHost         |  
|---------------|
| IUDX-INTERNAL |

##### Exchange table

| Exchange Name | Type of exchange | features |   
|---------------|------------------|----------|
| auditing      | direct           | durable  | 


##### Queue table


| Exchange Name | Queue Name | vHost   | routing key |
|---------------|------------|---------|-------------|
| auditing      | direct     | durable | #           |

##### Permissions
ACL-APD user could have write permission as it publishes audit data
```
 "permissions": [
        {
          "vhost": "IUDX-INTERNAL",
          "permission": {
            "configure": "^$",
            "write": "^auditing$",
            "read": "^$"
          }
        }
]
```


#### PostgresQL

- To setup PostgreSQL refer the docker files available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/postgres)
- **Note** : PostgreSQL database should be configured with a RBAC user having CRUD privileges
- Schemas for PostgreSQL tables are present here - [Flyway schema](src/main/resources/db/migration)

| Table Name               | Purpose                                                                                                                                                                  | 
|--------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| user_table               | To store user related information like first name, last name, email etc, that is fetched from AAA Server                                                                 |
| resource_entity          | To store resource information fetched from catalogue                                                                                                                     | 
| policy                   | To store policy related information, resource info, consumer and provider info                                                                                           | 
| request                  | To store the access request related information whenever an access request is created by a consumer to request provider to create policy for a resource / resource group |
| approved_access_requests | To store approved notifications when the provider sets the notification status to granted inorder to create policy                                                       |

#### PostgresQL
- Auditing is done using Immudb and Postgres DB
- To Setup immuclient for immudb please refer [here](https://github.com/datakaveri/iudx-deployment/tree/master/docs/immudb-setup)
- Schema for PostgreSQL table is present [here](https://github.com/datakaveri/iudx-resource-server/blob/master/src/main/resources/db/migration/V5_2__create-auditing-acl-apd-table.sql)
- Schema for Immudb table, index for the table is present [here](https://github.com/datakaveri/auditing-server/tree/main/src/main/resources/immudb/migration)


## Installation Steps
### Maven
1. Install java 11 and maven
2. Use the maven exec plugin based starter to start the server
   `mvn clean compile exec:java@acl-apd-server`

### JAR
1. Install java 11 and maven
2. Set Environment variables
```
export ACL_APD_URL=https://<acl-apd-domain-name>
export LOG_LEVEL=INFO
```
3. Use maven to package the application as a JAR
   `mvn clean package -Dmaven.test.skip=true`
4. 2 JAR files would be generated in the `target/` directory
    - `iudx.iudx.apd.acl.server-cluster-0.0.1-SNAPSHOT-fat.jar` - clustered vert.x containing micrometer metrics
    - `iudx.iudx.apd.acl.server-dev-0.0.1-SNAPSHOT-fat.jar` - non-clustered vert.x and does not contain micrometer metrics

#### Running the clustered JAR
**Note**: The clustered JAR requires Zookeeper to be installed. Refer [here](https://zookeeper.apache.org/doc/r3.3.3/zookeeperStarted.html) to learn more about how to set up Zookeeper. Additionally, the `zookeepers` key in the config being used needs to be updated with the IP address/domain of the system running Zookeeper.
The JAR requires 3 runtime arguments when running:

* --config/-c : path to the config file
* --hostname/-i : the hostname for clustering
* --modules/-m : comma separated list of module names to deploy

e.g. ```java -jar target/iudx.iudx.apd.acl.server-cluster-0.0.1-SNAPSHOT-fat.jar --host $(hostname)
-c secrets/all-verticles-configs/config-dev.json -m iudx.apd.acl.server.authentication.AuthenticationVerticle, iudx.apd.acl.server.apiserver.ApiServerVerticle,
iudx.apd.acl.server.policy.PolicyVerticle, iudx.apd.acl.server.notification.NotificationVerticle, iudx.apd.acl.server.auditing.AuditingVerticle```

Use the `--help/-h` argument for more information. You may additionally append an `ACL_APD_JAVA_OPTS` environment
variable containing any Java options to pass to the application.

e.g.
```
$ export ACL_APD_JAVA_OPTS="Xmx40496m"
$ java $ACL_APD_JAVA_OPTS -jar target/iudx.iudx.apd.acl.server-cluster-0.0.1-SNAPSHOT-fat.jar ...

```


#### Running the non-clustered JAR
The JAR requires 1 runtime argument when running

* --config/-c : path to the config file

e.g. `java -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.Log4j2LogDelegateFactory -jar target/iudx.iudx.apd.acl.server-cluster-0.0.1-SNAPSHOT-fat.jar -c secrets/all-verticles-configs/config-dev.json`

Use the `--help/-h` argument for more information. You may additionally append an `RS_JAVA_OPTS` environment variable containing any Java options to pass to the application.

e.g.
```
$ export ACL_APD_JAVA_OPTS="-Xmx1024m"
$ java ACL_APD_JAVA_OPTS -jar target/iudx.iudx.apd.acl.server-cluster-0.0.1-SNAPSHOT-fat.jar ...
```

### Docker
1. Install docker and docker-compose
2. Clone this repo
3. Build the images
   ` ./docker/build.sh`
4. Modify the `docker-compose.yml` file to map the config file you just created
5. Start the server in production (prod) or development (dev) mode using docker-compose
   ` docker-compose up prod `
### Maven based
1. Install java 11 and maven
2. Use the maven exec plugin based starter to start the server
   `mvn clean compile exec:java@acl-apd-server`

## Logging and Monitoring

## Testing
### Unit Testing
1. Run the server through either docker, maven or redeployer
2. Run the unit tests and generate a surefire report
   `mvn clean test-compile surefire:test surefire-report:report`
3. Jacoco reports are stored in `./target/


### Code Coverage Testing

Number of lines covered during unit testing or coverage is brought up to 90%

### Integration Testing

Integration tests are through Postman/Newman whose script can be found from [here](https://github.com/datakaveri/iudx-acl-apd/tree/main/src/test/resources).
1. Install prerequisites
- [postman](https://www.postman.com/) + [newman](https://www.npmjs.com/package/newman)
- [newman reporter-htmlextra](https://www.npmjs.com/package/newman-reporter-htmlextra)
2. Example Postman environment can be found [here](https://github.com/datakaveri/iudx-acl-apd/blob/main/src/test/resources/IUDX-ACL-APD-APIs.postman_environment.json)
3. Run the server through either docker, maven or redeployer
4. Run the integration tests and generate the newman report
   `newman run <postman-collection-path> -e <postman-environment> --insecure -r htmlextra --reporter-htmlextra-export .`
5. Reports are stored in `./target/`


### Performance Testing

Explaing how to execute tests. #TODO

### Security Testing

Explaing how to execute tests. #TODO