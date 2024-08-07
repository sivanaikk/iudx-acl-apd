# Modules

## Api Server Verticle

| Key Name          | Value Datatype | Value Example | Description                                                                                          |
|:------------------|:--------------:|:--------------|:-----------------------------------------------------------------------------------------------------|
| isWorkerVerticle  |    boolean     | false         | To check if worker verticle needs to be deployed for blocking operations                             |
| verticleInstances |    integer     | 1             | Number of instances required for verticles                                                           |
| ssl               |    boolean     | true          | To create a encrypted link between the browser and server to keep the information private and secure |
| httpPort          |    integer     | 8443          | Port for running the instance DX ACL-APD Server                                                      |

## Other Configuration

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
| commonConfig.authHost            |     String     | authvertx.iudx.io                    | Host name of DX AAA Server                                                                                                   |
| commonConfig.authPort            |    integer     | 443                                  | Port number to access HTTPS APIs of DX AAA server Default                                                                    |
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

## Policy Verticle

| Key Name          | Value Datatype | Value Example | Description                                                              |
|:------------------|:--------------:|:--------------|:-------------------------------------------------------------------------|
| isWorkerVerticle  |    boolean     | false         | To check if worker verticle needs to be deployed for blocking operations |
| verticleInstances |    integer     | 1             | Number of instances required for verticles                               |
| defaultExpiryDays |    integer     | 12            | Default number of days to expire a policy                                |
| domain            |     String     | iudx.io       | Domain for which DX ACL-APD Server is deployed                           |

## Notification Verticle

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


## Authentication Verticle

| Key Name          | Value Datatype | Value Example     | Description                                                              |
|:------------------|:--------------:|:------------------|:-------------------------------------------------------------------------|
| isWorkerVerticle  |    boolean     | false             | To check if worker verticle needs to be deployed for blocking operations |
| verticleInstances |    integer     | 1                 | Number of instances required for verticles                               |
| issuer            |     String     | cos.iudx.io       | To authenticate the issuer in the token                                  |
| authServerHost    |     String     | authvertx.iudx.io | DX AAA Server host name                                                  |
| jwtIgnoreExpiry   |    boolean     | false             | Set to true while using the server locally to allow expired tokens       |

## Auditing Verticle

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

