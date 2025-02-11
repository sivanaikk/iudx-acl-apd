![IUDX](./iudx.png)

# Setup and Installation Guide
This document contains the installation and configuration
processes of the external modules of each Verticle in Data Exchange ACL-APD Server.

## Configuration
In order to setup PostgreSQL, RabbitMQ, Email Service, connect with DX Catalogue Server, DX AAA Server, appropriate information
could be updated in configs. Please refer [Configurations](Configurations.md)

## Dependencies
### External
| Software Name | Purpose                                                                                                                        | 
|:--------------|:-------------------------------------------------------------------------------------------------------------------------------|
| PostgreSQL    | For storing information related to policy, access Request based CRUD operations, approved access requests, resources and users |
| RabbitMQ      | To publish auditing related data to auditing server via RMQ exchange                                                           |

Find the installation of the above along with the configurations to modify the database url, port, and associated credentials
in the appropriate sections [here](SETUP.md)

### Other Dependencies
| Software Name                                              | Purpose                                                               | 
|:-----------------------------------------------------------|:----------------------------------------------------------------------|
| Authentication Authorization and Accounting (AAA) Server   | used to download certificate for JWT token decoding, to get user info |
| Catalogue Server                                           | used to fetch the list of resource and provider related information   |

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

Explain how to execute tests. #TODO

### Security Testing

Explain how to execute tests. #TODO