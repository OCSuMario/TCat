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
    
    private String progVersion="0.1";
    private String progName="TCat";
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


