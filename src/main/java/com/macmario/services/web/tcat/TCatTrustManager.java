package com.macmario.services.web.tcat;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 *
 * @author SuMario
 */
public class TCatTrustManager implements X509TrustManager {

    private final TCat tcat;
    private X509TrustManager defTrustManager;
    private final TCatCert cert;
    private       TrustManager[] tm=null;
    
    public TCatTrustManager(TCat tcat, KeyStore keystore){
        this.tcat=tcat;
        try {
            TrustManagerFactory trustMgrFactory=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                                trustMgrFactory.init(keystore);
            tm = trustMgrFactory.getTrustManagers();
            for(int i=0; i < tm.length; i++ ){
                 if ( tm[i] instanceof X509TrustManager ) {
                     this.defTrustManager = (X509TrustManager) tm[i];
                     i=tm.length;
                 }   
            }  
            log(1, "initialize truststore manager done");
        } catch(java.security.NoSuchAlgorithmException|java.security.KeyStoreException ne) {
            log(1, "initialize truststore manager fails - "+ne.getMessage());
        }
        this.cert=this.tcat.getCert();
        
    }

    
    
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
        log(1,"checkClientTrusted : =>"+authType );
        try {
           this.defTrustManager.checkClientTrusted(chain, authType);
        } catch(java.security.cert.CertificateException|NullPointerException ce) {
           validateMy(chain,(X509Certificate)cert.getCertificate());
        }  
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
        log(1,"checkServerTrusted : =>"+authType );
        try {
           this.defTrustManager.checkServerTrusted(chain, authType);
        } catch(java.security.cert.CertificateException|NullPointerException ce) {
           validateMy(chain,(X509Certificate)cert.getCertificate());
        }   
    }

    private void validateMy( X509Certificate[] chain, X509Certificate ca) throws java.security.cert.CertificateException{
       try { 
            log(1,"Verify Trust:"+ca );
            for(X509Certificate c: chain){
                 log(1,"check with anchor :"+c.equals(ca)+" =>"+c );
                 if ( c.equals(ca) ) { log(1,"find anchor with:"+c ); return; }
            }
       }catch(NullPointerException ce){     
            throw new java.security.cert.CertificateException(ce.getMessage());
       }
    }
    
    @Override
    public X509Certificate[] getAcceptedIssuers() {
          log(1,"getAcceptedIssuers def=>"+(this.defTrustManager == null));
          if ( this.defTrustManager == null ) { return new X509Certificate[] { (X509Certificate)cert.getCertificate() }; }
          log(1,"getAcceptedIssuers collect from default");
          X509Certificate[] ai = this.defTrustManager.getAcceptedIssuers();
          X509Certificate[] ret = new X509Certificate[ai.length+1];
                   ret[0]= (X509Certificate)cert.getCertificate();
                   for(int i=0; i<ai.length;i++ ) {
                       ret[i+1]=ai[i];
                   }
          return ret;
    }
    
    private void log(int level, String msg) {
        this.tcat.log(level, "TCatTrustManager::"+msg);
    }
    
    public TrustManager[] getTrustManagers() {
        int j=1+((tm!=null)?tm.length:0);
        TrustManager[]  rm = new TrustManager[j];
        rm[0]=this;
        int m=1;    
        if ( tm != null && j>1 )
            for (int i=0;i<(j-1); i++){
                rm[m]=tm[i]; m++;
            }
        return rm;
    }
    
    public static TrustManager[] getInstance(TCat tcat, KeyStore store){
         TCatTrustManager t = new TCatTrustManager(tcat,store);
         return t.getTrustManagers();
    }
}
