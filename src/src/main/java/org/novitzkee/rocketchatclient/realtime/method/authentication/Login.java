package org.novitzkee.rocketchatclient.realtime.method.authentication;

import org.apache.commons.codec.digest.DigestUtils;
import org.novitzkee.rocketchatclient.realtime.common.MethodCall;
import org.novitzkee.rocketchatclient.realtime.common.MethodName;

import java.time.Instant;

public class Login extends MethodCall<Login.Info> {

    private Login(Object[] params) {
        super(MethodName.LOGIN, params);
    }

    @Override
    protected Class<Info> resultClass() {
        return Info.class;
    }

    public static Login usingUsernameAndPassword(String username, String password) {
        final User user = new User(username);
        final HashedPassword hashedPassword = new HashedPassword(DigestUtils.sha256Hex(password), "sha-256");
        return new Login(new Object[]{new CredentialsParam(user, hashedPassword)});
    }

    public static Login usingAuthenticationToken(String authenticationToken) {
        return new Login(new Object[]{new TokenParam(authenticationToken)});
    }

    public record TokenParam(String resume) { }

    public record CredentialsParam(User user, HashedPassword hashedPassword) { }

    public record User(String username) { }

    public record HashedPassword(String digest, String algorithm) { }

    public record Info(String id, String token, Instant tokenExpires, String type) { }
}
