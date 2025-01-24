
package com.macmario.services.web.tcat;


import java.sql.Connection;
import java.sql.SQLException;

/**
 *
 * @author SuMario
 */
public class TCatDb extends TCatVersion {

    private final TCat tcat;
    private String jurl=null;
   
    org.h2.tools.Server h2dbsrv;
    org.h2.jdbcx.JdbcDataSource h2db;
    
    TCatDb(TCat tcat) {
        this.tcat=tcat;
    }
    
    void updateJDBC(String jdbc) {
        this.jurl=( jdbc != null )?jdbc:null;
        if ( this.jurl == null || this.jurl.isEmpty() ) {
            log(1, "use defaults for h2.jdbc");
            this.jurl=("jdbc:h2:tcp://sa:"+tcat.getDefaultPass()+"@"+System.getProperty("h2.bindAddress")+":"+(tcat.tc.getConnector().getPort()+1)+"/"+tcat.getConfigDir()+"/data");
        }
    }
   
   private boolean isUpdateDBReCall=false;
   org.h2.jdbcx.JdbcDataSource updateDataSource(){
        org.h2.jdbcx.JdbcDataSource ds = new org.h2.jdbcx.JdbcDataSource();
        //final String jurl=System.getProperty("h2.jdbc");
        log(1, "jurl =>"+this.jurl);
        ds.setURL( getH2DB(jurl) );
        ds.setUser( getH2User(jurl));
        ds.setPassword( getH2Pass(jurl));
        try {
           try { 
                Connection conn = ds.getConnection();
                           conn.close();
           } catch(org.h2.jdbc.JdbcSQLInvalidAuthorizationSpecException ne){
               if ( ! isUpdateDBReCall ) {
                      System.out.println("DB:"+getH2DB(jurl).substring("jdbc:h2:file:".length()));
                      isUpdateDBReCall=true;
                      return updateDataSource();
               }
           }            
        } catch(SQLException se) {
            log(1, "SQL Error - "+se.getMessage()+" jdbc:"+this.jurl);
            se.printStackTrace();
        }
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
                    log(3,"at:"+at+":");
                   ret=( part == 1 )?at.substring(ap[0].length()+1):ap[part];                   
        }catch(NullPointerException|ArrayIndexOutOfBoundsException ne){}
        log(4,"return:"+ret+":");
        return ret;
    }
    String getH2DB(String url){
        StringBuilder sw = new StringBuilder("jdbc:h2:file:");
        //sw.append(webroot.getAbsolutePath().replaceAll(File.separator, "/"));
        sw.append(".");
        String[] ap = url.split("/");
        sw.append("/").append(ap[ap.length-1].split("\\?")[0].split(";")[0]);
        //sw.append(";IFEXIST=true;TRACE_LEVEL_FILE=3");
        log(3,"H2 link:"+sw.toString()+":");
        return sw.toString();
    }
    String getH2Port(String url){
        String ret="38383";
        try {
            log(3,"H2 Port URL:"+url);
            String[] ap = url.split(":");
            log(3,"H2 Port URL part:"+ap[5]+":");
            ret=ap[5].split("/")[0];
        } catch(NullPointerException ne){}
        log(3,"H2 Port:"+ret);
        return ret;
    }
    
    @Override
    public void log(int deb, String msg){ super.log(deb, "TCATDb::"+msg); }
}
