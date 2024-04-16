### Making configurable base path
- Base path can be added in postman environment file or in postman.
- `IUDX-ACL-APD-APIs.postman_environment.json` has **values** array that has fields named **basePath** whose **value** is currently set to `/dx/apd/acl/v1`, **authBasePath** with value `auth/v1`.
- These value(s) could be changed according to the deployment and then the collection with the `IUDX-ACL-APD-APIs.postman_environment.json` file can be uploaded to Postman
- For the changing the **basePath**, **authBasePath**,authBasePath, **resourceServerUrl**  values in postman after importing the collection and environment files, locate `IUDX ACL-APD` from **Environments** in sidebar of Postman application.
- To know more about Postman environments, refer : [postman environments](https://learning.postman.com/docs/sending-requests/managing-environments/)
- The **CURRENT VALUE** of the variable could be changed

### While running the integration tests manually
- The policies that are `ACTIVE` could be deleted using the GET /policies and DELETE /policies API
so that the access requests would not throw 4xx response

