package com.macmario.services.web.tcat;


/**
 *
 * @author SuMario
 */
public class TCatSSLHostConfigCertificate extends org.apache.tomcat.util.net.SSLHostConfigCertificate {

    public TCatSSLHostConfigCertificate(TCatSSLHostConfig shost, Type type, TCat tcat) {
        super(shost,type);
    }
    
}
