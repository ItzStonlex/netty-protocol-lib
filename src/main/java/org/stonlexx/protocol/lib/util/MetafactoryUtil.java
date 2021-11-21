package org.stonlexx.protocol.lib.util;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

import java.lang.invoke.*;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.function.Supplier;

@UtilityClass
@SuppressWarnings("unchecked")
public class MetafactoryUtil {

    public Method checkLambda(Class<?> lambdaType) {
        if (!lambdaType.isInterface()) {
            throw new IllegalArgumentException("lambdaType must be an interface");
        }

        Method[] lambdaMethods = lambdaType.getMethods();
        Method found = null;
        int i = 0;

        for (Method method : lambdaMethods) {
            if (Modifier.isAbstract(method.getModifiers())) {
                if (i >= 1) {
                    throw new IllegalStateException("Too many methods in lambda");
                }

                found = method;

                i++;
            }
        }

        if (found == null) {
            throw new IllegalStateException("Can't find methods");
        }

        return found;
    }

    public Method checkCanBeLambda(WrappedObject executable, Class<?> lambdaType) {
        Method lambdaMethod = checkLambda(lambdaType);

        if (!executable.getReturnType().isAssignableFrom(lambdaMethod.getReturnType())) {
            throw new IllegalArgumentException("lambda must return a " + executable.getReturnType() + ", but it returns " + lambdaMethod.getReturnType());
        }

        Class<?>[] params = executable.getParameterTypes();
        Class<?>[] lambdaParams = lambdaMethod.getParameterTypes();

        boolean failed = false;

        if (params.length != lambdaParams.length) {
            failed = true;
        } else {
            for (int i = 0; i < params.length; i++) {
                Class<?> oldClass = params[i];
                Class<?> newClass = lambdaParams[i];

                if (!newClass.isAssignableFrom(oldClass)) {
                    failed = true;
                    break;
                }
            }
        }

        if (failed) {
            throw new IllegalArgumentException("lambda must to have " + Arrays.toString(params) + " as params, but it has " + Arrays.toString(lambdaParams));
        }

        return lambdaMethod;
    }

    public <T> Supplier<T> objectConstructor(@NonNull Class<T> cls) throws Exception {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            MethodType lambdaMethodType = MethodType.methodType(Object.class);
            MethodType constructorType = MethodType.methodType(void.class);

            MethodHandle constructorTarget = lookup.findConstructor(cls, constructorType);

            CallSite callSite = LambdaMetafactory.metafactory(
                    lookup, "get",
                    MethodType.methodType(Supplier.class),
                    lambdaMethodType, constructorTarget, lambdaMethodType
            );

            return (Supplier<T>) callSite.getTarget().invoke();
        } catch (Throwable e) {
            throw new Exception(e);
        }
    }

    public <Lambda> Lambda objectConstructor(@NonNull Constructor<?> constructor, @NonNull Class<Lambda> lambdaType) throws Exception {
        Method lambdaMethod = checkCanBeLambda(new WrappedObject(constructor, ExecutableType.CONSTRUCTOR), lambdaType);

        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            MethodType lambdaMethodType = MethodType.methodType(constructor.getDeclaringClass(), constructor.getParameterTypes());
            MethodType constructorType = MethodType.methodType(void.class, constructor.getParameterTypes());

            MethodHandle constructorTarget = lookup.findConstructor(constructor.getDeclaringClass(), constructorType);

            CallSite callSite = LambdaMetafactory.metafactory(
                    lookup, lambdaMethod.getName(),
                    MethodType.methodType(lambdaType),
                    lambdaMethodType, constructorTarget, lambdaMethodType
            );

            return (Lambda) callSite.getTarget().invoke();
        } catch (Throwable e) {
            throw new Exception(e);
        }
    }

    public <Lambda> Lambda methodInvoker(@NonNull Object object, @NonNull Method method, @NonNull Class<Lambda> lambdaType) throws Exception {
        Method lambdaMethod = checkCanBeLambda(new WrappedObject(method, ExecutableType.METHOD), lambdaType);

        if (Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException("Method mustn't be static");
        }

        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            MethodHandle target = lookup.unreflect(method);

            MethodType type = target.type();
            MethodType withoutObjectType = type.dropParameterTypes(0, 1);

            CallSite callSite = LambdaMetafactory.metafactory(
                    lookup, lambdaMethod.getName(),
                    MethodType.methodType(lambdaType, type.parameterType(0)),
                    MethodType.methodType(type.returnType(), withoutObjectType.parameterArray()),
                    target, withoutObjectType
            );

            return (Lambda) callSite.getTarget().invoke(object);
        } catch (Throwable e) {
            throw new Exception(e);
        }
    }

    public <Lambda> Lambda staticMethodInvoker(Method method, Class<Lambda> lambdaType) throws Exception {
        Method lambdaMethod = checkCanBeLambda(new WrappedObject(method, ExecutableType.METHOD), lambdaType);

        if (!Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException("Method must be static");
        }

        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            MethodHandle target = lookup.unreflect(method);
            MethodType type = target.type();

            CallSite callSite = LambdaMetafactory.metafactory(
                    lookup, lambdaMethod.getName(),
                    MethodType.methodType(lambdaType),
                    type, target, type
            );

            return (Lambda) callSite.getTarget().invoke();
        } catch (Throwable e) {
            throw new Exception(e);
        }
    }

    @RequiredArgsConstructor
    private class WrappedObject {

        private final Object handle;
        private final ExecutableType type;

        public Class<?> getReturnType() {
            switch (type) {
                case CONSTRUCTOR:
                    return ((Constructor<?>) handle).getDeclaringClass();
                default:
                case SETTER:
                    return void.class;
                case GETTER:
                    return ((Field) handle).getType();
                case METHOD:
                    return ((Method) handle).getReturnType();
            }
        }

        public Class<?>[] getParameterTypes() {
            if (handle instanceof Executable) {
                return ((Executable) handle).getParameterTypes();
            }

            Field field = (Field) handle;

            switch (type) {
                case SETTER:
                    return new Class<?>[]{field.getType()};
                default:
                case GETTER:
                    return new Class<?>[0];
            }
        }

    }

    public enum ExecutableType {
        CONSTRUCTOR, SETTER, GETTER, METHOD
    }

}
