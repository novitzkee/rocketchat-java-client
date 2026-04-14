package org.novitzkee.rocketchatclient.util;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.time.Duration;

public class RocketChatIntegrationTest {

    private static final DockerImageName MONGO_8 = DockerImageName.parse("mongo:8.0");

    private static final DockerImageName ROCKET_CHAT_8 = DockerImageName.parse("rocket.chat:8.2.1");

    private static final Network NETWORK = Network.newNetwork();

    private static final String MONGO_NETWORK_ALIAS = "mongodb";

    private static final int ROCKET_CHAT_PORT = 3000;

    static final MongoDBContainer MONGO_DB = new MongoDBContainer(MONGO_8)
            .withNetwork(NETWORK)
            .withNetworkAliases(MONGO_NETWORK_ALIAS)
            .withReplicaSet()
            .withCommand("--replSet rs --oplogSize 128 --bind_ip_all")
            .withReuse(true);

    static final GenericContainer<?> ROCKET_CHAT = new GenericContainer<>(ROCKET_CHAT_8)
            .withNetwork(NETWORK)
            .withExposedPorts(ROCKET_CHAT_PORT)
            .dependsOn(MONGO_DB)
            .withEnv("MONGO_URL", String.format("mongodb://%s:27017/rocketchat", MONGO_NETWORK_ALIAS))
            .withEnv("MONGO_OPLOG_URL", String.format("mongodb://%s:27017/local", MONGO_NETWORK_ALIAS))
            .waitingFor(
                    Wait.forHttp("/readyz")
                            .forStatusCode(200)
                            .withStartupTimeout(Duration.ofMinutes(1))
            )
            .withReuse(true);
    // Reuse in this case only prevents killing the container after tests finish to inspect/debug.

    @BeforeAll
    static void spinUpRocketChat() {
        MONGO_DB.start();
        ROCKET_CHAT.start();
    }

    @SneakyThrows
    protected URI rocketChatHttpUrl() {
        final String uri = String.format("http://%s:%d/", ROCKET_CHAT.getHost(), ROCKET_CHAT.getMappedPort(ROCKET_CHAT_PORT));
        return new URI(uri);
    }

    @SneakyThrows
    protected URI rocketChatWebsocketUrl() {
        final String uri = String.format("ws://%s:%d/", ROCKET_CHAT.getHost(), ROCKET_CHAT.getMappedPort(ROCKET_CHAT_PORT));
        return new URI(uri);
    }
}
