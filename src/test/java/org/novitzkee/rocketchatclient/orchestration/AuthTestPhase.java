package org.novitzkee.rocketchatclient.orchestration;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Order(IntegrationSuiteOrchestrator.AUTH_PHASE_FIRST)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock(value = IntegrationSuiteOrchestrator.AUTH_PHASE_LOCK, mode = ResourceAccessMode.READ_WRITE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public @interface AuthTestPhase {
}
