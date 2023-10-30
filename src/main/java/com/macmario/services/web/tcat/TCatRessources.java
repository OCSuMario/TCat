/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.macmario.services.web.tcat;

import com.macmario.general.MyVersion;
import static com.macmario.general.Version.getJavaCacerts;
import com.macmario.io.file.ReadFile;
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
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.sql.SQLException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.catalina.startup.Tomcat;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author SuMario
 */
class TCatRessources extends TCatVersion {
    final private TCatCert cert;
    final private String cl;
    
    File webbase;
    File webroot;
    
    private Tomcat tc;
    org.h2.tools.Server h2dbsrv;
    org.h2.jdbcx.JdbcDataSource h2db;
    TCatRessources() {
        System.setProperty("com.macmario.TCAT.RestartAfterDeployment", "true");
        System.setProperty("org.apache.catalina.startup.EXIT_ON_INIT_FAILURE",  "true");
        System.setProperty("tomcat.util.scan.StandardJarScanFilter.jarsToSkip", "*.jar");
        
        
        cert = new TCatCert();
        this.cl="TCatRessources";
    }
    
    void init(Tomcat tc) {
        log("FINEST:  init start");
        this.tc=tc;
        try {
           if ( System.getProperty("h2.bindAddress") == null )
                System.setProperty("h2.bindAddress",tc.getHost().getName());
           System.setProperty("h2.jdbc", "jdbc:h2:tcp://sa:"+getDefaultPass()+"@"+System.getProperty("h2.bindAddress")+":"+(tc.getServer().getPort()+1)+"/data");
           log("FINEST:  bindAddress ->"+System.getProperty("h2.bindAddress"));
           this.h2db=updateDataSource();
           this.h2dbsrv = org.h2.tools.Server.createTcpServer("-tcp","-tcpAllowOthers","-tcpPort", getH2Port(System.getProperty("h2.jdbc")));
           this.h2dbsrv.start();
           log("INFO: h2db starts :"+this.h2db);
        } catch(SQLException|NullPointerException io)  {
            log("ERROR: init h3db Exception "+io.toString());
        }
    }
    
    org.h2.jdbcx.JdbcDataSource updateDataSource(){
        org.h2.jdbcx.JdbcDataSource ds = new org.h2.jdbcx.JdbcDataSource();
        final String jurl=System.getProperty("h2.jdbc");
        ds.setURL( getH2DB(jurl) );
        ds.setUser( getH2User(jurl));
        ds.setPassword( getH2Pass(jurl));
        return ds;
    }
    String getH2User(String url) { return getUserPart(url,0); }
    String getH2Pass(String url) { return getUserPart(url,1); }
    String getUserPart(String url, int part){
        String ret ="";
        try{
           String[] ap = url.split("/"); 
           String   at = ap[2].split("@")[0];
                    ap = at.split(":");
                    log("FINEST: at:"+at+":");
                   ret=( part == 1 )?at.substring(ap[0].length()+1):ap[part];                   
        }catch(NullPointerException|ArrayIndexOutOfBoundsException ne){}
        log("FINEST: return:"+ret+":");
        return ret;
    }
    String getH2DB(String url){
        StringBuilder sw = new StringBuilder("jdbc:h2:");
        //sw.append(webroot.getAbsolutePath().replaceAll(File.separator, "/"));
        sw.append(".");
        String[] ap = url.split("/");
        sw.append("/").append(ap[ap.length-1].split("\\?")[0].split(";")[0]);
        sw.append(";IFEXIST=true;");
        log("FINE: H2 link:"+sw.toString()+":");
        return sw.toString();
    }
    String getH2Port(String url){
        String ret="38383";
        try {
            String[] ap = url.split(":");
            ret=ap[3].split("/")[0];
        } catch(NullPointerException ne){}
        return ret;
    }
    
    void out(String info) {
         System.out.println(info);
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
    
    private File keyStore=null;
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
    }
    
    void setConnectorAddOne(String[] ar) {
        if ( ar != null ) {
             boolean start=false;
             for ( int i=0; i<ar.length; i++ ) {
                 
                 if ( start ) {
                   ar[i]=ar[i].toLowerCase();
                   if ( ar[i].equals("https") || ar[i].equals("ssl") || ar[i].equals("tls") ) { start=true; }  
                 } else {
                   switch(i) {
                       case 0,1,2: { break;}
                       case     3: { setKeyStore(ar[i]); break;}
                       case     4: { setKeyStorePass(ar[i]); break; }
                       case     5: { setTrustStore(ar[i]); break;}
                       case     6: { setTrustStorePass(ar[i]); break; }
                       default:  { out(""+i+" ->"+ar[i]+"<- "); break; }
                   }  
                 }
             }
             
        }
    }
    
    void createKeyStore(ReadFile fn, String pass) {
      try  {
        KeyStore ks = KeyStore.getInstance("JKS");
        if ( ! fn.isReadableFile() ) {
            ks.store(fn.getOutStream(), pass.toCharArray());
        }
      }catch ( KeyStoreException
              | java.io.IOException 
              |java.security.NoSuchAlgorithmException 
              |java.security.cert.CertificateException kse) {}  
        
    }
    
    KeyStore loadKeyStore(ReadFile fn, String pass) {
        try {
         KeyStore ks = KeyStore.getInstance("JKS");
                  ks.load(fn.getInputStream(), pass.toCharArray());
         return ks;
        } catch( KeyStoreException
              | java.io.IOException 
              |java.security.NoSuchAlgorithmException 
              |java.security.cert.CertificateException kse ) { return null; }
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
}
