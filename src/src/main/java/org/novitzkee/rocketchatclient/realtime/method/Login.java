package org.novitzkee.rocketchatclient.realtime.method;

import org.novitzkee.rocketchatclient.realtime.common.MethodCall;
import org.novitzkee.rocketchatclient.realtime.common.MethodName;

public class Login extends MethodCall<Login.Result> {

    private Login(Object[] params) {
        super(MethodName.LOGIN, params);
    }

    @Override
    protected Class<Result> resultClass() {
        return Result.class;
    }

    public record Token(String resume) { }

    // TODO: Parse full response
    public record Result(String id) { }

    // TODO: Add different login methods
    public static Login usingAuthenticationToken(String authenticationToken) {
        return new Login(new Object[]{new Token(authenticationToken)});
    }
}
