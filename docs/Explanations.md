# API Server Interactions
The Data Exchange (DX) Access Control List (ACL) based Access Policy Domain (APD) is used for creating, requesting and managing policy. Provider, provider delegates could allow the consumer, consumer delegates to access their resources by writing a policy against it. Policies are verified by Data Exchange (DX) Authentication Authorization and Accounting Server (AAA) Server to allow consumer, consumer delegates to access the resource.
![Overview](acl-apd-overview.png)

# API Server Solution Architecture
The following block diagram shows different components/services used in implementing the ACL-APD server.
![Solution Architecture](acl-apd-solution-architecture.png)
The above setup allows specific high load containers/services to scale with ease. Various services of the resource server are detailed in the sections below.

## API Server
An API server is an HTTPS Web Server and serves as an API gateway for actors (consumers, providers, DX AAA Server) to interact with the different services provided by the DX ACL APD Server.
These services (as described below) may be database read/write services. <br>
It is also responsible for calling the Authorization Server (via the authorization service) to authenticate and authorize access to resources.

## Database Module
Postgres is called by the specific services like Policy service, Notification service, Auth service for policy, access-request related CRUD operations, to store email, first name and last name of the user requesting the APIs. While fetching access requests, policies from the database response are processed and displayed according to the newly created or updated records.

## Auditing Service
The Data Broker is used by the API Server to log information related to successful creation, deletion of policies and successful creation, updation, and deletion of access requests.

## Authentication Service
The authentication service interacts with the DX AAA Server to validate tokens provided by a consumer of a protected resource and to fetch information about the user.

## Policy Service
The policy service is used to create, delete or list policies, for the resources owned by the provider
. Delegates of the provider could manage policies on behalf of the provider.
They could provide user specific constraints while creating a policy for a certain consumer for a given resource.
While creating a policy, the owner of the resource or provider delegate, provides ID of the resource, consumer email ID, constraints like subscription, file, async etc., along with policy expiry time in `yyyy-MM-dd'T'HH:mm:ss` format for consumer to access the resource.
By default the policy would expire in 12 days (if the expiry time is not provided).
The provider or delegate of the provider can also withdraw an active policy by providing the policy ID.
After the policy is successfully created, owner, consumer, delegates can view all the information related to the user policies.
DX AAA Server checks if any policy is present for the given resource by using the verify policy API.
Verify policy returns the constraints to access the resource for the active policy.

## Notification Service
Consumer or consumer delegates could request the provider to access the resource by providing information of the resource like it's ID
and additional information related to the purpose for which the resource is being accessed.
Academia, research, non-commercial could be listed down to access the resource to help the provider take an informed decision while approving the request.
An email is sent to the provider and provider delegates to approve or reject the request by visiting the provider panel.
While approving the access request, information like access constraints, ID of the access request, expiry time in `yyyy-MM-dd'T'HH:mm:ss` format to access the resource when the policy is created could be provided.  
If the consumer or consumer delegate no longer wants to create the access request or if an active policy for the given access request is present,
the consumer could withdraw the pending access request by providing it's ID.
After the access request is created, all the information regarding access request could be viewed by the user.
