package org.haikuos.haikudepotserver.dataobjects.auto;

import java.util.Date;

import org.haikuos.haikudepotserver.dataobjects.support.AbstractDataObject;

/**
 * Class _Response was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _Response extends AbstractDataObject {

    public static final String CREATE_TIMESTAMP_PROPERTY = "createTimestamp";
    public static final String RESPONSE_PROPERTY = "response";
    public static final String TOKEN_PROPERTY = "token";

    public static final String ID_PK_COLUMN = "id";

    public void setCreateTimestamp(Date createTimestamp) {
        writeProperty(CREATE_TIMESTAMP_PROPERTY, createTimestamp);
    }
    public Date getCreateTimestamp() {
        return (Date)readProperty(CREATE_TIMESTAMP_PROPERTY);
    }

    public void setResponse(String response) {
        writeProperty(RESPONSE_PROPERTY, response);
    }
    public String getResponse() {
        return (String)readProperty(RESPONSE_PROPERTY);
    }

    public void setToken(String token) {
        writeProperty(TOKEN_PROPERTY, token);
    }
    public String getToken() {
        return (String)readProperty(TOKEN_PROPERTY);
    }

}
