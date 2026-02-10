package com.github.waitlight.asskicker.logging;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.reactivestreams.Subscription;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

@Configuration
public class MdcContextLifterConfiguration {
    private static final String MDC_HOOK_KEY = "mdcContextLifter";

    @PostConstruct
    public void setupHook() {
        Hooks.onEachOperator(MDC_HOOK_KEY, Operators.lift((sc, subscriber) -> new MdcContextLifter<>(subscriber)));
    }

    @PreDestroy
    public void cleanupHook() {
        Hooks.resetOnEachOperator(MDC_HOOK_KEY);
    }

    private static void copyToMdc(ContextView contextView) {
        if (contextView.hasKey(RequestIdFilter.REQUEST_ID_KEY)) {
            MDC.put(RequestIdFilter.REQUEST_ID_KEY, contextView.get(RequestIdFilter.REQUEST_ID_KEY));
        } else {
            MDC.remove(RequestIdFilter.REQUEST_ID_KEY);
        }
    }

    private static void clearMdc() {
        MDC.remove(RequestIdFilter.REQUEST_ID_KEY);
    }

    private static class MdcContextLifter<T> implements CoreSubscriber<T> {
        private final CoreSubscriber<? super T> delegate;

        private MdcContextLifter(CoreSubscriber<? super T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            delegate.onSubscribe(subscription);
        }

        @Override
        public void onNext(T value) {
            copyToMdc(delegate.currentContext());
            try {
                delegate.onNext(value);
            } finally {
                clearMdc();
            }
        }

        @Override
        public void onError(Throwable throwable) {
            copyToMdc(delegate.currentContext());
            try {
                delegate.onError(throwable);
            } finally {
                clearMdc();
            }
        }

        @Override
        public void onComplete() {
            copyToMdc(delegate.currentContext());
            try {
                delegate.onComplete();
            } finally {
                clearMdc();
            }
        }

        @Override
        public Context currentContext() {
            return delegate.currentContext();
        }
    }
}
