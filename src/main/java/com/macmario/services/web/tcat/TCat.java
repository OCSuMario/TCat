/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.macmario.services.web.tcat;

import com.macmario.io.file.ReadDir;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Server;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.ErrorPage;
import com.macmario.io.file.ReadFile;
import com.macmario.io.file.SecFile;
import java.io.ByteArrayInputStream;
import java.util.Properties;
import org.apache.catalina.Globals;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;

/**
 *
 * @author SuMario
 */
public class TCat extends TCatRessources {
    private final Tomcat tc;
    private int   err=0;
    private final Context rcontext;
            final TCat tcat;
    private final File base;
   
    private Properties config = new Properties();
    private final String mainUrl;
    private final String cl;
    String root="/";
    
    String rWelcome="<html>\n<title>Welcome</title>\n<body>\n"+
                            "<h1>Welcome - Have a great Day!</h1>"+
                            "\n</body>\n</html>\n";
    
    public TCat( String host, int port, String ba ) {
        super();
        this.cl="TCat";
        this.tcat=this;
        this.base   =new File(ba);
        this.webbase=new File(ba+File.separator+"webapp");
        this.webroot = new File( webbase.getAbsolutePath()+File.separator+"ROOT");
        
        debug=2;
        
        this.tc = new Tomcat();
        this.tc.setHostname(host);
        this.tc.setPort(port);
        this.tc.setBaseDir(ba);
        this.tc.getConnector().setPort(port);
        this.tc.getConnector().setProperty("address", host); 
        this.tc.getConnector().setProperty("maxThreads", "1000"); 
        this.tc.setAddDefaultWebXmlToWebapp(true);
        
        Connector conn = this.tc.getConnector();
        mainUrl = (( conn.getSecure() )?"https":"http")+"://"+host+":"+port+"/";
        
        out(webroot.getAbsolutePath()+File.separator+"WEB-INF"+File.separator+"web.xml");
        
         rcontext= this.tc.addContext(this.tc.getHost(), "", webroot.getAbsolutePath() );
         rcontext.setPath(ba);
         rcontext.getServletContext().setAttribute(Globals.ALT_DD_ATTR, webroot.getAbsolutePath()+File.separator+"WEB-INF"+File.separator+"web.xml");
         rcontext.addErrorPage(new ErrorPage());
         rcontext.setCookies(true);
         rcontext.setSessionTimeout(30);
         rcontext.setParentClassLoader(this.getClass().getClassLoader());
         
        WebResourceRoot resources = new StandardRoot(rcontext);
                        
        rcontext.setResources(resources);
        
                
        sHttpServlet servlet = new sHttpServlet(this); 
              
        String slet="S1";
        
        //this.tc.addServlet(root+"app", slet, servlet);
        this.tc.addServlet("", slet, servlet);
        //this.tc.addServlet(root+"*", slet, servlet);
        //rcontext.addServletMappingDecoded(root+"go", slet );
        //rcontext.addServletMappingDecoded(root+"*", "index");
        
        
        this.tc.initWebappDefaults(rcontext);
        init(tc);
               
    }
    
    //public void init() throws LifecycleException { this.tc.init(); }

    public void start() throws LifecycleException { this.tc.start(); }

    public void stop() throws LifecycleException { this.h2dbsrv.stop(); this.tc.stop(); }

    public void destroy() throws LifecycleException { this.tc.destroy(); }

    public Server getServer() { return this.tc.getServer(); } 
    
    public boolean addResConnection(String file) { return addResConnection(new SecFile(file)); }
    public boolean addResConnection(SecFile file) {
        if ( ! file.isReadableFile() ) { return false; }
        if ( file.isCrypted() ) {
            try { 
                config.load(new ByteArrayInputStream( file.readOut().toString().getBytes() ) );
                System.out.println("config ->|"+config+"|<-");
            } catch(IOException io){
                return false;
            }    
        }
        return true;
    }
    
    public void addPublicListen(String[] ar) {
        final String func="addPublicListen(String[] ar)";
        printf(cl,func,1, "add public listen ->"+ar+"<-");
        Connector conn = null; 
        
        printf(cl,func,1,"ar.length->"+ar.length+" ->"+ar[2]+"<-");
        if ( ar.length < 3 || ! (  ar[2].equals("https") || ar[2].equals("ssl") || ar[2].equals("tls") ) ) {
            conn = new Connector();
            conn.setPort(Integer.parseInt(ar[1]));
            conn.setProperty("address", getLocalIpFrom(ar[0])); 
        } else {
            if (ar.length > 3 ) { setConnectorAddOne(ar); }
            conn = getSslConnector(ar);
        }  
        conn.setProperty("maxThreads", "1000"); 
        this.tc.setConnector(conn);
        
        printf(cl,func,1,"->"+this.tc.noDefaultWebXmlPath()+"<-");
        
    }
    
    private Connector getSslConnector(String[] ar) {
	Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
	Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
	
		File keystore = getKeyStore();
		File truststore = getTrustStore();
		connector.setScheme("https");
		connector.setSecure(true);
                connector.setProperty("address", ar[0]);
		connector.setPort(Integer.parseInt(ar[1]));
                
		protocol.setSSLEnabled(true);
                
                SSLHostConfig shost = new SSLHostConfig();
                              shost.setProtocols("TLSv1.2,+TLSv1.3");
                              
                SSLHostConfigCertificate cert = new SSLHostConfigCertificate(shost, SSLHostConfigCertificate.Type.DSA);
                                         cert.setCertificateFile(keystore.getAbsolutePath());
                                         cert.setCertificateKeyPassword(getKeyStorePassword(keystore));
                                         cert.setCertificateKeyAlias("server");
                                         cert.setCertificateKeystoreType("JKS");
                
                               shost.addCertificate(cert);
                               shost.setTruststoreFile(truststore.getAbsolutePath());
                               shost.setTruststorePassword(getTrustStorePassword(truststore));
				                
                protocol.addSslHostConfig(shost);
                
	return connector;
	
        
    }
    
    
    
    synchronized private ReadFile unpack(ReadFile file) {
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
    
    private void addDefaultWebapp() {
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
                out("file unzip ear");
                fn=file.getFileName().substring(0, fn.length()-4).split("-")[0];
                out(" file unzip war to "+webbase+File.separator+fn);
                
                if ( file.extractZip(webbase+File.separator+fn) ) {
                    file = new ReadFile(webbase+File.separator+fn);
                    
                    ReadDir rd = new ReadDir(webbase+File.separator+fn);
                    if ( rd.isDirectory() ) {
                        String[] files = rd.getFiles(".war$");
                        for( String f : files ) {
                            System.out.println("add ear webapp:"+f);
                            addWebapp( new ReadFile( webbase+File.separator+fn+File.separator+f));
                        }
                    }    
                }
            }
            
        }
        
        out("addWebapp ->"+file.getFQDNFileName() );
        String[] sp = file.getFQDNName().split("\\.")[0].toLowerCase().split(File.separator);
        String cont = root+sp[ sp.length -1 ];
        out("cont:"+cont+":  (pre)");
        if ( cont.equals("/root") ) { cont="/ROOT"; }
        out("cont:"+cont+":");
        Context c =tc.addWebapp(cont, file.getFQDNFileName());
                c.setResponseCharacterEncoding("UTF-8");
                //c.setSessionTimeout(tc.getS);
                b=true;
                if ( nfile != null ) registerAppChanged(nfile,c);
                
        
        return b;
    }
    
    private TCatAppChecker tcapp=null;
    void registerAppChanged(ReadFile f, Context c){
        if ( tcapp == null ) {
             tcapp = new TCatAppChecker(this); 
             tcapp.start();
        }
        tcapp.register(f,c);
    }
    
    private String progVersion="0.1";
    private String progName="Elevator";
    String getProductVersion(){ return progVersion; }
    String getProduct(){ return progName; }

    void printVersion() {
        System.out.println(mh+" - "+getProduct()+" / "+ getProductVersion());
    }
    void printUsage() {
        System.out.println("usage()");
        printVersion();
        err=1;
    }
    
    public static void main(String[] args) throws Exception {
        TCat  tc = null; 
        boolean loop = getBooleanValue( System.getProperty("com.macmario.TCAT.RestartAfterDeployment"));
        do {
              System.out.println("loop start");
              tc = new TCat( "127.0.1.10", 37373, System.getProperty("user.dir") );
              tc.addDefaultWebapp();
              if ( args.length > 0)
                  for ( int i=0; i<args.length; i++ ) {
                      boolean stop=false;
                      if ( args[i].equals("-app")            ) { tc.addWebapp(args[++i]); } 
                      else if ( args[i].equals("-conn")      ) { tc.addResConnection(args[++i]); }
                      else if ( args[i].equals("-public")    ) { tc.addPublicListen(args[++i].split(":")); }
                      else if ( args[i].equals("-keystore")  ) { tc.setKeyStore(args[++i]); }
                      else if ( args[i].equals("-truststore")) { tc.setTrustStore(args[++i]); }
                      else if ( args[i].equals("-version")   ) { tc.printVersion(); stop=true;  }
                      else if ( args[i].equals("-d")         ) { tc.debug++; }
                      else { 
                          System.out.println("unknown: "+args[i]);
                          tc.printUsage(); stop=true; 
                      }
                      
                      if ( stop ) { System.exit(tc.err); }
              }
                
              System.out.println(tc.getDefaultPass());
              
              tc.start();
              tc.getServer().await();
              if ( getBooleanValue( System.getProperty("com.macmario.TCAT.RestartAfterDeployment")) ) { sleep(3000); }
              if ( tc.tcapp != null ) { tc.tcapp.stop(); }
              tc=null;
              System.out.println("loop done");
        } while( loop );  
        
    }

}

class sHttpServlet extends HttpServlet {
                   final private TCat tc;
                   sHttpServlet(TCat tc) {
                        super();
                        this.tc=tc;
                   }
                   @Override
                   protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                       tc.out("remote called");
                       PrintWriter pw = resp.getWriter();
                       pw.println(this.tc.rWelcome);
                   }
}

