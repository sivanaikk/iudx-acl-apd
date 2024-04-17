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

## Setting up config
- In order to setup PostgreSQL, RabbitMQ, Email Service, connect with
DX Catalogue Server, DX AAA Server, appropriate information
could be updated in configs available at  [config-example.json](example-config/config-dev.json)
- Please refer [README.md](example-config/README.md) to update configs



## Prerequisites
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



## Setting up RabbitMQ for DX ACL-APD Server
- To setup RMQ refer the docker files available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/databroker)


### vHost table
| vHost         |  
|---------------|
| IUDX-INTERNAL |

### Exchange table

| Exchange Name | Type of exchange | features |   
|---------------|------------------|----------|
| auditing      | direct           | durable  | 


### Queue table


| Exchange Name | Queue Name | vHost   | routing key |
|---------------|------------|---------|-------------|
| auditing      | direct     | durable | #           |

### Permissions
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

## Setting up Postgres for DX ACL-APD Server
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
                      
## Setting up Auditing for DX ACL-APD Server

- Auditing is done using Immudb and Postgres DB
- To Setup immuclient for immudb please refer [here](https://github.com/datakaveri/iudx-deployment/tree/master/docs/immudb-setup) 
- Schema for PostgreSQL table is present [here](https://github.com/datakaveri/iudx-resource-server/blob/master/src/main/resources/db/migration/V5_2__create-auditing-acl-apd-table.sql)
- Schema for Immudb table, index for the table is present [here](https://github.com/datakaveri/auditing-server/tree/main/src/main/resources/immudb/migration)
