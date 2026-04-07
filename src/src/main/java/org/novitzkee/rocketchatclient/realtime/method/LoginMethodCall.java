package org.novitzkee.rocketchatclient.realtime.method;

import org.novitzkee.rocketchatclient.realtime.common.DdpMessageType;
import org.novitzkee.rocketchatclient.realtime.common.MethodCall;
import org.novitzkee.rocketchatclient.realtime.util.CallUtil;

public class LoginMethodCall extends MethodCall<LoginMethodResult, String> {

    public static final String METHOD_NAME = "login";

    @Override
    public Class<LoginMethodResult> getResponseMessageClass() {
        return LoginMethodResult.class;
    }

    @Override
    public String getResult(LoginMethodResult responseMessage) {
        CallUtil.expectMessageOfType(responseMessage, DdpMessageType.RESULT);
        return "OK"; // TODO
    }
}
