package com.macmario.services.web.tcat;

/**
 *
 * @author SuMario
 */
public class TCatSSLHostConfig extends org.apache.tomcat.util.net.SSLHostConfig{

    private final TCat tcat;
    private Type type;

    TCatSSLHostConfig(TCat tcat) {
        super();
        this.tcat=tcat;
        this.type=Type.JSSE;
    }
    
}
