package com.macmario.services.web.tcat;

import com.macmario.io.file.ReadDir;
import com.macmario.io.file.ReadFile;
import java.io.File;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 *
 * @author SuMario
 */
abstract public class TCatVersion extends com.macmario.general.Version {
    
    public int debug=1;    
 
    ReadDir _workDir   = null;
    ReadDir _webDir    = null;
    ReadDir _configDir = null;
    ReadDir _logDir    = null;
    Logger logger;
    LogManager lm=LogManager.getLogManager();
    java.util.logging.Logger logi = null;
     
    public void log(int level, StringBuilder sw) { log(level, sw.toString()); }
    public void log(int level, String sw) {
       if ( level <= debug ) {
            final String msg = "DEBUG["+level+"/"+debug+"] - "+sw.toString();
            if (level>0) { 
                System.out.println(msg);
                switch(level){
                    case 2: { logger.fine(msg);   break; }
                    case 3: { logger.finer(msg);  break; }
                    case 4: { logger.finest(msg); break; }
                    case 5: { logger.info(msg); break; }
                }
            } else   System.out.println(sw.toString());    
       }     
    }
    
    public void initLogger(){
        logi = java.util.logging.Logger.getLogger("");
        logi.setLevel(Level.parse(getLogLevel(debug)));
               Handler   handle;
               /* Handler[] handlers = logi.getHandlers();
               
               if ( handlers.length == 1 && handlers[0] instanceof ConsoleHandler){
                   handle = handlers[0];
               } else {
                   handle = new ConsoleHandler();
               }*/
               try {
                       handle = new FileHandler(getLogsDir()+File.separator+"console.out", true);
                       handle.setFormatter(new SimpleFormatter());
                       handle.setLevel(Level.ALL);
                       handle.setEncoding("UTF-8");
               } catch(java.io.IOException io) {
                       handle = new ConsoleHandler();
               }        
        logi.addHandler(handle);
        
        String file = getConfigDir()+File.separator+"logging.properties";
        log(1,"logging :"+file+": ");
        ReadFile slog=new ReadFile(file);
        if ( slog.isReadableFile() ) {
            try { 
                lm.readConfiguration( slog.getInputStream() );
                log(0,"INFO: read "+slog.getFQDNName()+" successfully");
            }catch(java.io.IOException io){
                log(0,"WARN: read "+slog.getFQDNName()+" failed");
            }    
        }
        logger = lm.getLogger("global");
        if ( logger == null ) { logger = Logger.getGlobal(); }
        logger.setLevel(Level.parse(getLogLevel(debug)));
        
    }
    
    public boolean getBoolean(String s){
        return ( s!= null && ( s.equals("1") || s.toLowerCase().equals("true") ) );
    }
    
    /*public int getInt(String s){
        int ret=-1;
        try { ret=Integer.parseInt(s); }catch(NullPointerException|NumberFormatException ne){}
        return ret;
    }
    
    public long getLong(String s){
        long ret=-1L;
        try { ret=Long.parseLong(s); }catch(NullPointerException|NumberFormatException ne){}
        return ret;
    }*/
    
    public String getLogLevel(int level){
       String ret="INFO";
        switch(level){
            case 2: { ret="FINE"; break; }
            case 3: { ret="FINER"; break; }
            case 4: { ret="FINEST"; break; }
            case 5: { ret="ALL"; break; }
        }
        return ret;
    }
    
    public String getWorkingDir() { 
        if (_workDir != null ) { return _workDir.getAbsolutePath(); }
        return System.getProperty("user.dir"); 
    }
    
    public String getWebDir() { 
        if (_webDir != null ) { return _webDir.getAbsolutePath(); }
        return getWorkingDir()+File.separator+"webapp";
    }
    public String getConfigDir() { 
        if (_configDir != null ) { return _configDir.getAbsolutePath(); }
        return getWorkingDir()+File.separator+"conf"; 
    }
     public String getLogsDir() { 
        if (_logDir != null ) { return _logDir.getAbsolutePath(); }
        return getWorkingDir()+File.separator+"log"; 
    }
    
}
