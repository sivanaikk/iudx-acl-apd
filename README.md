[![Jenkins Build](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fjenkins.iudx.io%2Fjob%2FACL-APD%2520Server(master)%2F)](https://jenkins.iudx.io/job/ACL-APD%20Server(master)/lastBuild/)
[![Jenkins Tests](https://img.shields.io/jenkins/tests?jobUrl=https%3A%2F%2Fjenkins.iudx.io%2Fjob%2FACL-APD%2520Server(master)%2F)](https://jenkins.iudx.io/job/ACL-APD%20Server(master)/lastBuild/testReport/)
[![Jenkins Coverage](https://img.shields.io/jenkins/coverage/jacoco?jobUrl=https%3A%2F%2Fjenkins.iudx.io%2Fjob%2FACL-APD%2520Server(master)%2F)](https://jenkins.iudx.io/job/ACL-APD%20Server(master)/lastBuild/jacoco/)
[![Jenkins Build](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fjenkins.iudx.io%2Fjob%2FACL-APD%2520Server(master)%2F&label=integration%20tests)](https://jenkins.iudx.io/job/ACL-APD%20Server(master)/lastBuild/Integration_20Test_20Report/)
[![Jenkins Build](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fjenkins.iudx.io%2Fjob%2FACL-APD%2520Server(master)%2F&label=security%20tests)](https://jenkins.iudx.io/job/ACL-APD%20Server(master)/lastBuild/zap/)


![IUDX](./docs/iudx.png)

# DX Access Control List (ACL) Access Policy Domain (APD) Server

# Explanation 
## Introduction
The Data Exchange (DX) Access Control List (ACL) based Access Policy Domain (APD)
is used for creating, requesting and managing policy. Provider, provider delegates could
allow the consumer, consumer delegates to access their resources by writing a policy against it.
Policies are verified by Data Exchange (DX) Authentication Authorization and Accounting Server (AAA) Server to
allow consumer, consumer delegates to access the resource.

<p align="center">
<img src="./docs/acl-apd-overview.png">
</p>

## Users and Roles
- Shall be available in the docs page
<div style="text-align: center;">
<img src="./docs/users-and-roles.png" alt="Users and Roles" width="600" height="400"/>
</div>

- Tokens for a user could be created using DX AAA Server API : [link to the API docs](https://authorization.iudx.org.in/apis#tag/Token-APIs/operation/post-auth-v1-token) 
- Provider, Consumers and delegates of provider and consumers are allowed to access the APIs
- Provider / provider delegate specific APIs : Create Policy, Update access request, Delete Policy
- Consumer / consumer delegate specific APIs : Create Access Request, Withdraw access request
- Commonly accessible APIs for both the users : List Access Request,  Get Policies
- How is the user considered as a consumer, provider or delegate?
   - If the **role** in token fetched from DX AAA Server is **provider**, **consumer**
   - A user is considered as a delegate of the consumer if **role** is **delegate** and **drl** is **consumer**
   - A user is considered as a delegate of the provider if **role** is **delegate** and **drl** is **provider**
## Tokens in DX
### Tokens accepted in ACL APD Server
- Identity token is used in the header of the request for all the user specific APIs
- Bearer token is used in the header of the request for Verify Policy API
  - Verify Policy request is made by DX AAA Server

## Terminologies and Definitions
- Policy : An agreement or contract between the owner of the resource to allow the consumer to access the resource
- Notification : (or access request) Is a requisition from consumer to owner of the resource to access the resource
- Constraints: (or capabilities) Are different methods in which information related to resource can be fetched
- Policy Status : Policy could be in either of these states - active, deleted or expired
- Notification Status: Notification could be in either of these states - pending, granted, rejected, withdrawn
- Delegate : Consumer or provider appointed user who could act on behalf of the delegator

## Features
- Allows provider, provider delegates to create, fetch, manage policies over their resources
- Allows consumers fetch policies, request access for resources by sending email notifications to the provider, provider delegates
- Emails are sent asynchronously using Vert.x SMTP Mail Client
- Integration with DX AAA Server for token introspection to serve data privately to the designated user
- Uses Vert.x, Postgres to create scalable, service mesh architecture
- Integration with auditing server for metering purposes

## Solution Architecture
The following block diagram shows different components/services used in implementing the ACL-APD server.
![Solution Architecture](./docs/acl-apd-solution-architecture.png)
The above setup allows specific high load containers/services to scale with ease. Various services of the resource server are detailed in the sections below.

### API Server
An API server is an HTTPS Web Server and serves as an API gateway for actors (consumers, providers, DX AAA Server) to interact with the different services provided by the DX ACL APD Server.
These services (as described below) may be database read/write services. <br>
It is also responsible for calling the Authorization Server (via the authorization service) to authenticate and authorize access to resources.

### Database Module
Postgres is called by the specific services like Policy service, Notification service, Auth service for policy, access-request related CRUD operations, to store email, first name and last name of the user requesting the APIs. While fetching access requests, policies from the database response are processed and displayed according to the newly created or updated records.

### Auditing Service
The Data Broker is used by the API Server to log information related to successful creation, deletion of policies and successful creation, updation, and deletion of access requests.

### Authentication Service
The authentication service interacts with the DX AAA Server to validate tokens provided by a consumer of a protected resource and to fetch information about the user.

### Policy Service
The policy service is used to create, delete or list policies, for the resources owned by the provider
. Delegates of the provider could manage policies on behalf of the provider.
They could provide user specific constraints while creating a policy for a certain consumer for a given resource.
While creating a policy, the owner of the resource or provider delegate, provides ID of the resource, consumer email ID, constraints like subscription, file, async etc., along with policy expiry time in `yyyy-MM-dd'T'HH:mm:ss` format for consumer to access the resource.
By default the policy would expire in 12 days (if the expiry time is not provided).
The provider or delegate of the provider can also withdraw an active policy by providing the policy ID.
After the policy is successfully created, owner, consumer, delegates can view all the information related to the user policies.
DX AAA Server checks if any policy is present for the given resource by using the verify policy API.
Verify policy returns the constraints to access the resource for the active policy.

### Notification Service
Consumer or consumer delegates could request the provider to access the resource by providing information of the resource like it's ID
and additional information related to the purpose for which the resource is being accessed.
Academia, research, non-commercial could be listed down to access the resource to help the provider take an informed decision while approving the request.
An email is sent to the provider and provider delegates to approve or reject the request by visiting the provider panel.
While approving the access request, information like access constraints, ID of the access request, expiry time in `yyyy-MM-dd'T'HH:mm:ss` format to access the resource when the policy is created could be provided.  
If the consumer or consumer delegate no longer wants to create the access request or if an active policy for the given access request is present,
the consumer could withdraw the pending access request by providing it's ID.
After the access request is created, all the information regarding access request could be viewed by the user.

# How To Guide
## Setup and Installation
SETUP and installation guide is available [here](./docs/SETUP-and-Installation.md)

# Tutorial
## Tutorials and Explanations
-> Provide the link here #TODO
-> Flows and sequence #TODO

# Reference
## API Docs
API docs are available [here](https://acl-apd.iudx.org.in/apis)

## FAQ
FAQs are available [here](./docs/FAQ.md)

## Contributing
We follow Git Merge based workflow
1. Fork this repo
2. Create a new feature branch in your fork. Multiple features must have a hyphen separated name
3. Commit to your fork and raise a Pull Request with upstream

## License
[View License](./LICENSE)

