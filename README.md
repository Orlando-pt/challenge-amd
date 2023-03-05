# The Asset Management Digital Challenge

Some improvements to have into consideration before sending the application to production:

- One of the fastest fixes would be to add a `Dockerfile` that would allow the creation of a docker container.
This would ensure that the application runs equally regardless of where it is deployed or tested.
Containers also allow applications to be more rapidly deployed, patched or scaled.

- The documentation of the application API should also be considered. The use of an `OpenAPI` specification and its provisioning
would be advantageous for other developers.

- Using this type of in-memory storage does not allow the application to scale, there is no efficient way for instances to
communicate with the `ConcurrentHashMap`. We can still use in-memory storage if, for instance, low latency of operations is
a requirement, but the app would need something more robust like a `Redis` cluster that would allow multiple instances to access
data and work with it concurrently. If in-memory databases are not a requirement, then the app could benefit from using databases
like `MySQL`, `PostgreSQL`, ... or also NoSQL databases like `MongoDB`.

- Monitoring is also something that the project should take care of before production. A solution like `Grafana` should be added
to the project, allowing graphical monitoring of hardware requirements about the deployment and also query about important
information related to transactions or accounts.

- Both `TDD` and `BDD` would help to ensure that code is reliable and meets the needs of stakeholders.
By writing tests first, developers are forced to think about how the code should behave and what edge cases should be considered.
This can lead to more robust and bug-free code, as well as faster development cycles since issues are caught early in the process.

- Lastly, it would be very important to add a `CI/CD` pipeline. It is a key practice for modern software development.
It helps teams move quickly, deliver high-quality code, and collaborate effectively, all while maintaining stable infrastructure.