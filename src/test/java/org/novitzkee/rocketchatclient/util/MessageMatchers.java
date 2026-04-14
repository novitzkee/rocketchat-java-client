package org.novitzkee.rocketchatclient.util;

import com.jayway.jsonpath.JsonPath;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.novitzkee.rocketchatclient.realtime.common.DdpMessageType;
import org.novitzkee.rocketchatclient.realtime.common.MethodName;

import java.util.Objects;

import static org.mockito.ArgumentMatchers.argThat;

@NoArgsConstructor(access = AccessLevel.NONE)
public class MessageMatchers {

    private static final JsonPath METHOD_NAME_PATH = JsonPath.compile("$.method");

    public static String methodCallWithName(MethodName methodName) {
        return argThat(jsonMessage -> StringUtils.isNotBlank(jsonMessage) &&
                Objects.equals(DdpMessageType.METHOD.value(), DdpMessageType.DDP_MESSAGE_TYPE_PATH.read(jsonMessage)) &&
                Objects.equals(methodName.value(), METHOD_NAME_PATH.read(jsonMessage))
        );
    }

    public static String ddpMessageOfType(DdpMessageType ddpMessageType) {
        return argThat(jsonMessage -> StringUtils.isNotBlank(jsonMessage) &&
                Objects.equals(ddpMessageType.value(), DdpMessageType.DDP_MESSAGE_TYPE_PATH.read(jsonMessage))
        );
    }
}
