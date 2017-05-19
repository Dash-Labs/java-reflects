package com.dashlabs.dash;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import static junit.framework.Assert.*;

/**
 * User: blangel
 * Date: 2/2/15
 * Time: 3:05 PM
 */
public class ReflectsTest {

    @Test
    public void newType() {
        DomainObject value = Reflects.construct(DomainObject.class).build();
        assertNotNull(value);
        assertNotNull(value.getId());
        assertNotNull(value.getDateTime());
        assertNotNull(value.getMetadata());

        String defined = UUID.randomUUID().toString();
        value = Reflects.construct(DomainObject.class).with("id", defined).build();
        assertNotNull(value);
        assertEquals(defined, value.getId());
        assertNotNull(value.getDateTime());
        assertNotNull(value.getMetadata());

        value = Reflects.construct(DomainObject.class).with("id", defined).buildRestNullOrDefault();
        assertNotNull(value);
        assertEquals(defined, value.getId());
        assertNull(value.getDateTime());
        assertNull(value.getMetadata());
    }

    @Test
    public void nullOrDefault() throws Exception {
        Method nullOrDefaultMethod = Reflects.class.getDeclaredMethod("nullOrDefault", Class.class);
        nullOrDefaultMethod.setAccessible(true);

        ImmutableList<?> list = (ImmutableList) nullOrDefaultMethod.invoke(null, ImmutableList.class);
        assertNull(list);

        String result = (String) nullOrDefaultMethod.invoke(null, String.class);
        assertNull(result);

        byte byteValue = (byte) nullOrDefaultMethod.invoke(null, byte.class);
        assertEquals(0, byteValue);

        char charValue = (char) nullOrDefaultMethod.invoke(null, char.class);
        assertEquals(0, charValue);

        short shortValue = (short) nullOrDefaultMethod.invoke(null, short.class);
        assertEquals(0, shortValue);

        int intValue = (int) nullOrDefaultMethod.invoke(null, int.class);
        assertEquals(0, intValue);

        long longValue = (long) nullOrDefaultMethod.invoke(null, long.class);
        assertEquals(0L, longValue);

        float floatValue = (float) nullOrDefaultMethod.invoke(null, float.class);
        assertEquals(0f, floatValue);

        double doubleValue = (double) nullOrDefaultMethod.invoke(null, double.class);
        assertEquals(0d, doubleValue);

        try {
            nullOrDefaultMethod.invoke(null, void.class);
            fail("Expecting an AssertionError as void is not supported");
        } catch (InvocationTargetException ite) {
            assertEquals(AssertionError.class, ite.getCause().getClass());
        }

    }

    @Test
    public void randomValue() throws Exception {
        Method randomValueMethod = Reflects.class.getDeclaredMethod("randomValue", Class.class);
        randomValueMethod.setAccessible(true);

        ImmutableList<?> list = (ImmutableList) randomValueMethod.invoke(null, ImmutableList.class);
        assertNotNull(list);

        ImmutableSet<?> set = (ImmutableSet) randomValueMethod.invoke(null, ImmutableSet.class);
        assertNotNull(set);

        ImmutableMap<?, ?> map = (ImmutableMap) randomValueMethod.invoke(null, ImmutableMap.class);
        assertNotNull(map);

        ImmutableCollection<?> collection = (ImmutableCollection) randomValueMethod.invoke(null, ImmutableCollection.class);
        assertNotNull(collection);
    }

    @Test
    public void varArgs() {
        assertNull(Reflects.varArgs((String[]) null));

        String[] varArgs = Reflects.varArgs("String1", "String2");
        assertEquals(2, varArgs.length);
        assertEquals("String1", varArgs[0]);
        assertEquals("String2", varArgs[1]);
    }

}
