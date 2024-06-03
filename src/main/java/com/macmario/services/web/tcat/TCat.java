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
import com.macmario.io.file.XMLReadFile;
import java.util.ArrayList;
import java.util.Properties;
import org.apache.catalina.Globals;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.webresources.StandardRoot;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author SuMario
 */
public class TCat extends TCatRessources {
    
    
    public TCat( String host, int port, String ba ) {
        super();
        this.cl="TCat";
        tcat=this;
        cert = new TCatCert(tcat);
        tcatdb=new TCatDb(tcat);
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
        log(1,"mainURL ->"+mainUrl+"<-");
        
        final String webxml = webroot.getAbsolutePath()+File.separator+"WEB-INF"+File.separator+"web.xml";
        out(webxml);
        
         rcontext= this.tc.addContext(this.tc.getHost(), "", webroot.getAbsolutePath() );
         rcontext.setPath(ba);
         rcontext.getServletContext().setAttribute(Globals.ALT_DD_ATTR, webxml);
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
        
        /*for ( String[] mp : registerServletFromWebXml(webxml)) {
                 slet=mp[0]; 
            String rt=mp[1];
            HttpServlet serv = getNewClass(mp[2]);
            if ( serv != null )
             this.tc.addServlet(rt, slet, serv);
        }*/
        
        this.tc.initWebappDefaults(rcontext);
        //rcontext.addServletMappingDecoded("/*", "ROOT");
        init(tc);
               
    }
    
    //public void init() throws LifecycleException { this.tc.init(); }

    public void start() throws LifecycleException { this.tc.start(); }

    public void stop() throws LifecycleException { tcatdb.h2dbsrv.stop(); this.tc.stop(); }

    public void destroy() throws LifecycleException { this.tc.destroy(); }

    public Server getServer() { return this.tc.getServer(); } 
    
    private HttpServlet getNewClass(String cname) {
        HttpServlet o = null;
        try {            
                    o=(HttpServlet)Class.forName(cname).newInstance();          
        } catch(ClassNotFoundException|NullPointerException|IllegalAccessException|InstantiationException ce){
            printf(cl,"getNewClass",1,"class loadinng Error - "+ce.getMessage());
        }
        return o;
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
    
    public void log(int deb, String msg){ super.log(deb, "TCAT::"+msg); }
    
    public static void main(String[] args) throws Exception {
        TCat  tc = null; 
        boolean loop = getBooleanValue( System.getProperty("com.macmario.TCAT.RestartAfterDeployment"));
        try { 
          //do {
              System.out.println("loop start");
              tc = new TCat( _defHost, _defPort, System.getProperty("user.dir") );
              tc.addDefaultWebapp();
              if ( args.length > 0)
                  for ( int i=0; i<args.length; i++ ) {
                      tc.log(2, "verify parameter:"+i+" ->"+args[i]+"<-");
                      boolean stop=false;
                      if ( args[i].equals("-app")            ) { tc.addWebapp(args[++i]); } 
                      else if ( args[i].equals("-conn")      ) { tc.addResConnection(args[++i]); }
                      else if ( args[i].equals("-public")    ) { tc.addPublicListen(args[++i].split(":")); }
                      else if ( args[i].equals("-keystore")  ) { tc.cert.setKeyStore(args[++i]); }
                      else if ( args[i].equals("-truststore")) { tc.cert.setTrustStore(args[++i]); }
                      else if ( args[i].equals("-version")   ) { tc.printVersion(); stop=true;  }
                      else if ( args[i].equals("-d")         ) { tc.debug++; }
                      else if ( args[i].equals("-defaultPass")){ System.out.println(tc.getDefaultPass()); stop=true; }
                      else { 
                          System.out.println("unknown: "+args[i]);
                          tc.printUsage(); stop=true; 
                      }
                      tc.log(2, "verify parameter exit ->"+stop+" =>"+tc.err+"<-");
                      if ( stop ) { System.exit(tc.err); }
              }
                
              
              tc.startExtraConfig();
              
              tc.start();
              tc.getServer().await();
              //if ( loop ) { sleep(3000); }
              if ( tc.tcapp != null ) { tc.tcapp.stop(); }
              tc=null;
              System.out.println("loop done");
         //} while( loop );  
        } catch(Exception e ) {
            System.out.println("ERROR - "+e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
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
                   @Override
                   protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                       tc.out("remote called");
                       PrintWriter pw = resp.getWriter();
                       pw.println(this.tc.rWelcome);
                   }
}


