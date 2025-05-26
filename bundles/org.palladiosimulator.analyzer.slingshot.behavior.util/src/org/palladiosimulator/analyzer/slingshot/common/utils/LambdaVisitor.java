package org.palladiosimulator.analyzer.slingshot.common.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class LambdaVisitor<B, A> implements Function<B, A> {
    private final Map<Class<?>, Function<Object, A>> classToFunction = new HashMap<>();

    public <C> Acceptor<A, B, C> on(final Class<C> clazz) {
        return new Acceptor<>(this, clazz);
    }

    @Override
    public A apply(final Object o) {
        final Optional<Function<Object, A>> result = this.applyInternal(o.getClass());
        return result.map(function -> function.apply(o)).orElse(null);
    }

    private Optional<Function<Object, A>> applyInternal(final Class<?> clazz) {
        Function<Object, A> f = classToFunction.get(clazz);
        if (f == null) {
            for (final Class<?> interfaceClazz : clazz.getInterfaces()) {
                if (classToFunction.containsKey(interfaceClazz)) {
                    f = classToFunction.get(interfaceClazz);
                    break;
                }
            }
            if (f == null) {
                final Class<?> superClazz = clazz.getSuperclass();
                if (superClazz != null) {
                    return this.applyInternal(superClazz);
                } else {
                    return Optional.empty();
                }
            }
        }
        return Optional.of(f);
    }

    public static final class Acceptor<A, B, C> {
        @SuppressWarnings("rawtypes")
        private final LambdaVisitor visitor;
        private final Class<C> clazz;

        Acceptor(final LambdaVisitor<B, A> visitor, final Class<C> clazz) {
            this.visitor = visitor;
            this.clazz = clazz;
        }

        @SuppressWarnings("unchecked")
        public LambdaVisitor<B, A> then(final Function<C, A> f) {
            visitor.classToFunction.put(clazz, f);
            return visitor;
        }
    }
}