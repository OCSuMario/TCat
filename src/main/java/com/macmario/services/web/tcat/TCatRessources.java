/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.macmario.services.web.tcat;

import com.macmario.general.MyVersion;
import com.macmario.io.file.ReadDir;
import com.macmario.io.file.ReadFile;
import com.macmario.io.file.SecFile;
import com.macmario.io.file.XMLReadFile;
import jakarta.servlet.http.HttpServlet;
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
import java.util.ArrayList;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http11.Http11NioProtocol;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author SuMario
 */
class TCatRessources extends TCatVersion {
    TCat tcat;
    TCatDb tcatdb;
    TCatCert cert;
    
    static String _defHost="127.0.1.10";
    static int    _defPort=37373;
    
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
    
    public ArrayList<String[]> registerServletFromWebXml(String file) { return registerServletFromWebXml(new ReadFile(file)); }
    public ArrayList<String[]> registerServletFromWebXml(ReadFile file) {
        ArrayList<String[]> mp = new ArrayList<>();
        if ( file.isReadableFile() ) {
            XMLReadFile xml = new XMLReadFile(file.getFile());
            NodeList     nl = xml.getNodeList("servlet");
            NodeList     ml = xml.getNodeList("servlet-mapping");
            
            if ( nl != null && nl.getLength()>0 )
                for ( int i=0; i<nl.getLength(); i++ ){
                    log(4, "loop nl["+i+"]");
                    String[] sp = new String[] { "","","" };
                    Node n = nl.item(i);
                    Node m = ml.item(i);
                    log(1, "Node =>"+n.getNodeName()+"  and "+m.getNodeName() );
                    NodeList nls = n.getChildNodes();
                    NodeList mls = m.getChildNodes();
                    for ( int j=0; j<nls.getLength(); j++ ){
                        log(4, "loop ml["+j+"]");
                        Node ns = nls.item(j);
                        Node ms = mls.item(j);
                        log(2, "Node Slave ns=>"+ns.getNodeName()+"<=>"+ns.getTextContent()+"<=");
                        if      ( ns.getNodeName().equals("servlet-name")  ) { sp[1]=ns.getTextContent(); }
                        else if ( ns.getNodeName().equals("servlet-class") ) { sp[2]=ns.getTextContent(); }
                        
                        log(2, "Node Slave ms=>"+ms.getNodeName()+"<=>"+ms.getTextContent()+"<=");
                        if ( ms.getNodeName().equals("url-pattern")  ) { sp[0]=ms.getTextContent(); }
                        log(3,"sp set =>"+sp[0]+"<->"+sp[1]+"<->"+sp[2]+"<=");
                    }
                    log(2,"add sp =>"+sp);
                    mp.add(sp);
                }
        }
        log(2,"return =>"+mp.toString());
        return mp;
    }
    
    public boolean addWebapp(String file) {  return addWebapp( new ReadFile(file)); }
    public boolean addWebapp(ReadFile file) { 
        boolean b=false;
        ReadFile nfile=null;
        
        String[] jars = new String[]{ ".jar", ".war" };
        String[] ears = new String[]{ ".ear" };
        if ( file.isReadableFile() ) {
            String fn = file.getFileName();
            if ( file.endsWith( jars )  ) {
                 nfile=file;
                 file=unpack(file);                  
            } 
            if ( file.endsWith(ears) ) {
                log(3,"file unzip ear");
                fn=file.getFileName().substring(0, fn.length()-4).split("-")[0];
                log(2," file unzip war to "+webbase+File.separator+fn);
                
                if ( file.extractZip(webbase+File.separator+fn) ) {
                    file = new ReadFile(webbase+File.separator+fn);
                    
                    ReadDir rd = new ReadDir(webbase+File.separator+fn);
                    if ( rd.isDirectory() ) {
                        String[] files = rd.getFiles(".war$");
                        for( String f : files ) {
                            log(2,"add ear webapp:"+f);
                            addWebapp( new ReadFile( webbase+File.separator+fn+File.separator+f));
                        }
                    }    
                }
            }
            
        }
        
        log(2,"addWebapp ->"+file.getFQDNFileName() );
        String[] sp = file.getFQDNName().split("\\.")[0].toLowerCase().split(File.separator);
        String cont = root+sp[ sp.length -1 ];
        log(2,"cont:"+cont+":  (pre)");
        if ( cont.equals("/root") ) { cont="/ROOT"; }
        log(2,"cont:"+cont+":");
        Context c =tc.addWebapp(cont, file.getFQDNFileName());
                c.setResponseCharacterEncoding("UTF-8");
                //c.setSessionTimeout(tc.getS);
                b=true;
                if ( nfile != null ) registerAppChanged(nfile,c);
                final String webxml = file.getFQDNFileName()+ File.separator+"WEB-INF"+File.separator + "web.xml";
                log(2,"web.xml =>"+webxml);
                for ( String[] mp : registerServletFromWebXml(webxml)) {
                        String slet=mp[0]; 
                        String rt=mp[1];
                        HttpServlet serv = getNewClass(mp[2]);
                        if ( serv != null )
                             tc.addServlet(cont+rt, slet, serv);
                }
        
        return b;
    }
    
     TCatAppChecker tcapp=null;
    void registerAppChanged(ReadFile f, Context c){
        if ( tcapp == null ) {
             tcapp = new TCatAppChecker(tcat); 
             tcapp.start();
        }
        tcapp.register(f,c);
    }
    
    synchronized ReadFile unpack(ReadFile file) {
        String fn = file.getFileName();
        fn=fn.substring(0, fn.length()-4).split("-")[0];
        out(" file unzip war to "+webbase+File.separator+fn);
                
        if ( file.extractZip(webbase+File.separator+fn) ) {
             return new ReadFile(webbase+File.separator+fn);
        }
        return file;
    }
    
    synchronized public void redeployApp(ReadFile f, Context c) {
        if ( f.isReadableFile() ) {
            f=unpack(f);
            c.reload();
        }
    }
    
    private boolean _couldRedeploy=false;
    boolean allowRedeploy() { return _couldRedeploy; }
    
    private Context firstContext=null;
    private Context  rootContext=null;
    
    void addDefaultWebapp() {
        ReadDir rd = new ReadDir(webbase+File.separator+"ROOT");
        if ( rd.isDirectory() ) {
            addWebapp( new ReadFile(rd.getFile()) );
        }
        /*if( rootContext == null && firstContext != null ) { 
            out("root set to:"+firstContext.getPath().toString() );
            //rootContext = tc.addContext("", firstContext.getPath() );
            return;
        } else { return; }*/
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
                    TCatConnector conn;
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
        }  
        printf(cl,func,1,"add connector to server");
        
        this.tc.setConnector(conn);
        
        printf(cl,func,1,"->"+this.tc.noDefaultWebXmlPath()+"<-");
        
    }
    
    HttpServlet getNewClass(String cname) {
        HttpServlet o = null;
        try {            
                    o=(HttpServlet)Class.forName(cname).newInstance();          
        } catch(ClassNotFoundException|NullPointerException|IllegalAccessException|InstantiationException ce){
            printf(cl,"getNewClass",1,"class loadinng Error - "+ce.getMessage());
        }
        return o;
    }
       
    TCatConnector getConnector(Properties ar) {
       TCatConnector conn = TCatConnector.getInstance(tcat,ar);     
       return conn;     
    }
    
    TCatConnector getSslConnector(Properties ar) {
        ar.put("SECURE", "TRUE");
        TCatConnector connector = TCatConnector.getInstance(tcat, "org.apache.coyote.http11.Http11NioProtocol", ar);
        
        return connector; 
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
