package com.dashlabs.dash;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: blangel
 * Date: 11/14/14
 * Time: 5:16 PM
 */
public final class Reflects {

    public static class Method<T> {

        private final Object owner;

        private final Class<T> returnType;

        private final java.lang.reflect.Method method;

        private Method(Object owner, Class<T> returnType, java.lang.reflect.Method method) {
            this.owner = owner;
            this.returnType = returnType;
            this.method = method;
        }

        @SuppressWarnings("unchecked")
        public T invoke(Object ... parameters) {
            try {
                Object result = method.invoke(owner, parameters);
                if (returnType.isPrimitive()) {
                    return (T) result;
                }
                return (result == null ? null : returnType.cast(result));
            } catch (Throwable throwable) {
                if (throwable instanceof InvocationTargetException) {
                    throwable = ((InvocationTargetException) throwable).getTargetException();
                }
                throw new RuntimeException(throwable);
            }
        }

        /**
         * Use this method to invoke a method which takes one parameter which is an object array
         * @param parameters which are the object array
         * @return the result of calling the method
         */
        public T varArgsInvoke(Object ... parameters) {
            if ((parameters == null) || (parameters.length < 1) || (parameters[0] == null)) {
                return invoke(new Object[] { parameters });
            }
            Class<?> argType = parameters[0].getClass();
            Object[] array = (Object[]) Array.newInstance(argType, 1);
            array[0] = parameters;
            return invoke(array);
        }

    }

    public static class FieldValue {

        private final String fieldName;

        private final Object fieldValue;

        public FieldValue(String fieldName, Object fieldValue) {
            this.fieldName = fieldName;
            this.fieldValue = fieldValue;
        }
    }

    public static class Builder<T> {

        private final Class<T> type;

        private final List<Object> alreadyInstantiated;

        public Builder(Class<T> type) {
            this.type = type;
            this.alreadyInstantiated = new LinkedList<>();
        }

        public Builder<T> with(String fieldName, Object fieldValue) {
            alreadyInstantiated.add(new FieldValue(fieldName, fieldValue));
            return this;
        }

        public Builder<T> with(Object alreadyInstantiated) {
            this.alreadyInstantiated.add(alreadyInstantiated);
            return this;
        }

        public Builder<T> withNull(String fieldName) {
            return with(fieldName, null);
        }

        public T build() {
            return Reflects.newType(type, false, alreadyInstantiated.toArray(new Object[alreadyInstantiated.size()]));
        }

        /**
         * All types not specifically specified with calls to {@linkplain #with(String, Object)} or {@linkplain #with(Object)}
         * will be given null values (or the default value if the type is a primitive).
         * @return the built object
         */
        public T buildRestNullOrDefault() {
            return Reflects.newType(type, true, alreadyInstantiated.toArray(new Object[alreadyInstantiated.size()]));
        }

    }

    public static <T> Builder<T> construct(Class<T> type) {
        return new Builder<>(type);
    }

    @SuppressWarnings("rawtypes")
    private static <T> T newType(Class<T> type, boolean nullOrDefault, Object ... alreadyInstantiated) {
        if (type.isEnum()) {
            return type.cast(randomValue(type));
        }
        Map<Class<?>, Object> alreadyInstantiatedMapping = map(alreadyInstantiated);
        List<FieldValue> fieldValues = fieldMappings(alreadyInstantiated);
        T result = construct(type, alreadyInstantiatedMapping, true, nullOrDefault);
        if (result == null) {
            try {
                result = type.newInstance();
            } catch (Exception e) {
                // one last try, ignored no-args private constructors above, retry
                result = construct(type, alreadyInstantiatedMapping, false, nullOrDefault);
                if (result == null) {
                    throw new RuntimeException(String.format("Could not construct type %s", type.getName()), e);
                }
            }
        }
        for (FieldValue fieldValue : fieldValues) {
            try {
                AtomicReference<NoSuchFieldException> notFound = new AtomicReference<>(null);
                Class<?> typeHierarchy = type;
                while (typeHierarchy != null) {
                    try {
                        Field field = typeHierarchy.getDeclaredField(fieldValue.fieldName);
                        field.setAccessible(true);
                        field.set(result, fieldValue.fieldValue);
                        notFound.set(null);
                        break;
                    } catch (NoSuchFieldException nsfe) {
                        if (notFound.get() == null) {
                            notFound.set(nsfe);
                        }
                        typeHierarchy = typeHierarchy.getSuperclass();
                    }
                }
                if (notFound.get() != null) {
                    throw notFound.get();
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
        return result;
    }

    private static <T> T construct(Class<T> type, Map<Class<?>, Object> alreadyInstantiatedMapping, boolean ignoreNoArgs,
                                   boolean useNullOrDefault) {
        T result = null;
        // choose the constructor with the most arguments first
        Constructor<?>[] constructors = type.getDeclaredConstructors();
        List<Constructor<?>> sortedConstructors = new ArrayList<>(constructors.length);
        Collections.addAll(sortedConstructors, constructors);
        Collections.sort(sortedConstructors, new Comparator<Constructor<?>>() {
            @Override public int compare(Constructor<?> left, Constructor<?> right) {
                int leftLength = left.getParameterTypes() == null ? 0 : left.getParameterTypes().length;
                int rightLength = right.getParameterTypes() == null ? 0 : right.getParameterTypes().length;
                return Integer.valueOf(leftLength).compareTo(rightLength);
            }
        });
        for (Constructor<?> constructor : sortedConstructors) {
            try {
                result = construct(type, constructor, alreadyInstantiatedMapping, ignoreNoArgs, useNullOrDefault);
                if (result != null) {
                    break;
                }
            } catch (Throwable t) {
                result = null; // try the next, if any
            }
        }
        return result;
    }

    private static <T> T construct(Class<T> type, Constructor<?> constructor, Map<Class<?>, Object> alreadyInstantiatedMapping,
                                   boolean ignoreNoArgs, boolean useNullOrDefault) {
        constructor.setAccessible(true);
        Class<?>[] types = constructor.getParameterTypes();
        if (ignoreNoArgs && (types.length < 1)) {
            return null;
        }
        List<Object> objects = new ArrayList<>(types.length);
        if (types.length > 0) {
            for (Class<?> parameterType : types) {
                if (alreadyInstantiatedMapping.containsKey(parameterType)) {
                    objects.add(alreadyInstantiatedMapping.get(parameterType));
                } else if (!useNullOrDefault) {
                    objects.add(randomValue(parameterType));
                } else {
                    objects.add(nullOrDefault(parameterType));
                }
            }
        }
        Object[] parameters = objects.toArray();
        try {
            return type.cast(constructor.newInstance(parameters));
        } catch (Throwable throwable) {
            if (throwable instanceof InvocationTargetException) {
                throwable = ((InvocationTargetException) throwable).getTargetException();
            }
            throw new RuntimeException(String.format("Could not construct type %s", type.getName()), throwable);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Class<?>, Object> map(Object[] values) {
        if ((values == null) || (values.length < 1)) {
            return Collections.emptyMap();
        }
        Map<Class<?>, Object> map = new HashMap<>(values.length, 1.0f);
        for (Object obj : values) {
            if (obj instanceof FieldValue) {
                continue;
            }
            Class<?> type = obj.getClass();
            while (type != Object.class) {
                map.put(type, obj);
                Class<?>[] interfaces = type.getInterfaces();
                for (Class<?> interfaceType : interfaces) {
                    map.put(interfaceType, obj);
                }
                type = type.getSuperclass();
            }
        }
        return map;
    }

    private static List<FieldValue> fieldMappings(Object[] values) {
        if ((values == null) || (values.length < 1)) {
            return Collections.emptyList();
        }
        List<FieldValue> fieldValues = new ArrayList<>(values.length);
        for (Object obj : values) {
            if (obj instanceof FieldValue) {
                fieldValues.add((FieldValue) obj);
            }
        }
        return fieldValues;
    }

    private static Object nullOrDefault(Class<?> type) {
        if (type.isPrimitive()) {
            if (boolean.class == type) {
                return false;
            } else if (byte.class == type) {
                return (byte) 0;
            } else if (char.class == type) {
                return (char) 0;
            } else if (short.class == type) {
                return (short) 0;
            } else if (int.class == type) {
                return 0;
            } else if (long.class == type) {
                return 0L;
            } else if (float.class == type) {
                return 0f;
            } else if (double.class == type) {
                return 0d;
            } else {
                throw new AssertionError("Unknown primitive (or unsupported; i.e., void) " + type);
            }
        }
        return null;
    }

    private static Object randomValue(Class<?> type) {
        if (String.class == type) {
            return UUID.randomUUID().toString();
        } else if (Boolean.class == type) {
            return (Math.round(Math.random()) == 0L);
        } else if (Byte.class == type) {
            return (byte) Math.round(Math.random() * 100d);
        } else if (Character.class == type) {
            return (char) Math.round(Math.random() * 100d);
        } else if (Short.class == type) {
            return (short) Math.round(Math.random() * 100d);
        } else if (Integer.class == type) {
            return (int) Math.round(Math.random() * 1000d);
        } else if (Long.class == type) {
            return Math.round(Math.random() * 1000000d);
        } else if (Float.class == type) {
            return (float) Math.random();
        } else if (Double.class == type) {
            return Math.random();
        } else if (AtomicBoolean.class == type) {
            return new AtomicBoolean(Math.round(Math.random()) == 0L);
        } else if (AtomicInteger.class == type) {
            return new AtomicInteger((int) Math.round(Math.random() * 1000d));
        } else if (AtomicLong.class == type) {
            return new AtomicLong(Math.round(Math.random() * 1000000d));
        } else if (BigInteger.class == type) {
            return new BigInteger(String.valueOf((int) Math.round(Math.random() * 1000d)));
        } else if (BigDecimal.class == type) {
            return new BigDecimal(Math.random());
        } else if (type.isPrimitive()) {
            if (boolean.class == type) {
                return (Math.round(Math.random()) == 0L);
            } else if (byte.class == type) {
                return (byte) Math.round(Math.random() * 100d);
            } else if (char.class == type) {
                return (char) Math.round(Math.random() * 100d);
            } else if (short.class == type) {
                return (short) Math.round(Math.random() * 100d);
            } else if (int.class == type) {
                return (int) Math.round(Math.random() * 1000d);
            } else if (long.class == type) {
                return Math.round(Math.random() * 1000000d);
            } else if (float.class == type) {
                return (float) Math.random();
            } else if (double.class == type) {
                return Math.random();
            } else {
                throw new AssertionError("Unknown primitive (or unsupported; i.e., void) " + type);
            }
        } else if ((Date.class == type) || Date.class.isAssignableFrom(type)) {
            return new Date();
        } else if (List.class == type) {
            return new ArrayList<>();
        } else if (Set.class == type) {
            return new HashSet<>();
        } else if (Map.class == type) {
            return new HashMap<>();
        } else if (Collection.class == type) {
            return new ArrayList<>();
        } else if (ImmutableList.class == type) {
            return ImmutableList.of();
        } else if (ImmutableSet.class == type) {
            return ImmutableSet.of();
        } else if (ImmutableMap.class == type) {
            return ImmutableMap.of();
        } else if (ImmutableCollection.class == type) {
            return ImmutableList.of();
        } else if (type.isArray()) {
            return Array.newInstance(type.getComponentType(), 1);
        } else if (type.isEnum()) {
            Object[] values = type.getEnumConstants();
            if ((values == null) || (values.length < 1)) {
                return null;
            }
            return values[new Random().nextInt(values.length)];
        } else if (type.isInterface()) {
            return null;
        } else if ((TimeZone.class == type) || TimeZone.class.isAssignableFrom(type)) {
            return TimeZone.getDefault();
        } else if ((Locale.class == type)|| Locale.class.isAssignableFrom(type)) {
            return Locale.getDefault();
        } else if ((Calendar.class == type) || Calendar.class.isAssignableFrom(type)) {
            return Calendar.getInstance();
        } else {
            return newType(type, false);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Method<T> method(String named, Object on, Class<?> ... parameterTypes) {
        return method(named, on, on.getClass(), parameterTypes);
    }

    @SuppressWarnings("unchecked")
    private static <T> Method<T> method(String named, Object on, Class<?> type, Class<?> ... parameterTypes) {
        try {
            java.lang.reflect.Method method = type.getDeclaredMethod(named, parameterTypes);
            method.setAccessible(true);
            return new Method<>(on, (Class<T>) method.getReturnType(), method);
        } catch (NoSuchMethodException nsme) {
            if (type != Object.class) {
                return method(named, on, type.getSuperclass(), parameterTypes);
            }
            throw new RuntimeException(nsme);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Method<T> staticMethod(String named, Class<?> on, Class<?> ... parameterTypes) {
        try {
            java.lang.reflect.Method method = on.getDeclaredMethod(named, parameterTypes);
            method.setAccessible(true);
            return new Method<>(null, (Class<T>) method.getReturnType(), method);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Syntactic sugar to simulate var-args when used in a non-end-parameter case; e.g.
     * <pre>
     *     public void foo(Object[] varArgWannaBe, Object ... varArg) { ... }
     * </pre>
     * Method {@literal foo} could be invoked via
     * <pre>
     *     foo(varArgs(obj1, obj2), obj3, obj4);
     * </pre>
     *
     * @param parameters to return
     * @return the inputted parameters
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> T[] varArgs(T ... parameters) {
        return parameters;
    }

    private Reflects() { }

}
