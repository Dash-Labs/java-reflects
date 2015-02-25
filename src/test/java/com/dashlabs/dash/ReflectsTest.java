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

}
