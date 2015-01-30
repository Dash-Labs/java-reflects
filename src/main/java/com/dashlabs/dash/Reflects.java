package com.dashlabs.dash;

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

    }

    public static class FieldValue {

        private final String fieldName;

        private final Object fieldValue;

        public FieldValue(String fieldName, Object fieldValue) {
            this.fieldName = fieldName;
            this.fieldValue = fieldValue;
        }
    }

    @SuppressWarnings("rawtypes")
    public static <T> T newType(Class<T> type, Object ... alreadyInstantiated) {
        Map<Class<?>, Object> alreadyInstantiatedMapping = map(alreadyInstantiated);
        List<FieldValue> fieldValues = fieldMappings(alreadyInstantiated);
        T result = null;
        for (Constructor constructor : type.getConstructors()) {
            Class<?>[] types = constructor.getParameterTypes();
            if (types.length < 1) {
                continue;
            }
            List<Object> objects = new ArrayList<>(types.length);
            for (Class<?> parameterType : types) {
                if (alreadyInstantiatedMapping.containsKey(parameterType)) {
                    objects.add(alreadyInstantiatedMapping.get(parameterType));
                } else {
                    objects.add(randomValue(parameterType));
                }
            }
            Object[] parameters = objects.toArray();
            try {
                result = type.cast(constructor.newInstance(parameters));
            } catch (Throwable throwable) {
                if (throwable instanceof InvocationTargetException) {
                    throwable = ((InvocationTargetException) throwable).getTargetException();
                }
                throw new RuntimeException(throwable);
            }
        }
        if (result == null) {
            try {
                return type.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        for (FieldValue fieldValue : fieldValues) {
            try {
                Field field = type.getDeclaredField(fieldValue.fieldName);
                field.setAccessible(true);
                field.set(result, fieldValue.fieldValue);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
        return result;
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

    private static Object randomValue(Class<?> type) {
        if (String.class == type) {
            return UUID.randomUUID().toString();
        } else if (Boolean.class == type) {
            return false;
        } else if (Byte.class == type) {
            return (byte) Math.round(Math.random());
        } else if (Character.class == type) {
            return (char) Math.round(Math.random());
        } else if (Short.class == type) {
            return (short) Math.round(Math.random());
        } else if (Integer.class == type) {
            return (int) Math.round(Math.random());
        } else if (Long.class == type) {
            return Math.round(Math.random());
        } else if (Float.class == type) {
            return (float) Math.random();
        } else if (Double.class == type) {
            return Math.random();
        } else if (AtomicBoolean.class == type) {
            return new AtomicBoolean(false);
        } else if (AtomicInteger.class == type) {
            return new AtomicInteger((int) Math.round(Math.random()));
        } else if (AtomicLong.class == type) {
            return new AtomicLong(Math.round(Math.random()));
        } else if (BigInteger.class == type) {
            return new BigInteger(String.valueOf(Math.round(Math.random())));
        } else if (BigDecimal.class == type) {
            return new BigDecimal(Math.random());
        } else if (type.isPrimitive()) {
            if (boolean.class == type) {
                return false;
            } else if (byte.class == type) {
                return (byte) Math.round(Math.random());
            } else if (char.class == type) {
                return (char) Math.round(Math.random());
            } else if (short.class == type) {
                return (short) Math.round(Math.random());
            } else if (int.class == type) {
                return (int) Math.round(Math.random());
            } else if (long.class == type) {
                return Math.round(Math.random());
            } else if (float.class == type) {
                return (float) Math.random();
            } else if (double.class == type) {
                return Math.random();
            } else {
                throw new AssertionError("Unknown primitive (or unsupported; i.e., void) " + type);
            }
        } else if (Date.class == type) {
            return new Date();
        } else if (List.class == type) {
            return new ArrayList<>();
        } else if (Set.class == type) {
            return new HashSet<>();
        } else if (Map.class == type) {
            return new HashMap<>();
        } else if (Collection.class == type) {
            return new ArrayList<>();
        } else if (type.isArray()) {
            return Array.newInstance(type.getComponentType(), 1);
        } else if (type.isEnum()) {
            Object[] values = type.getEnumConstants();
            if ((values == null) || (values.length < 1)) {
                return null;
            }
            return values[0];
        } else {
            return newType(type);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Method<T> method(String named, Object on, Class<?> ... parameterTypes) {
        try {
            java.lang.reflect.Method method = on.getClass().getDeclaredMethod(named, parameterTypes);
            method.setAccessible(true);
            return new Method<>(on, (Class<T>) method.getReturnType(), method);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Reflects() { }

}
