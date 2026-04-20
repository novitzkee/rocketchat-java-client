package org.novitzkee.rocketchatclient.orchestration;

import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.*;

/**
 * Marks a class as a structured Integration Test Suite with managed orchestration.
 *
 * <p>This annotation configures a suite to follow a "Gatekeeper" execution pattern:
 * <ol>
 * <li><b>Phase 1 (Authentication):</b> Classes marked with {@link AuthTestPhase}
 * run first and sequentially.</li>
 * <b>Gate Check:</b> If all tests in the Auth phase pass, the suite proceeds.
 * If any fail, all later tests in the suite are skipped.
 * <li><b>Phase 2 (Domain):</b> Classes marked with {@link DomainTestPhase}
 * run in parallel to maximize performance.</li>
 * </ol>
 *
 * <h3>Usage:</h3>
 * <pre>
 * &#64;IntegrationSuite
 * public class MyClientSuite {
 *
 *  &#64;Nested
 *  &#64;AuthTestPhase
 *  class AuthTests { ... }
 *
 *  &#64;Nested
 *  &#64;DomainTestPhase
 *  class SomeDomainTests { ... }
 *
 * }
 * </pre>
 *
 * @see IntegrationSuiteOrchestrator
 * @see AuthTestPhase
 * @see DomainTestPhase
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(IntegrationSuiteOrchestrator.class)
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public @interface IntegrationSuite {
}
