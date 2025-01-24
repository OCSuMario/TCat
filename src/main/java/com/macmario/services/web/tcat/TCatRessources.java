package com.macmario.services.web.tcat;

import com.macmario.general.MyVersion;
import com.macmario.io.file.ReadDir;
import com.macmario.io.file.ReadFile;
import com.macmario.io.file.SecFile;
import com.macmario.io.file.XMLReadFile;
//import com.macmario.services.web.tcat.proxy.TCatNProxyServer;
//import com.macmario.services.web.tcat.proxy.TCatNProxy;
import com.macmario.services.web.tcat.proxy.TCatProxy;
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
import java.util.HashMap;
import java.util.Properties;
import javax.net.ssl.TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.startup.Tomcat;
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
    TCatProxy proxy;
    //TCatNProxyServer proxy;
    //TCatNProxy nProxy;
    
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
    public String mainUrl;
    String cl;
    String root="/";
    
    String rWelcome="<html>\n<title>Welcome</title>\n<body>\n"+
                            "<h1>Welcome - Have a great Day!</h1>"+
                            "\n</body>\n</html>\n";
    
    TCatRessources() {
        System.setProperty("com.macmario.TCAT.RestartAfterDeployment", "true");
        System.setProperty("org.apache.catalina.startup.EXIT_ON_INIT_FAILURE",  "true");
        System.setProperty("tomcat.util.scan.StandardJarScanFilter.jarsToSkip", "*.jar");
        System.setProperty("org.apache.catalina.logger.FileLogger", "true");
        System.setProperty("jdk.tls.acknowledgeCloseNotify", "true");
        
        this.cl="TCatRessources";
        this._configDir = new ReadDir( getConfigDir() );
        this._logDir    = new ReadDir( getLogsDir() );
        initLogger();
        
    }
    
    void init(TCat tcat) {
        this.tcat=tcat;
        initLogger();
        
        
        cert  = new TCatCert(tcat);
        tcatdb= new TCatDb(tcat);
        proxy = new TCatProxy(tcat);
        // proxy = new TCatNProxyServer(tcat);
        //nProxy = new TCatNProxy(tcat);
    }
    
    void init(Tomcat tc) {
        log("FINEST:  init start");
        this.tc=tc;
        Engine engine = this.tc.getEngine();
        engine.setDefaultHost(tc.getHost().getName());
        try {
           if ( System.getProperty("h2.bindAddress") == null )
                System.setProperty("h2.bindAddress",tc.getHost().getName());
           //System.setProperty("h2.jdbc", "jdbc:h2:tcp://sa:"+getDefaultPass()+"@"+System.getProperty("h2.bindAddress")+":"+(tc.getConnector().getPort()+1)+"/"+getWorkingDir()+"/data");
           log(3,"bindAddress ->"+System.getProperty("h2.bindAddress"));
           log(3,"bindPort ->"+tc.getConnector().getPort());
           tcatdb.updateJDBC(("jdbc:h2:tcp://sa:"+getDefaultPass()+"@"+System.getProperty("h2.bindAddress")+":"+(tc.getConnector().getPort()+1)+"/"+getConfigDir()+"/data"));
           log(4, "updateJDBC H2 done");
           tcatdb.h2db=tcatdb.updateDataSource();
           log(4, "updateDataSource H2 done");
           tcatdb.h2dbsrv = org.h2.tools.Server.createTcpServer("-tcp","-tcpAllowOthers","-tcpPort", tcatdb.getH2Port(System.getProperty("h2.jdbc")));
           log(4, "update H2 createTcpServer  done");
           tcatdb.h2dbsrv.start();
           log("INFO: h2db starts :"+tcatdb.h2db);
        } catch(SQLException|NullPointerException io)  {
            log("ERROR: init h3db Exception "+io.toString());
        }
    }
    
    
    /*org.apache.catalina.logger.FileLogger getLogger(){
        org.apache.catalina.logger.FileLogger embeddedFileLogger = new org.apache.catalina.logger.FileLogger();         
        embeddedFileLogger.setDirectory(_logDir.getFQDNDirName());
        embeddedFileLogger.setPrefix("TCATlog_");
        embeddedFileLogger.setSuffix(".txt");
        embeddedFileLogger.setTimestamp(true);
        embeddedFileLogger.setVerbosity(3);
        return 
    }*/
    
    
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
                    log(3, "Node =>"+n.getNodeName()+"  and "+m.getNodeName() );
                    NodeList nls = n.getChildNodes();
                    NodeList mls = m.getChildNodes();
                    for ( int j=0; j<nls.getLength(); j++ ){
                        log(4, "loop ml["+j+"]");
                        Node ns = nls.item(j);
                        Node ms = mls.item(j);
                        log(3, "Node Slave ns=>"+ns.getNodeName()+"<=>"+ns.getTextContent()+"<=");
                        if      ( ns.getNodeName().equals("servlet-name")  ) { sp[1]=ns.getTextContent(); }
                        else if ( ns.getNodeName().equals("servlet-class") ) { sp[2]=ns.getTextContent(); }
                        
                        log(3, "Node Slave ms=>"+ms.getNodeName()+"<=>"+ms.getTextContent()+"<=");
                        if ( ms.getNodeName().equals("url-pattern")  ) { sp[0]=ms.getTextContent(); }
                        log(4,"sp set =>"+sp[0]+"<->"+sp[1]+"<->"+sp[2]+"<=");
                    }
                    log(3,"add sp =>"+sp);
                    mp.add(sp);
                }
        }
        log(4,"return =>"+mp.toString());
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
                registerApplicationLinks(cont,file.getFQDNFileName());
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
    
    HashMap<String, ArrayList<String>> applinks=new HashMap<>();
    void registerApplicationLinks(String cont, String basedir ) {
         String filters="[$!(\\/WEB-INF\\/)(\\\\.jar$)(\\\\/META-INF\\\\/)(\\\\.swp)(\\\\$)(\\\\/inc\\\\/)]";
         //String filter=".html$|.htm$|.css$|.js$";
         String filter="html$|htm$|css$|js$|javascript$|jsp$";
         ReadDir rd = new ReadDir(basedir);
         ArrayList<String> ar = new ArrayList<>();        
         for( String f : rd.getFiles(filter,true) ) {
            if ( ! f.contains("-INF/") ) { 
                String[] sp = f.split("/");
                String s=f.substring(sp[0].length());
                log(3, cont+" ->"+s+"<-"); 
                ar.add( s );
            }
         }      
         applinks.put(cont, ar);
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
    
    
    public String[] getApplicationLinks() {
        ArrayList<String> ar = new ArrayList<>();
        for (String k : applinks.keySet()) {
            ArrayList<String> aar=applinks.get(k);
            for(String pr: aar){
                ar.add(k+pr);
            }    
        }
        return (String[]) ar.toArray();
    }
    
    public boolean setConfigDir(String dir) { return setConfigDir( new ReadDir(dir) ); } 
    public boolean setConfigDir(ReadDir dir) {
        this._configDir = dir;
        return dir.isReadable();
    }
    
    public boolean addResConnection(String file) { 
        if (file.contains(File.separator))         return addResConnection(new SecFile(file)   ); 
        return addResConnection(new SecFile(_configDir.getAbsolutePath()+File.separator+file ) );
    }
    public boolean addResConnection(SecFile file) {
        if ( ! file.isReadableFile() ) { return false; }
        if ( file.isCrypted() ) {
            return addRessourceConnection(file.readOut().toString());
        }
        return true;
    }
    
    public boolean addRessourceConnection(String msg) {
    
        try { 
                Properties conf = new Properties();
                    conf.load(new ByteArrayInputStream( msg.getBytes() ) );
                log(1,"config ->|"+conf+"|<-");
                if ( conf.getOrDefault("PROXYAPP", "").equals("")) {
                    int lCount = getInt(config.getProperty("LLISTEN", "0"));
                    if ( lCount < _config.length) {
                        _config[lCount]=conf;
                        lCount++;  
                        config.setProperty("LLISTEN",""+lCount);
                    }     
                    log(1, "pickup LLISTEN after add :"+config.getProperty("LLISTEN", "0")+":");
                } else {
                   proxy.addProxyConf(conf);
                   //nProxy.addProxyConf(conf);
                }    
        } catch(IOException io){
                log(1,"ERROR - "+io.getMessage()+" ");
                return false;
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
                    log(2, "INFO - add listener"+max+" "+conn);
                    this.tc.setConnector(conn);
                } else {
                    log(1, "ERROR - missing config port for listener"+max);
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
            //conn = this.getConnector(prop);
        } else {
            if (ar.length > 3 ) { setConnectorAddOne(ar); }
            prop.setProperty("SECURE", "1");
            prop.setProperty("TRUSTSTORE", getJavaCacerts().getAbsolutePath());
            prop.setProperty("TRUSTSTOREPW", "changeit");
            prop.setProperty("KEYSTORE", ".keyfile.jks");
            prop.setProperty("KEYSTOREPW", getDefaultPass());
            for ( String kv: ar[3].split(";") ) {
                printf(cl,func,1,"add ssl connector property :"+kv+":");
                String sp[] = kv.split("=");
                final String k = sp[0].toUpperCase(); 
                final String v = kv.substring(k.length()+1);
                printf(cl,func,1,"add ssl connector property :"+kv+":  k=|"+k+"| v=|"+v+"|");
                prop.setProperty(k, v);
            }
            //conn = this.getSslConnector(prop);
        }  
        printf(cl,func,1,"add connector to server");
        
        // this.tc.setConnector(conn);
        proxy.addPublic(conn,new String[]{""});
        /*try { proxy.addPublic(prop); } catch (CloneNotSupportedException|IOException ne){
            throw new RuntimeException(ne);
        }*/
        
        
        
        printf(cl,func,1,"->"+this.tc.noDefaultWebXmlPath()+"<-");
        
    }
    
    HttpServlet getNewClass(String cname) {
        HttpServlet o = null;
        try {         
                    log(1,"create HttpServlet for "+cname);
                    o=(HttpServlet)Class.forName(cname).newInstance();          
        } catch(ClassNotFoundException|NullPointerException|IllegalAccessException|InstantiationException ce){
            printf(cl,"getNewClass",1,"class loadinng Error - "+ce.getMessage());
        }
        return o;
    }
       
    public TCatConnector getConnector(Properties ar) {
       TCatConnector conn = TCatConnector.getInstance(tcat,ar);     
       return conn;     
    }
    
    public TCatConnector getSslConnector(Properties ar) {
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
    
    String getDefaultPass(  ) {  
        return getMessageID( "Host:"+super.getHostKey()+": User:"+super.getUserKey()+": Jar:"+m.getLocationMD5() ); 
    }
    public String getIntPass() { return  getDefaultPass(); } 
    public TCatCert getCert()  { return this.cert; }
    
    private MyVersion m = new MyVersion();
    public String getMessageID(String msg) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update( msg.getBytes()  );
        
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
    public String getMessageID() {
       String ml=getMessageID( com.macmario.io.crypt.GetPassword.getEasyPassword() );
       int len=(ml.length()>6)?6:ml.length()-1;
       return ml.substring(0, len);
    }
    
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
    
    public TrustManager[] getTrustManager(KeyStore store) { return cert.getTrustManager(store); }
    
    public void log(int deb, String msg){ super.log(deb, "TCATRessource::"+msg); }
}
