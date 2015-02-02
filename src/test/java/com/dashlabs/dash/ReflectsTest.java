package com.dashlabs.dash;

import org.junit.Test;

import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * User: blangel
 * Date: 2/2/15
 * Time: 3:05 PM
 */
public class ReflectsTest {

    @Test
    public void newType() {
        DomainObject value = Reflects.newType(DomainObject.class);
        assertNotNull(value);
        assertNotNull(value.getId());
        assertNotNull(value.getDateTime());
        assertNotNull(value.getMetadata());

        String defined = UUID.randomUUID().toString();
        value = Reflects.newType(DomainObject.class, new Reflects.FieldValue("id", defined));
        assertNotNull(value);
        assertEquals(defined, value.getId());
        assertNotNull(value.getDateTime());
        assertNotNull(value.getMetadata());
    }

}
