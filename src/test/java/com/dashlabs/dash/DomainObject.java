package com.dashlabs.dash;

/**
 * User: blangel
 * Date: 2/2/15
 * Time: 3:06 PM
 */
public class DomainObject {

    private final String id;

    private final Long dateTime;

    private final String metadata;

    public DomainObject(String id, Long dateTime, String metadata) {
        this.id = id;
        this.dateTime = dateTime;
        this.metadata = metadata;
    }

    public String getId() {
        return id;
    }

    public Long getDateTime() {
        return dateTime;
    }

    public String getMetadata() {
        return metadata;
    }
}
