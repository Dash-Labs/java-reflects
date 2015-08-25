package com.dashlabs.dash;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

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
