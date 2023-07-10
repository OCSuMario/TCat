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
import org.apache.catalina.webresources.StandardRoot;

/**
 *
 * @author SuMario
 */
public class TCat extends TCatRessources {
    private final Tomcat tc;
    private final Context rcontext;
            final TCat tcat;
    private final File base;
    private       File webbase;
    private       File webroot;
    private Properties config = new Properties();
    String root="/";
    
    String rWelcome="<html>\n<title>Welcome</title>\n<body>\n"+
                            "<h1>Welcome - Have a great Day!</h1>"+
                            "\n</body>\n</html>\n";
    
    public TCat( String host, int port, String ba ) {
        super();
        this.tcat=this;
        this.base   =new File(ba);
        this.webbase=new File(ba+File.separator+"webapp");
        this.webroot = new File( webbase.getAbsolutePath()+File.separator+"ROOT");
        
        
        this.tc = new Tomcat();
        this.tc.setHostname(host);
        this.tc.setPort(port);
        this.tc.setBaseDir(ba);
        this.tc.getConnector().setPort(port);
        this.tc.getConnector().setProperty("address", host); 
        this.tc.getConnector().setProperty("maxThreads", "1000"); 
        this.tc.setAddDefaultWebXmlToWebapp(true);
        
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
               
    }
    
    //public void init() throws LifecycleException { this.tc.init(); }

    public void start() throws LifecycleException { this.tc.start(); }

    public void stop() throws LifecycleException { this.tc.stop(); }

    public void destroy() throws LifecycleException { this.tc.destroy(); }

    public Server getServer() { return this.tc.getServer(); } 
    
    public boolean addResConnection(String file) { return addResConnection(new SecFile(file)); }
    public boolean addResConnection(SecFile file) {
        if ( ! file.isReadableFile() ) { return false; }
        if ( file.isCrypted() ) {
            try { 
                config.load(new ByteArrayInputStream( file.readOut().toString().getBytes() ) );
            } catch(IOException io){
                return false;
            }    
        }
        return true;
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
        if( rootContext == null && firstContext != null ) { 
            out("root set to:"+firstContext.getPath().toString() );
            rootContext = tc.addContext("", firstContext.getPath() );
            return;
        } else { return; }
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
        if ( cont.equals("/root") ) { cont="";}
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
    
    public static void main(String[] args) throws Exception {
        TCat  tc = new TCat( "127.0.1.10", 37373, System.getProperty("user.dir") );
              if ( args.length > 0)
                  for ( int i=0; i<args.length; i++ ) {
                  
                      if ( args[i].equals("-app") ) {  tc.addWebapp(args[++i]); } 
                      else if ( args[i].equals("-conn") ) {  tc.addResConnection(args[++i]); }
              }
              //tc.addDefaultWebapp();  
              System.out.println(tc.getDefaultPass());
              
              tc.start();
              tc.getServer().await();
              if( tc.tcapp != null ) { tc.tcapp.stop(); }
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
};
