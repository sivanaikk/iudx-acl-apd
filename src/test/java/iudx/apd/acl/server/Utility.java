package iudx.apd.acl.server;

import static iudx.apd.acl.server.apiserver.util.Constants.RESULT;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import iudx.apd.acl.server.policy.PostgresService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;

public class Utility {
    public static final String INSERT_INTO_RESOURCE_ENTITY_TABLE =
            "INSERT INTO resource_entity(_id, provider_id, resource_group_id, created_at, updated_at) VALUES ($1, $2, $3, $4, $5) RETURNING _id;";
    public static final String INSERT_INTO_POLICY_TABLE =
            "INSERT INTO policy(_id, user_emailid, item_id, item_type, owner_id, status, expiry_at, constraints, created_at, updated_at) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10) RETURNING _id;";
    public static final String INSERT_INTO_USER_TABLE =
            "INSERT INTO user_table(_id, email_id, first_name, last_name, created_at, updated_at) VALUES ($1, $2, $3, $4, $5, $6) RETURNING _id;";
    public static final String INSERT_INTO_REQUEST_TABLE = "INSERT INTO request(_id, user_id, item_id, item_type, owner_id, status, expiry_at, created_at, updated_at, constraints) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10) RETURNING _id;";
    private static final Logger LOG = LoggerFactory.getLogger(Utility.class);
    private PgPool pool;
    private String resourceType;
    private String status;
    private JsonObject constraints;
    private UUID resourceId;
    private UUID policyId;
    private UUID consumerId;
    private String consumerEmailId;
    private String consumerFirstName;
    private String consumerLastName;
    private UUID ownerId;
    private String ownerEmailId;
    private String ownerFirstName;
    private String ownerLastName;
    private Tuple resourceInsertionTuple;
    private Tuple consumerTuple;
    private Tuple ownerTuple;
    private Tuple policyInsertionTuple;
    private Tuple requestInsertionTuple;
    private UUID requestId;
    private String requestStatus;
    private boolean hasFailed;

    public static UUID generateRandomUuid() {
        return UUID.randomUUID();
    }

    public static String generateRandomString() {
        return UUID.randomUUID().toString();
    }

    public static String generateRandomEmailId() {
        return generateRandomString().substring(0, 6)
                + "@"
                + generateRandomString().substring(0, 3)
                + ".com";
    }
    public PostgresService setUp(PostgreSQLContainer container) {
        Vertx vertx = Vertx.vertx();
        Integer port = container.getFirstMappedPort();
        String host = container.getHost();
        String db = container.getDatabaseName();
        String user = container.getUsername();
        String password = container.getPassword();
        String jdbcUrl = container.getJdbcUrl();

        JsonObject postgresConfig =
                new JsonObject()
                        .put("databaseIP", host)
                        .put("databasePort", port)
                        .put("databaseName", db)
                        .put("databaseUserName", user)
                        .put("databasePassword", password)
                        .put("poolSize", 25);

        if (container.isRunning()) {
            LOG.info("container is running....");
            PostgresService postgresService = new PostgresService(postgresConfig, vertx);
            pool = postgresService.getPool();


            Flyway flyway =
                    Flyway.configure()
                            .dataSource(
                                    jdbcUrl,
                                    postgresConfig.getString("databaseUserName"),
                                    postgresConfig.getString("databasePassword"))
                            .placeholders(Map.of("user", user, "aclApdUser", user))
                            .locations("db/migration/")
                            .load();
            flyway.repair();
            var migrationResult = flyway.migrate();
            LOG.info("Migration result {}", migrationResult.migrationsExecuted);
            LOG.info("Migration details {}", migrationResult.getTotalMigrationTime());

            initialize();
            return postgresService;
        }
        return null;
    }

    private void initialize() {
        resourceType = "RESOURCE_GROUP";
        status = "ACTIVE";
        LocalDateTime expiryTime = LocalDateTime.of(2025, 12, 10, 3, 20, 20, 29);
        constraints = new JsonObject();
        resourceId = generateRandomUuid();
        ownerId = generateRandomUuid();

        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.of(2023, 12, 10, 3, 20, 10, 9);

        resourceInsertionTuple = Tuple.of(resourceId, ownerId, null, createdAt, updatedAt);

        consumerId = generateRandomUuid();
        consumerEmailId = generateRandomEmailId();
        consumerFirstName = generateRandomString();
        consumerLastName = generateRandomString();
        consumerTuple =
                Tuple.of(
                        consumerId, consumerEmailId, consumerFirstName, consumerLastName, createdAt, updatedAt);

        ownerEmailId = generateRandomEmailId();
        ownerFirstName = generateRandomString();
        ownerLastName = generateRandomString();
        ownerTuple =
                Tuple.of(ownerId, ownerEmailId, ownerFirstName, ownerLastName, createdAt, updatedAt);

        policyId = generateRandomUuid();

        policyInsertionTuple =
                Tuple.of(
                        policyId,
                        consumerEmailId,
                        resourceId,
                        resourceType,
                        ownerId,
                        status,
                        expiryTime,
                        constraints,
                        createdAt,
                        updatedAt);
        requestId = generateRandomUuid();
        requestStatus = "PENDING";

        requestInsertionTuple = Tuple.of(requestId, consumerId, resourceId, resourceType, ownerId, requestStatus, expiryTime, createdAt, updatedAt, constraints);
    }
    public Future<Boolean> testInsert()
    {
        LOG.info("inside test insert method");
        Promise<Boolean> promise = Promise.promise();
        executeBatchQuery(List.of(ownerTuple, consumerTuple),INSERT_INTO_USER_TABLE).onComplete(  handler -> {
            if (handler.succeeded()) {
                executeQuery(resourceInsertionTuple, INSERT_INTO_RESOURCE_ENTITY_TABLE).onComplete(resourceHandler -> {
                    if(resourceHandler.succeeded())
                    {
                        executeQuery(requestInsertionTuple, INSERT_INTO_REQUEST_TABLE).onComplete(requestInsertionHandler -> {
                            if(requestInsertionHandler.succeeded())
                            {
                                executeQuery(policyInsertionTuple, INSERT_INTO_POLICY_TABLE).onComplete(policyHandler -> {
                                    if(policyHandler.succeeded())
                                    {
                                        LOG.info("Succeeded in inserting all the queries");
                                        LOG.info("Result from the insertion : {}, {}, {}, {}",handler.result(),resourceHandler.result(), policyHandler.result(), requestInsertionHandler.result());
                                        promise.complete(true);
                                    }
                                    else
                                    {
                                        hasFailed = true;
                                    }
                                });
                            }
                            else
                            {
                                hasFailed = true;
                            }
                        });
                    }
                    else
                    {
                        hasFailed = true;
                    }
                });
            } else {
                hasFailed = true;
            }
            if (hasFailed)
            {
                LOG.info("Failed to insert");
                promise.fail("Failed to insert");
            }
        });
        return promise.future();
    }


    public Future<JsonObject> executeBatchQuery(List<Tuple> tuple, String query) {
        Promise<JsonObject> promise = Promise.promise();
        Collector<Row, ?, List<JsonObject>> rowListCollectors =
                Collectors.mapping(row -> row.toJson(), Collectors.toList());

        pool.withConnection(
                        sqlConnection ->
                                sqlConnection
                                        .preparedQuery(query)
                                        .collecting(rowListCollectors)
                                        .executeBatch(tuple)
                                        .map(rows -> rows.value()))
                .onSuccess(
                        successHandler -> {
                            promise.complete(
                                    new JsonObject().put(RESULT, "Success").put("response", successHandler));
                        })
                .onFailure(
                        failureHandler -> {
                            failureHandler.printStackTrace();
                            promise.fail("Failure due to: " + failureHandler.getCause().getMessage());
                        });
        return promise.future();
    }
    public Future<JsonObject> executeQuery(Tuple tuple, String query) {
        Promise<JsonObject> promise = Promise.promise();
        Collector<Row, ?, List<JsonObject>> rowListCollectors =
                Collectors.mapping(row -> row.toJson(), Collectors.toList());

        pool.withConnection(
                        sqlConnection ->
                                sqlConnection
                                        .preparedQuery(query)
                                        .collecting(rowListCollectors)
                                        .execute(tuple)
                                        .map(rows -> rows.value()))
                .onSuccess(
                        successHandler -> {
                            promise.complete(
                                    new JsonObject().put(RESULT, "Success").put("response", successHandler));
                        })
                .onFailure(
                        failureHandler -> {
                            failureHandler.printStackTrace();
                            promise.fail("Failure due to: " + failureHandler.getCause().getMessage());
                        });
        return promise.future();
    }

    public String getResourceType() {
        return resourceType;
    }
    public UUID getRequestId() {
        return requestId;
    }

    public String getStatus() {
        return status;
    }

    public JsonObject getConstraints() {
        return constraints;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public UUID getPolicyId() {
        return policyId;
    }

    public UUID getConsumerId() {
        return consumerId;
    }

    public String getConsumerEmailId() {
        return consumerEmailId;
    }

    public String getConsumerFirstName() {
        return consumerFirstName;
    }

    public String getConsumerLastName() {
        return consumerLastName;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getOwnerEmailId() {
        return ownerEmailId;
    }

    public String getOwnerFirstName() {
        return ownerFirstName;
    }

    public String getOwnerLastName() {
        return ownerLastName;
    }
    public String getRequestStatus(){
        return requestStatus;
    }

}
