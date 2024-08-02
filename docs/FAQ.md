# Frequently Asked Questions (FAQs)

1. Which kind of token is required to make HTTP requests to the ACL-APD Server?
- Identity token from DX Auth Server. [Link to the API docs of DX Auth server](https://cos.iudx.org.in/auth/apis#tag/Token-APIs/operation/post-auth-v1-token)

2. What do we do when there is any error during flyway migration?
- We could run this command `mvn flyway:repair` and do the flyway migration again
-If the error persists, it needs to be resolved manually and a backup of the database could be taken from postgres if the table needs to be changed

3. “Request could not be created, as resource was not found” - even if the resource is found while creating access request
- This error occurs when the resource server URL that the consumer is associated to while requesting the API, does not match with the resource server URL of the resource item

4. Can the constraints given while creating the policy for a resource be different ?
- Yes, the constraints given to access the resource is defined by resource server and provider could follow the same json structure to add different type of user specific constraints

5. Are there any access restrictions for delegates of the provider or consumer?
- No, delegates can access all the specific provider or consumer related APIs. Please refer [users and roles](../README.md#users-and-roles)

6. “Given id is invalid - it is group level resource” - Unable to create policies for resource groups
- From Release [version 1.0.1](https://github.com/datakaveri/iudx-acl-apd/releases/tag/v1.0.1), the policies for new resource groups are restricted to be created
Policies could be created for a resource item

