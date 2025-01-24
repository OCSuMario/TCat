package com.macmario.services.web.tcat;

import static com.macmario.general.Version.getJavaCacerts;
import com.macmario.services.web.tcat.proxy.TCatProxy;
import java.io.File;
import java.security.KeyStore;
import java.util.Properties;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;

/**
 *
 * @author MNO
 */
public class TCatConnector extends org.apache.catalina.connector.Connector {

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
    private TCatConnector(TCatProxy tp){
        this(tp.getTCat());
    }
    
    public void configTLS(Properties ar) { configSSL(ar); }
    public void configSSL(Properties ar) {
        final String defProto="TLSv1.3";
        ar=updateHost(ar);
        setHost( ar.getProperty("HOST", tcat._defHost), tcat.getInt(ar.getProperty("PORT", ""+tcat._defPort)));
        
        log(4, "add new SSL Listener "+ar);
            Http11NioProtocol protocol_ = (Http11NioProtocol) getProtocolHandler();
                              protocol_.setSslImplementationName("org.apache.tomcat.util.net.jsse.JSSEImplementation");
            
            this.Keys=ar.getProperty("KEYSTORE", cert.getDefaultKeyStore().getAbsolutePath());
            if (! Keys.contains(File.separator) ) {
                Keys=tcat.getConfigDir()+File.separator+Keys;
            }
            this.KeysPW=ar.getProperty("KEYSTOREPW", tcat.getDefaultPass());
            if ( KeysPW.isEmpty()  ||  KeysPW.equals("<default>") ) { KeysPW  = tcat.getDefaultPass(); }
            
        log(4, "defaults keystore set - done ");    
            File keystore = new File(Keys); //(new ReadFile("store")).getFile();
            KeyStore kst = cert.openKeystore(keystore, KeysPW);
        log(4, "open keystore "+kst);
        log(4, "defaults truststore set - start ");
             this.Trust=ar.getProperty("TRUSTSTORE", getJavaCacerts().getAbsolutePath());
             if ( this.Trust.isEmpty() ) { this.Trust=getJavaCacerts().getAbsolutePath();}
             log(1, "truststore config =>"+this.Trust+"<=");
             this.TrustPW=ar.getProperty("TRUSTSTOREPW", "changeit");
             if ( TrustPW.isEmpty() || TrustPW.equals("<default>") ) { TrustPW = "changeit"; }
        log(4, "defaults truststore set - done ");
            File truststore = new File(this.Trust); //(new ReadFile("store")).getFile();
            kst = cert.openTrustStore(truststore, TrustPW);
        log(4, "open trusstore "+kst);    
        
            setScheme("https");
            setSecure(true);           
            protocol_.setSSLEnabled(true);
            
            
            SSLHostConfig sslHostConfig = new SSLHostConfig();
            this.PROT=ar.getProperty("PROTOCOL", defProto);
            log(1, "default Protocol:"+sslHostConfig.getSslProtocol()+": replace with:"+this.PROT+":");
                    sslHostConfig.setSslProtocol(PROT);
                    
            this.Cipher=ar.getProperty("CIPHER", "");
            this.Cipher=(this.Cipher.equals("<default>"))?"":this.Cipher;
           
            if ( ! this.Cipher.isEmpty() ) sslHostConfig.setCiphers(this.Cipher);
            SSLHostConfigCertificate sslCertificate = new SSLHostConfigCertificate(sslHostConfig, SSLHostConfigCertificate.Type.RSA);
            
            log(1, "keystore =>"+keystore.getAbsolutePath()+"<= ALG>"+cert.getStoreAlg()+"<-PW>"+KeysPW+"<-Alias>"+cert.getDefaultAlias()+"<");
            sslCertificate.setCertificateKeystoreFile(keystore.getAbsolutePath());
            sslCertificate.setCertificateKeystoreType(cert.getStoreAlg());
            sslCertificate.setCertificateKeystorePassword(KeysPW);
            sslCertificate.setCertificateKeyAlias(cert.getDefaultAlias());
            
            log(1, "truststore =>"+truststore.getAbsolutePath()+"<=");
            sslHostConfig.setTruststoreFile(truststore.getAbsolutePath());
            sslHostConfig.setTruststorePassword(TrustPW);
            
            sslHostConfig.addCertificate(sslCertificate);
            protocol.addSslHostConfig(sslHostConfig);
            
        log(4, "listern config done "+this.toString());        
    }
    
    public void config(Properties ar) {
        ar=updateHost(ar);
        setHost( ar.getProperty("HOST", tcat._defHost), tcat.getInt(ar.getProperty("PORT", ""+tcat._defPort)));
        
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
                    ar.put("HOST",tcat._defHost);
                }
        }
        return ar;
    }
    
   
    
    private void setHost(String host, int port) {
        log(2, "update host with ->"+host+":"+port+"<-");
        setProperty("address", host);
	setPort(port);
    }
    
    public void log(int deb, String msg){ tcat.log(deb, "TCATConnector::"+msg); }
    
    public static TCatConnector getInstance(TCat tcat, String proto, Properties props ) {
        TCatConnector conn = (proto == null || proto.isEmpty()) ? new TCatConnector(tcat) : new TCatConnector(proto,tcat);
        
        
        if ( tcat.getBoolean( (String) props.getOrDefault("SECURE","FALSE") ) ){ 
            conn.configSSL(props);
        } else {
            conn.config(props);
        }
        
        conn.setProperty("maxThreads", props.getProperty("MAXTHREADS", "100"));
        
        return conn;
    }
    public static TCatConnector getInstance(TCat tcat, Properties props ) {
         return getInstance(tcat,null,props);
    }
    
    public static TCatConnector getInstance(TCat tcat) {
         return getInstance(tcat,null,null);
    }
    
    public static TCatConnector getInstance(TCatProxy tp) {
         return getInstance(tp.getTCat(),null,null);
    }
    public static TCatConnector getInstance(TCatProxy tp, Properties props) {
         return getInstance(tp.getTCat(),null,props);
    }
}
