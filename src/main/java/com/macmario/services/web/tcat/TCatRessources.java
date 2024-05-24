/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.macmario.services.web.tcat;

import com.macmario.general.MyVersion;
import com.macmario.io.file.SecFile;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http11.Http11NioProtocol;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author SuMario
 */
class TCatRessources extends TCatVersion {
    TCat tcat;
    TCatDb tcatdb;
    TCatCert cert;
    
    
    File webbase;
    File webroot;
    
    Tomcat tc;
    
    int   err=0;
    Context rcontext;
            
    File base;
   
    Properties    config = new Properties();
    Properties[] _config = new Properties[10];
    String mainUrl;
    String cl;
    String root="/";
    
    String rWelcome="<html>\n<title>Welcome</title>\n<body>\n"+
                            "<h1>Welcome - Have a great Day!</h1>"+
                            "\n</body>\n</html>\n";
    
    TCatRessources() {
        System.setProperty("com.macmario.TCAT.RestartAfterDeployment", "true");
        System.setProperty("org.apache.catalina.startup.EXIT_ON_INIT_FAILURE",  "true");
        System.setProperty("tomcat.util.scan.StandardJarScanFilter.jarsToSkip", "*.jar");
        
        this.cl="TCatRessources";
    }
    
    void init(Tomcat tc) {
        log("FINEST:  init start");
        this.tc=tc;
        try {
           if ( System.getProperty("h2.bindAddress") == null )
                System.setProperty("h2.bindAddress",tc.getHost().getName());
           //System.setProperty("h2.jdbc", "jdbc:h2:tcp://sa:"+getDefaultPass()+"@"+System.getProperty("h2.bindAddress")+":"+(tc.getConnector().getPort()+1)+"/"+getWorkingDir()+"/data");
           log(3,"bindAddress ->"+System.getProperty("h2.bindAddress"));
           log(3,"bindPort ->"+tc.getConnector().getPort());
           //log(1,"jdbc ->"+System.getProperty("h2.jdbc"));
           tcatdb.updateJDBC(("jdbc:h2:tcp://sa:"+getDefaultPass()+"@"+System.getProperty("h2.bindAddress")+":"+(tc.getConnector().getPort()+1)+"/"+getWorkingDir()+"/data"));
           tcatdb.h2db=tcatdb.updateDataSource();
           tcatdb.h2dbsrv = org.h2.tools.Server.createTcpServer("-tcp","-tcpAllowOthers","-tcpPort", tcatdb.getH2Port(System.getProperty("h2.jdbc")));
           tcatdb.h2dbsrv.start();
           log("INFO: h2db starts :"+tcatdb.h2db);
        } catch(SQLException|NullPointerException io)  {
            log("ERROR: init h3db Exception "+io.toString());
        }
    }
    

    
    
    void out(String info) {
         System.out.println(info);
    }
    
    public boolean addResConnection(String file) { return addResConnection(new SecFile(file)); }
    public boolean addResConnection(SecFile file) {
        if ( ! file.isReadableFile() ) { return false; }
        if ( file.isCrypted() ) {
            try { 
                Properties conf = new Properties();
                log(1,"load property from  "+file.getFQDNName());
                    conf.load(new ByteArrayInputStream( file.readOut().toString().getBytes() ) );
                log(1,"config ->|"+conf+"|<-");
                int lCount = getInt(config.getProperty("LLISTEN", "0"));
                if ( lCount < _config.length) {
                    _config[lCount]=conf;
                    lCount++;  
                    config.setProperty("LLISTEN",""+lCount);
                }     
                log(1, "pickup LLISTEN after add :"+config.getProperty("LLISTEN", "0")+":");
            } catch(IOException io){
                log(1,"ERROR - "+io.getMessage()+" ");
                return false;
            }    
        }
        return true;
    }
    void startExtraConfig() {
        int lCount = getInt(config.getProperty("LLISTEN", "0"));
        for ( int max=0; max<=lCount; max++) {
            Properties conf = _config[max]; 
            if ( conf != null ) {
                log(1,"config["+max+"] ->|"+conf+"|<-"); 
                if ( conf.getProperty("PORT") != null ) {
                    TCatConnector conn=null;
                    if ( conf.getProperty("KEYSTORE") != null ) {
                          conn = getSslConnector(conf);
                    } else {
                          conn = getConnector(conf);
                    } 
                    this.tc.setConnector(conn);
                }    
            }
        }
    }
    
    public void addPublicListen(String[] ar) {
        final String func="addPublicListen(String[] ar)";
        printf(cl,func,1, "add public listen ->"+ar+"<-");
        TCatConnector conn = null; 
        Properties prop = new Properties();
                   prop.setProperty("PORT", ar[1]);
                   prop.setProperty("HOST", ar[0]);
                   prop.setProperty("PUBLIC", "1");
        printf(cl,func,1,"ar.length->"+ar.length+" ->"+ar[2]+"<-");
        if ( ar.length < 3 || ! (  ar[2].equals("https") || ar[2].equals("ssl") || ar[2].equals("tls") ) ) {
            conn = this.getConnector(prop);
            
        } else {
            if (ar.length > 3 ) { setConnectorAddOne(ar); }
            prop.setProperty("TRUSTSTORE", getJavaCacerts().getAbsolutePath());
            prop.setProperty("TRUSTSTOREPW", "changeit");
            prop.setProperty("KEYSTORE", ".keyfile.jks");
            prop.setProperty("KEYSTOREPW", getDefaultPass());
            conn = this.getSslConnector(prop);
            
            /*conn = getSslConnector(ar);
            conn.setDiscardFacades(false);*/
        }  
        printf(cl,func,1,"add connector to server");
        
        this.tc.setConnector(conn);
        
        printf(cl,func,1,"->"+this.tc.noDefaultWebXmlPath()+"<-");
        
    }
    
    TCatConnector getConnector(Properties ar) {
        TCatConnector conn = new TCatConnector(tcat);
            conn.setDiscardFacades(false);
            
            String   PO=ar.getProperty("PORT", "37373");
            String   HO=ar.getProperty("HOST");
            String  PUB=ar.getProperty("PUBLIC", "0");
            String  maxThreads=ar.getProperty("MAXTHREADS", "1000");
            if ( HO == null ) {
                if ( PUB.equals("1") ) {
                    HO=getLocalIpFrom(getHostname());
                } else {
                    HO="localhost";
                }
            }
            
            conn.setPort(Integer.parseInt(PO));
            conn.setProperty("address", HO); 
            conn.setProperty("maxThreads", maxThreads);
            
       return conn;     
    }
    
    TCatConnector getSslConnector(Properties ar) {
        
        TCatConnector connector = new TCatConnector("org.apache.coyote.http11.Http11NioProtocol", tcat);
                  
	Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
        
        String   PO=ar.getProperty("PORT", "37373");
        String   HO=ar.getProperty("HOST", "localhost");
        String  PUB=ar.getProperty("PUBLIC", "0");
        String PROT=ar.getProperty("PROTOCOL", "TLS");
        String   Trust=ar.getProperty("TRUSTSTORE", cert.getDefaultTrustStore().getAbsolutePath());
        String TrustPW=ar.getProperty("TRUSTSTOREPW", "changeit");
        String    Keys=ar.getProperty("KEYSTORE", cert.getDefaultKeyStore().getAbsolutePath());
        String  KeysPW=ar.getProperty("KEYSTOREPW", getDefaultPass());
        String  Cipher=ar.getProperty("CIPHER;", "");
        String  maxThreads=ar.getProperty("MAXTHREADS", "1000");
        if (   Trust.isEmpty()           ) { Trust= cert.getDefaultTrustStore().getAbsolutePath(); }
        if ( TrustPW.equals("<default>") ) { TrustPW="changeit"; }
        if (  KeysPW.equals("<default>") ) {  KeysPW=getDefaultPass(); }
        
        log(4,"HOST "+HO+":"+PO+" PROT->"+PROT+" Cipher:"+Cipher+":\n\t   KeyFile:"+Keys+":\n\t TrustFile:"+Trust+":");
        
        log(4,"keys:"+Keys+":");
        File   keystore = new File(Keys);
        cert.openKeystore(keystore, KeysPW);
        log(4,"trusts:"+Trust+":");
        File truststore = new File(Trust);
        cert.openTrustStore(truststore, TrustPW);
        
        connector.setScheme("https");
		connector.setSecure(true);
                connector.setProperty("SSLEnabled", "true");
                connector.setProperty("address", HO);
		connector.setPort(getInt(PO));
                
		protocol.setSSLEnabled(true);
        
                log(4,"TLS defaults are set - define cert");     
                TCatSSLHostConfig  shost = new TCatSSLHostConfig(tcat);
                               //shost.setSslProtocol(PROT); <- Cipher
                               //shost.setProtocols(PROT);
        if (!Cipher.isEmpty()) shost.setCiphers(Cipher);
                              
                TCatSSLHostConfigCertificate scert = new TCatSSLHostConfigCertificate(shost, TCatSSLHostConfigCertificate.Type.DSA, tcat);
                                         KeyStore ks = cert.openKeystore(keystore, KeysPW);
                                                       //cert.updateKeyStoreWithDefault(ks,keystore, KeysPW);
                                         scert.setCertificateKeystore(ks);
                                         //scert.setCertificateFile(keystore.getAbsolutePath());
                                         //scert.setCertificateKeyPassword(KeysPW);
                                         scert.setCertificateKeyAlias(cert.getDefaultAlias());
                                         scert.setCertificateKeystoreType(cert.getStoreAlg());
                                         
                                         
                log(4,"add now Certificate to SSLHostConfig");
                               shost.addCertificate(scert);
                log(4,"add now TrustStore to SSLHostConfig");
                               shost.setTruststoreFile(truststore.getAbsolutePath());
                               shost.setTruststorePassword(TrustPW);
	
                log(4,"add now SSLHostConfig");
                               
                protocol.addSslHostConfig(shost);
                
                connector.setProperty("maxThreads", maxThreads);
                
                log(4,"return SSLConnector");
        return connector;
    }
    
    /*Connector getSslConnector(String[] ar) {
	Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
	Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
        
        log(1, "parameter:"+ar);
	
		File keystore = cert.getKeyStore();
            log(2,"keystore:"+keystore);
		File truststore = cert.getTrustStore();
            log(2,"truststore:"+truststore);    
		connector.setScheme("https");
		connector.setSecure(true);
                connector.setProperty("SSLEnabled", "true");
                connector.setProperty("address", ar[0]);
		connector.setPort(getInt(ar[1]));
                
		protocol.setSSLEnabled(true);
                
                log(4,"TLS defaults are set - define cert");     
                SSLHostConfig shost = new SSLHostConfig();
                              shost.setProtocols("TLSv1.2,+TLSv1.3");
                              
                SSLHostConfigCertificate scert = new SSLHostConfigCertificate(shost, SSLHostConfigCertificate.Type.DSA);
                                         scert.setCertificateFile(keystore.getAbsolutePath());
                                         scert.setCertificateKeyPassword(cert.getKeyStorePassword(keystore));
                                         scert.setCertificateKeyAlias("server");
                                         scert.setCertificateKeystoreType("JKS");
                                         
                log(4,"add now Certificate to SSLHostConfig");
                               shost.addCertificate(scert);
                log(4,"add now TrustStore to SSLHostConfig");
                               shost.setTruststoreFile(truststore.getAbsolutePath());
                               shost.setTruststorePassword(cert.getTrustStorePassword(truststore));
	
                log(4,"add now SSLHostConfig");
                               
                protocol.addSslHostConfig(shost);
                
	return connector;
	
        
    }*/
    
    TCatConnector createSslConnector(){
           TCatConnector httpsConnector = new TCatConnector(tcat);
           httpsConnector.setPort(443);
           httpsConnector.setSecure(true);
           httpsConnector.setScheme("https");
           //httpsConnector.setAttribute("SSLEnabled", "true");
           //httpsConnector.setProperty("SSLEnabled", "true");
           TCatSSLHostConfig sslConfig = new TCatSSLHostConfig(tcat);

           TCatSSLHostConfigCertificate certConfig = new TCatSSLHostConfigCertificate(sslConfig, TCatSSLHostConfigCertificate.Type.RSA,tcat);
           certConfig.setCertificateKeystoreFile("/root/.keystore");
           certConfig.setCertificateKeystorePassword("changeit");
           certConfig.setCertificateKeyAlias("mykeyalias");
           sslConfig.addCertificate(certConfig);

           httpsConnector.addSslHostConfig(sslConfig);

           return httpsConnector;
    }
    
    InputStream getFromRessource(String res) {
         return getClass().getClassLoader().getResourceAsStream(res);
    }
    
    String loadStringFromRessource(String res) {
        StringBuilder sw= new StringBuilder();
        InputStream in = getFromRessource(res);
        String line;
        try {    
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                while ((line = br.readLine()) != null) {
                    sw.append(line).append("\n");
                }
            }
        } catch(IOException | NullPointerException  io) {}    
        return sw.toString();
    }
    
    Document getXMLFromStream(InputStream in) {
        Document doc=null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Create the builder and parse the file
            doc = factory.newDocumentBuilder().parse(in);
        } catch(ParserConfigurationException|IOException|SAXException|NullPointerException pce) {
            doc = getNewXmlDocument("error", pce.getMessage());
        }
        return doc;
    }
    Document getXMLFromFile(File f) { 
        try { 
            return getXMLFromStream( new java.io.FileInputStream(f)); 
        }catch(IOException io ) {
            return getNewXmlDocument("error", io.getMessage());
        }
    }
    Document getXMLFromRessource(String res) { return getXMLFromStream( getFromRessource(res) ); }
    
    Document getNewXmlDocument(String l1, String l2) {
        Document doc =null;
        try {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        
        InputStream in  = new ByteArrayInputStream(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\t<info key=\""+l1+"\" value=\""+l2+"\" />\n</xml>\n").getBytes(StandardCharsets.UTF_8));
                       builder.parse(in);
            doc = builder.newDocument();
        }catch(NullPointerException|IOException|ParserConfigurationException|SAXException ne) {}    
        return doc;
    }
    
    String getDefaultPass() {
        
        MyVersion m = new MyVersion();
        
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(  ("Uost:"+super.getHostKey()+": User:"+super.getUserKey()+": Jar:"+m.getLocationMD5() ).getBytes()  );
        
            byte[] mdbytes = md.digest();

            //convert the byte to hex format method 1
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mdbytes.length; i++) {
              sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch( java.security.NoSuchAlgorithmException io ) {}
        return m.getLocationMD5();
        
    }
    
    /*private File keyStore=null;
    private File trustStore=null;
    File getKeyStore() { 
        if ( keyStore == null ) {
            setKeyStore(null);
        }
        ReadFile fn = new ReadFile(keyStore);
        if ( ! fn.isReadableFile() ){ 
            createKeyStore(fn,getKeyStorePassword(fn.getFile()));
        }
        return keyStore;
    }
    File getTrustStore() { 
        if ( trustStore == null ) {
            setTrustStore(null);
        }
        ReadFile fn = new ReadFile(trustStore);
        if ( ! fn.isReadableFile() ){ 
            //createKeyStore(fn,getKeyStorePassword(fn.getFile()));
        }
        return trustStore; 
    }
    void setKeyStore(String file){
        ReadFile fn;
        if ( file != null ) { 
            fn = new ReadFile(file); 
            if ( fn.isReadableFile() ) { keyStore=fn.getFile(); }
        } 
        if( keyStore == null ) {
            keyStore=getDefaultKeyStore();
            fn = new ReadFile(keyStore);
        }
    }
    private SecFile secKeyStoreFile=null;
    private SecFile secTrustStoreFile=null;
    String getKeyStorePassword(File keys) {
        if ( secKeyStoreFile != null ) {
            return secKeyStoreFile.readOut().toString();
        }
        File ks = getDefaultKeyStore();
        if ( ks.getAbsolutePath().equals(keys.getAbsolutePath()) ) {
            return this.getDefaultPass();
        } else {
             SecFile sn = new SecFile( keys.getAbsolutePath().replaceAll(".jks$", "")+".pw");
             if ( sn.isReadableFile() ) { return sn.readOut().toString(); }
        }
        return "changit";
    }
    String getTrustStorePassword(File keys) {
        if ( secTrustStoreFile != null ) {
            return secTrustStoreFile.readOut().toString();
        }
        File ks = getDefaultTrustStore();
        if ( ks.getAbsolutePath().equals(keys.getAbsolutePath()) ) {
            return this.getDefaultPass();
        }else {
             SecFile sn = new SecFile( keys.getAbsolutePath().replaceAll(".jks$", "")+".pw");
             if ( sn.isReadableFile() ) { return sn.readOut().toString(); }
        }
        return "changit";
    }
    File getDefaultKeyStore()   { return new File( getTempDir()+File.separator+".keystore.jks"); }
    File getDefaultTrustStore() { return getJavaCacerts(); }
    
    void setTrustStore(String file){
        if ( file != null ) {
            ReadFile fn = new ReadFile(file); 
            if ( fn.isReadableFile() ) { trustStore=fn.getFile(); }
            
        } else {
            trustStore=getJavaCacerts();
        }
    }
    
    void setKeyStorePass(String file) {
        if ( file != null && ! file.isEmpty() ) {
            SecFile sn = new SecFile(file);
            if ( sn.isReadableFile() ) { this.secKeyStoreFile=sn; }
        }
    }
    
    void setTrustStorePass(String file) {
        if ( file != null && ! file.isEmpty() ) {
            SecFile sn = new SecFile(file);
            if ( sn.isReadableFile() ) { this.secTrustStoreFile=sn; }
        }
    }*/
    
    void setConnectorAddOne(String[] ar) {
        if ( ar != null ) {
             boolean start=false;
             for ( int i=0; i<ar.length-1; i++ ) {
                 
                 if ( start ) {
                   ar[i]=ar[i].toLowerCase();
                   if ( ar[i].equals("https") || ar[i].equals("ssl") || ar[i].equals("tls") ) { start=true; }  
                 } else {
                   switch(i) {
                       case 0,1,2: { break;}
                       case     3: { cert.setKeyStore(ar[i]); break;}
                       case     4: { cert.setKeyStorePass(ar[i]); break; }
                       case     5: { cert.setTrustStore(ar[i]); break;}
                       case     6: { cert.setTrustStorePass(ar[i]); break; }
                       default:  { out(""+i+" ->"+ar[i]+"<- "); break; }
                   }  
                 }
             }
             
        }
    }
    
    
    
    String getLocalIpFrom(String name) {
        final String func="getLocalIpFrom(String name)";
        String ret=name;
        try { 
            InetAddress inetAddress = InetAddress.getByName(name);
            ret=inetAddress.getHostAddress();
        } catch(UnknownHostException | NullPointerException ne ) {
             printf(cl,func,1,"getLocalIpFrom("+name+") ->"+ne.getLocalizedMessage());
        }
        printf(cl,func,2,"return:"+ret+": from name:"+name+":");
        return ret;
    }
    
    String getHostname() { return com.macmario.net.tcp.TcpHost.getHostname(); }
    
    public void log(int deb, String msg){ super.log(deb, "TCATRessource::"+msg); }
}
