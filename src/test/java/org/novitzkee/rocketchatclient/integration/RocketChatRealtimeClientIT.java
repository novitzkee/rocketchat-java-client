package org.novitzkee.rocketchatclient.integration;

import org.junit.jupiter.api.*;
import org.novitzkee.rocketchatclient.orchestration.AuthTestPhase;
import org.novitzkee.rocketchatclient.orchestration.DomainTestPhase;
import org.novitzkee.rocketchatclient.orchestration.IntegrationSuite;
import org.novitzkee.rocketchatclient.realtime.RocketChatRealtimeClient;
import org.novitzkee.rocketchatclient.realtime.method.authentication.LoginMethodCall;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationSuite
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RocketChatRealtimeClientIT extends RocketChatIntegrationTest {

    private static final String ROCKET_CHAT_REALTIME_API_PATH = "websocket";

    private final RocketChatRealtimeClient rocketChatRealtimeClient = RocketChatRealtimeClient.builder()
            .apiUri(rocketChatWebsocketUrl().resolve(ROCKET_CHAT_REALTIME_API_PATH))
            .callTimeoutDuration(Duration.ofSeconds(15))
            .httpClient(
                    HttpClient.newBuilder()
                            .version(HttpClient.Version.HTTP_1_1)
                            .build()
            )
            .build();

    @Nested
    @AuthTestPhase
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class AuthenticationTest {

        private String token;

        @Test
        @Order(1)
        void testConnect() {
            // when
            final String session = rocketChatRealtimeClient.connect().join();

            // then
            assertThat(session).isNotBlank();
        }

        @Test
        @Order(2)
        void testLoginWithUsernameAndPassword() {
            // given
            final LoginMethodCall loginWithUsernameAndPasswordCall =
                    LoginMethodCall.usingUsernameAndPassword(ADMIN_USERNAME, ADMIN_PASSWORD);

            // when
            final LoginMethodCall.Info loginResult =
                    rocketChatRealtimeClient.performMethodCall(loginWithUsernameAndPasswordCall).join();

            // then
            assertThat(loginResult.id()).isNotBlank();
            assertThat(loginResult.token()).isNotBlank();
            assertThat(loginResult.tokenExpires()).isNotNull();
            assertThat(loginResult.type()).isEqualTo("password");

            token = loginResult.token();
        }

        @Test
        @Order(3)
        void testLoginWithAuthenticationToken() {
            Objects.requireNonNull(token, "Token must be set before this test can be executed.");

            // given
            final LoginMethodCall loginWithAuthenticationTokenCall = LoginMethodCall.usingAuthenticationToken(token);

            // when
            final LoginMethodCall.Info loginResult =
                    rocketChatRealtimeClient.performMethodCall(loginWithAuthenticationTokenCall).join();

            // then
            assertThat(loginResult.id()).isNotBlank();
            assertThat(loginResult.token()).isNotBlank();
            assertThat(loginResult.tokenExpires()).isNotNull();
            assertThat(loginResult.type()).isEqualTo("resume");
        }
    }

    @Nested
    @DomainTestPhase
    class ChannelsTest {

        @Test
        void testCreateChannel() {
            // TODO: Implement method and fill in the test.
            assertThat(true).isTrue();
        }
    }

    @Nested
    @DomainTestPhase
    class RoomsTest {

        @Test
        void testGetRoomByID() {
            // TODO: Implement method and fill in the test.
            assertThat(true).isTrue();
        }
    }

    @Nested
    @DomainTestPhase
    class MessageTest {

        @Test
        void testSendMessage() {
            // TODO: Implement method and fill in the test.
            assertThat(true).isTrue();
        }
    }

    @Nested
    @DomainTestPhase
    class UsersTest {

        @Test
        void testSetUserPresence() {
            // TODO: Implement method and fill in the test.
            assertThat(true).isTrue();
        }
    }
}
