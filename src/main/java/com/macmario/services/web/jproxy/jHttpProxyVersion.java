package com.macmario.services.web.jproxy;

import com.macmario.general.Version;

/**
 *
 * @author Sumario
 */
public class jHttpProxyVersion extends Version {
     public final String VERSION = "0.1";
     public final String SUBVERSION="alpha";    
    
     public final String SERVERNAME = "jHttpProxyServer";
     
     public String getServerIdentification() { return SERVERNAME+"/" + getServerVersion();}
     public String getServerVersion() { return VERSION; }
     public String getServerFullVersion() { return VERSION+SUBVERSION; }
        
}
