
package com.macmario.services.web.jproxy;

import static com.macmario.general.Version.getBooleanValue;
import static com.macmario.general.Version.getInt;
import com.macmario.io.crypt.Crypt;
import com.macmario.io.net.Http;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;

/**
 *
 * @author SuMario
 */
public class jHttpProxyServerRessource extends jHttpProxyRunnableT {
    
    jHttpProxyServer server=null;
    
    final jHttpProxyVersion version = new jHttpProxyVersion();
    
	final String HTTP_VERSION = "HTTP/1.1";
        final Crypt crypt = new Crypt();
	final String MAIN_LOGFILE           = "log" +File.separator+"proxyserver.log";
	final String DATA_FILE              = "conf"+File.separator+"proxyserver.data";
	final String SERVER_PROPERTIES_FILE = "conf"+File.separator+"proxyserver.properties";
        
        Http http;
        boolean console = false;

        // Chrome v132
	String httpUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36";
	ServerSocket listen;
	BufferedWriter logFile;
	BufferedWriter accessLogFile;
	Properties serverproperties = null;

	private volatile long bytesread;
	private volatile long byteswritten;
	private volatile int numConnections;

	boolean enable_cookies_by_default = true;
	WildcardDictionary dic = new WildcardDictionary();
	Vector<OnURLAction> urlactions = new Vector<OnURLAction>();

	public final int DEFAULT_SERVER_PORT = 8088;
	public final String WEB_CONFIG_FILE = "admin/jp2-config";

	public int port = DEFAULT_SERVER_PORT;
	public InetAddress proxy;
	public int proxy_port = 0;

	public long confAuth = 0;
	public long config_session_id = 0;
	//public String config_user = "";
	//public String config_password = "";
        jHttpProxyUser confAdmin = null;

	public boolean fatalError;
	String errorMessage;
	
	public boolean useProxy = false;
	public boolean block_urls = false;
	public boolean filter_http = false;
	public boolean ldebug = false;
	public boolean log_access = true;
	public String log_access_filename = "log"+File.separator+"access.log";
	public boolean webconfig = false;
	public boolean www_server = true;
        public boolean proxypass  = false;
        public String  proxyapp   = "";
        
        jHttpProxyServerRessource() {
            try {
                this.http = new Http(new URL("http://127.0.1.10:37373/"));
            } catch( MalformedURLException|NoSuchAlgorithmException|KeyManagementException ne) {} 
            crypt.setCustomKey(version.SERVERNAME);
        }

        boolean updateParameter(String[] args) {
             if ( args != null && args.length>0) {
                 for ( int i=0; i<args.length; i++) {
                     
                     if ( args[i].equals("-d")) { ldebug=true; version.debug++; }
                     else if ( args[i].equals("-p") ) { port=getInt(args[++i]); }
                     else if ( args[i].equals("-proxy_port") ) { proxy_port=getInt(args[++i]); }
                     else if ( args[i].equals("-app") ) { proxypass=true; proxyapp=args[++i]+";"+proxyapp; }
                     else if ( args[i].equals("-getAdmin" ) ) {
                          if ( console ) {
                             System.out.println("admin: "+confAdmin.getUsername()+"\n"+
                                                "pass : "+confAdmin.getCrypted(confAdmin.getPassword())
                             );
                             return false;
                          }
                     }
                 }
             }   
             
             return true;
        }
        
        
        public void addBytesRead(long read) {
		bytesread += read;
	}

	
	public void addBytesWritten(int written) {
		byteswritten += written;
	}

	public int getServerConnections() { return numConnections; }

        public boolean enableCookiesByDefault() { return this.enable_cookies_by_default; }

	public void enableCookiesByDefault(boolean a) { enable_cookies_by_default = a; 	 }

	public void resetStat() {
		bytesread = 0;
		byteswritten = 0;
	}
        
	public long getBytesRead() { return bytesread;	}

	public long getBytesWritten() { return byteswritten;	}

	public void increaseNumConnections() { numConnections++;   }

	public void decreaseNumConnections() { numConnections--;	}
        public int getConnextionCount() {return numConnections; }

        
	void init() {
		// create new BufferedWriter instance for logging to file
		try {
			logFile = new BufferedWriter(new FileWriter(MAIN_LOGFILE, true));
		} catch (Exception e_logfile) {
			setErrorMsg("Unable to open the main log file.");
			if (logFile == null)
				setErrorMsg(version.SERVERNAME+" need write permission for the file "
						+ MAIN_LOGFILE);
			errorMessage += " " + e_logfile.getMessage();
		}
		writeLog(version.SERVERNAME+" proxy server startup...");

		// restore settings from file. If this fails, default settings will be used
		restoreSettings();
		
	}
        
        
	public void saveSettings() {
		
		Boolean propertiesFileSaved = false;
		Boolean objectFileSaved = false;
		
		if (serverproperties == null)
			return;
		
		serverproperties.setProperty("server.http-proxy",          ""+Boolean.getBoolean(""+useProxy));
		serverproperties.setProperty("server.http-proxy.hostname", proxy.getHostAddress());
		serverproperties.setProperty("server.http-proxy.port",     ""+getInt(""+proxy_port));
		serverproperties.setProperty("server.filter.http",         ""+Boolean.getBoolean(""+filter_http));
		serverproperties.setProperty("server.filter.url",          ""+Boolean.getBoolean(""+block_urls) );
		serverproperties.setProperty("server.filter.http.useragent", httpUserAgent);
		serverproperties.setProperty("server.enable-cookies-by-default",""+Boolean.getBoolean(""+enable_cookies_by_default) );
		serverproperties.setProperty("server.debug-logging",       ""+Boolean.getBoolean(""+ldebug));
		serverproperties.setProperty("server.port",                ""+getInt(""+port));
		serverproperties.setProperty("server.access.log",          ""+Boolean.getBoolean(""+log_access) );
		serverproperties.setProperty("server.access.log.filename", log_access_filename);
		serverproperties.setProperty("server.webconfig",           ""+Boolean.getBoolean(""+webconfig)  );
		serverproperties.setProperty("server.www",                 ""+Boolean.getBoolean(""+www_server) );
                serverproperties.setProperty("server.proxypass",           ""+Boolean.getBoolean(""+proxypass) );
                serverproperties.setProperty("server.proxyapp",            proxyapp );
		serverproperties.setProperty("server.webconfig.username",  crypt.getCrypted(confAdmin.getUsername()));
		serverproperties.setProperty("server.webconfig.password",  crypt.getCrypted(confAdmin.getPassword()));
		
		try {
		    serverproperties
			.store(new FileOutputStream(SERVER_PROPERTIES_FILE),
			       version.SERVERNAME+" properties. Look at the README file for further documentation.");
			propertiesFileSaved = true;
		} catch (IOException io) {
			writeLog("storeServerProperties(): " + io.getMessage());
		}
		
		try {
			ObjectOutputStream file = new ObjectOutputStream(new FileOutputStream(
				DATA_FILE));
			file.writeObject(dic);
			file.writeObject(urlactions);
			file.close();
			objectFileSaved = true;
		}
		catch(IOException io){
			writeLog("storeServerProperties(): " + io.getMessage());
		}
		
		if (objectFileSaved && propertiesFileSaved)
			writeLog("Configuration saved successfully");
		else
			writeLog("Failure during saving server properties or object stream");
		
	}

	
	public void restoreSettings()
	{
		Boolean propertiesLoaded = false;
		Boolean fileLoaded = false;
		
		if (serverproperties == null) {
			serverproperties = new Properties();
			try {
				serverproperties.load(new DataInputStream(new FileInputStream(SERVER_PROPERTIES_FILE)));
				propertiesLoaded = true;
			} catch (IOException e) {
				writeLog("getServerProperties(): " + e.getMessage());
			
			}
		}
		
		useProxy = getBooleanValue(serverproperties.getProperty("server.http-proxy", "false"));
		try {
		   proxy = InetAddress.getByName(serverproperties.getProperty("server.http-proxy.hostname", "127.0.0.1"));
		} catch (UnknownHostException e) {
		}
		proxy_port = getInt(serverproperties.getProperty("server.http-proxy.port", "8080"));
		block_urls = getBooleanValue(serverproperties.getProperty("server.filter.url", "false"));
		httpUserAgent = serverproperties.getProperty(
				"server.filter.http.useragent",
				"Mozilla/4.0 (compatible; MSIE 4.0; WindowsNT 5.0)");
		filter_http = getBooleanValue(serverproperties.getProperty("server.filter.http", "false"));
		enable_cookies_by_default = getBooleanValue(serverproperties.getProperty("server.enable-cookies-by-default", "true"));
                if ( debug == 0 ) {
		     ldebug = getBooleanValue(serverproperties.getProperty("server.debug-logging", "false"));
                } else {
                     ldebug = true;
                }  
                
		port        = getInt(serverproperties.getProperty("server.port", "8088"));
		log_access  = getBooleanValue(serverproperties.getProperty("server.access.log", "true"));
		log_access_filename = serverproperties.getProperty("server.access.log.filename", log_access_filename);
		webconfig   = getBooleanValue(serverproperties.getProperty("server.webconfig", "true"));
		www_server  = getBooleanValue(serverproperties.getProperty("server.www","true"));
                proxypass   = getBooleanValue(serverproperties.getProperty("server.proxypass","true"));
                proxyapp    = serverproperties.getProperty("server.proxypass","");
                
		 String u = serverproperties.getProperty("server.webconfig.username", "admin");
                        u = crypt.isCrypted(u)?crypt.getUnCrypted(u):u;
		// create random password with 16 characters as default value for the web configuration module 
		 String p = serverproperties.getProperty("server.webconfig.password", jHttpProxyUtils.randomString(16));
                        p = crypt.isCrypted(p)?crypt.getUnCrypted(p):p;
                 confAdmin = new jHttpProxyUser(u,p,server);       
		
		try {
			accessLogFile = new BufferedWriter(new FileWriter(
					log_access_filename, true));
			// Restore the WildcardDioctionary and the URLActions with the
			// ObjectInputStream (settings.dat)...
			ObjectInputStream objInputStream;
			File file = new File(DATA_FILE);
			if (file.exists()) {
				objInputStream = new ObjectInputStream(new FileInputStream(file));
				dic = (WildcardDictionary) objInputStream.readObject();
				urlactions = (Vector<OnURLAction>) objInputStream.readObject();
				objInputStream.close();
				// loading successful 
				fileLoaded = true;
			}
			
		} catch (IOException|ClassNotFoundException io) {
			setErrorMsg("restoreSettings(): " + io.getMessage());
		}
		
		if (!fileLoaded || !propertiesLoaded) {
			writeLog("Error occured during configuration read, trying to save configuration...");
			saveSettings();
		}	
		
	}

        
        public void starting() {
            
            // create now server socket
		try {
			listen = new ServerSocket(port);
		} catch (BindException e_bind_socket) {
			setErrorMsg("The socket " + port
					+ " is already in use (Another "+version.SERVERNAME+" proxy running?) "
					+ e_bind_socket.getMessage());
		} catch (IOException e_io_socket) {
			setErrorMsg("IO Exception occured while creating server socket on port "
					+ port + ". " + e_io_socket.getMessage());
		}

		if (fatalError) {
			writeLog(errorMessage);
			return;
		}
                
            super.start();
        }
    
        

	public void setErrorMsg(String a) {
		fatalError = true;
		errorMessage = a;
	}

	/**
	 * Tests what method is used with the reqest
	 * 
	 * @return -1 if the server doesn't support the method
	 */
	public int getHttpMethod(String d) {
            if ( isNotNullOrEmpty(d)) {
                String[] sp = d.split(" ");
                switch(sp[0]){
                    case "GET"    :
                    case "HEAD"   : { return 0; }
                    case "POST"   :
                    case "PUT"    : { return 1; }
                    case "CONNECT": { return 2; }
                    case "OPTIONS": { return 3; }
                    default: { break; }
                }        
            }
	    return -1;
	}

        public boolean startsWith(String a, String what) {
		int l = what.length();
		int l2 = a.length();
		return l2 >= l ? a.substring(0, l).equals(what) : false;
	}
        
        public String getUserAgent() { return httpUserAgent; }

	public void setUserAgent(String ua) {
            if ( isNotNullOrEmpty(ua) )
		       httpUserAgent = ua;
	}
        
        public String getGMTString() { return (new Date()).toString(); 	}

        
        // Return HTTP Version z.B. HTTP/1.1
        public String getHttpVersion() { return HTTP_VERSION; 	}
        
	public void run() {
            setRunning();
            writeLog("Server running on port " + this.port);
	    while(! isClosed() ) {
                  try {
                     Socket client = listen.accept();     
                     new jHttpSession(server, client);    
                  } catch(IOException io){
                      sleep(10);
                  }   
            }
        }
        
       	public void writeLog(String s) {writeLog(s, true); }

	public void writeLog(String s, boolean new_line) {
		try {
			s = new Date().toString() + " " + s;
			logFile.write(s, 0, s.length());
			if (new_line)
				logFile.newLine();
			logFile.flush();
			if (ldebug)
				System.out.println(s);
		} catch (IOException e) {
                    if (ldebug) {
			e.printStackTrace();
                    }    
		}
	}

	public void closeLog() {
		try {
			writeLog("Server shutdown.");
			logFile.flush();
			logFile.close();
			accessLogFile.close();
		} catch (IOException io) {
		}
	}
        
        public void logAccess(String s) {
		try {
			accessLogFile.write("[" + new Date().toString() + "] " + s + "\r\n");
			accessLogFile.flush();
		} catch (IOException e) {
			writeLog(version.SERVERNAME+".access(String): " + e.getMessage());
		}
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}

	public void shutdownServer() {
		closeLog();
		if ( console ) System.exit(0);
	}

    @Override
    public void log(int level, String msg){
        if ( ldebug ) {
            super.log(level, "jHttpProxyServerRessource::"+msg);
        }
    }
}
