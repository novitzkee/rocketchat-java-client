package org.novitzkee.rocketchatclient.realtime.method.authentication;

import org.apache.commons.codec.digest.DigestUtils;
import org.novitzkee.rocketchatclient.realtime.common.MethodCall;
import org.novitzkee.rocketchatclient.realtime.common.MethodName;

import java.time.Instant;

public class LoginMethodCall extends MethodCall<LoginMethodCall.Info> {

    private LoginMethodCall(Object[] params) {
        super(MethodName.LOGIN, params);
    }

    @Override
    protected Class<Info> resultClass() {
        return Info.class;
    }

    public static LoginMethodCall usingUsernameAndPassword(String username, String password) {
        final User user = new User(username);
        final HashedPassword hashedPassword = new HashedPassword(DigestUtils.sha256Hex(password), "sha-256");
        return new LoginMethodCall(new Object[]{new CredentialsParam(user, hashedPassword)});
    }

    public static LoginMethodCall usingAuthenticationToken(String authenticationToken) {
        return new LoginMethodCall(new Object[]{new TokenParam(authenticationToken)});
    }

    public record TokenParam(String resume) { }

    public record CredentialsParam(User user, HashedPassword password) { }

    public record User(String username) { }

    public record HashedPassword(String digest, String algorithm) { }

    public record Info(String id, String token, Instant tokenExpires, String type) { }
}
