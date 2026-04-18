package org.novitzkee.rocketchatclient.orchestration;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.support.AnnotationSupport;

import java.lang.annotation.Annotation;

@Slf4j
public class IntegrationSuiteOrchestrator implements TestWatcher, AfterAllCallback, ExecutionCondition {

    static final String AUTH_PHASE_LOCK = "authentication_lock";

    static final int AUTH_PHASE_FIRST = 1;

    static final int DOMAIN_PHASE_AFTER_AUTH_PHASE = 2;

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(IntegrationSuiteOrchestrator.class);

    private static final String AUTH_STATE_KEY = "AUTH_STATE";

    private enum State {PASSED, FAILED}

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        if (isAuthTestPhase(context)) {
            setFailedAuthPhaseState(context);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (isAuthTestPhase(context) && getAuthPhaseState(context) != State.FAILED) {
            getStore(context).put(AUTH_STATE_KEY, State.PASSED);
        }
    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (isSuiteRoot(context)) {
            return ConditionEvaluationResult.enabled("Global suite container is always enabled.");
        }

        if (isAuthTestPhase(context)) {
            return ConditionEvaluationResult.enabled("Auth test phase always runs.");
        }

        if (getAuthPhaseState(context) == State.PASSED) {
            return ConditionEvaluationResult.enabled("Auth successful for this global suite.");
        }

        if (isDomainTestPhase(context)) {
            return ConditionEvaluationResult.disabled("Skipping: Authentication phase failed or was not found.");
        }

        return ConditionEvaluationResult.enabled("Test is not either of authentication or domain phase.");
    }

    private void setFailedAuthPhaseState(ExtensionContext context) {
        getStore(context).put(AUTH_STATE_KEY, State.FAILED);
    }

    private State getAuthPhaseState(ExtensionContext context) {
        return getStore(context).get(AUTH_STATE_KEY, State.class);
    }

    private ExtensionContext.Store getStore(ExtensionContext context) {
        for (ExtensionContext current = context; getParent(current) != null; current = getParent(current)) {
            if (isSuiteRoot(current)) {
                return current.getStore(NAMESPACE);
            }
        }

        throw new IllegalStateException("No suite root found in current test context.");
    }

    private boolean isSuiteRoot(ExtensionContext context) {
        return isClassOfPhaseWithType(context, IntegrationSuite.class);
    }

    private boolean isAuthTestPhase(ExtensionContext context) {
        return isTestOfPhaseWithType(context, AuthTestPhase.class);
    }

    private boolean isDomainTestPhase(ExtensionContext context) {
        return isTestOfPhaseWithType(context, DomainTestPhase.class);
    }

    private boolean isTestOfPhaseWithType(ExtensionContext context, Class<? extends Annotation> phaseAnnotation) {
        return isClassOfPhaseWithType(context, phaseAnnotation) || isMethodOfPhaseWithType(context, phaseAnnotation);
    }

    private boolean isClassOfPhaseWithType(ExtensionContext context, Class<? extends Annotation> phaseAnnotation) {
        return AnnotationSupport.isAnnotated(context.getElement(), phaseAnnotation);
    }

    private boolean isMethodOfPhaseWithType(ExtensionContext context, Class<? extends Annotation> phaseAnnotation) {
        return context.getParent()
                .map(parent -> AnnotationSupport.isAnnotated(parent.getElement(), phaseAnnotation))
                .orElse(false);
    }

    private ExtensionContext getParent(ExtensionContext context) {
        return context.getParent().orElse(null);
    }
}
