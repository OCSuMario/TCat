/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.macmario.services.web.tcat;

import static com.macmario.general.Version.getJavaCacerts;
import com.macmario.io.file.ReadFile;
import java.io.File;
import java.security.KeyStore;
import java.util.Properties;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.coyote.http2.Http2Protocol;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;

/**
 *
 * @author MNO
 */
public class TCatConnector extends org.apache.catalina.connector.Connector{

    private final TCat tcat;
    private final TCatCert cert;
    private final ProtocolHandler protocol;
    private String PROT="";
    private String Trust="";
    private String TrustPW="";
    private String Keys="";
    private String KeysPW="";
    private String Cipher="";
    
    private TCatConnector(TCat tcat) {
        this("org.apache.coyote.http11.Http11NioProtocol", tcat);
    }
    private TCatConnector(String proto, TCat tcat) {
        super(proto);
        this.protocol = getProtocolHandler();
        this.tcat=tcat;
        this.cert=tcat.cert;
    }
    public void configTLS(Properties ar) { configSSL(ar); }
    public void configSSL(Properties ar) {
        ar=updateHost(ar);
        setHost( ar.getProperty("HOST", tcat._defHost), tcat.getInt(ar.getProperty("PORT", ""+tcat._defPort)));
        
        //Connector connector = new Connector();
            Http11NioProtocol protocol = (Http11NioProtocol) getProtocolHandler();

            this.Keys=ar.getProperty("KEYSTORE", cert.getDefaultKeyStore().getAbsolutePath());
            this.KeysPW=ar.getProperty("KEYSTOREPW", tcat.getDefaultPass());
            if ( KeysPW.isEmpty()  ||  KeysPW.equals("<default>") ) { KeysPW  = tcat.getDefaultPass(); }
            
            File keystore = new File(Keys); //(new ReadFile("store")).getFile();
            
             this.Trust=ar.getProperty("TRUSTSTORE", getJavaCacerts().getAbsolutePath());
             if ( this.Trust.isEmpty() ) { this.Trust=getJavaCacerts().getAbsolutePath();}
             log(1, "truststore config =>"+this.Trust+"<=");
             this.TrustPW=ar.getProperty("TRUSTSTOREPW", "changeit");
             if ( TrustPW.isEmpty() || TrustPW.equals("<default>") ) { TrustPW = "changeit"; }
        
            File truststore = new File(this.Trust); //(new ReadFile("store")).getFile();
            
            setScheme("https");
            setSecure(true);           
            protocol.setSSLEnabled(true);
            
            SSLHostConfig sslHostConfig = new SSLHostConfig();
            SSLHostConfigCertificate sslCertificate = new SSLHostConfigCertificate(sslHostConfig, SSLHostConfigCertificate.Type.UNDEFINED);
            
            sslCertificate.setCertificateKeystoreFile(keystore.getAbsolutePath());
            sslCertificate.setCertificateKeystoreType(cert.getStoreAlg());
            sslCertificate.setCertificateKeystorePassword(KeysPW);
            sslCertificate.setCertificateKeyAlias("server");
            
            log(1, "truststore =>"+truststore.getAbsolutePath()+"<=");
            sslHostConfig.setTruststoreFile(truststore.getAbsolutePath());
            sslHostConfig.setTruststorePassword(TrustPW);

            sslHostConfig.addCertificate(sslCertificate);
            protocol.addSslHostConfig(sslHostConfig);
    }
    
    public void config(Properties ar) {
        ar=updateHost(ar);
        setHost( ar.getProperty("HOST", "localhost"), tcat.getInt(ar.getProperty("PORT", "37373")));
        
        setDiscardFacades(false);
    }
     
    private Properties updateHost(Properties ar) {
       ar = (ar == null )?new Properties():ar;
       String   HO=ar.getProperty("HOST");
       String  PUB=ar.getProperty("PUBLIC", "0");
       if ( HO == null ) {
                if ( PUB.equals("1") ) {
                    ar.put("HOST",tcat.getLocalIpFrom(tcat.getHostname()));
                } else {
                    ar.put("HOST","localhost");
                }
        }
        return ar;
    }
    
   
    
    private void setHost(String host, int port) {
        setProperty("address", host);
	setPort(port);
    }
    
    public void log(int deb, String msg){ tcat.log(deb, "TCATConnector::"+msg); }
    
    public static TCatConnector getInstance(TCat tcat, String proto, Properties props ) {
        TCatConnector conn = (proto == null || proto.isEmpty()) ? new TCatConnector(tcat) : new TCatConnector(proto,tcat);
        
        if ( props.getOrDefault("SECURE","FALSE").equals("TRUE") ){ 
            conn.configSSL(props);
        } else {
            conn.config(props);
        }
        
        conn.setProperty("maxThreads", props.getProperty("MAXTHREADS", "1000"));
        
        return conn;
    }
    public static TCatConnector getInstance(TCat tcat, Properties props ) {
         return getInstance(tcat,null,props);
    }
}
