/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.macmario.services.web.tcat;

import static com.macmario.general.Version.getJavaCacerts;
import com.macmario.io.crypt.Base64;
import com.macmario.io.file.ReadFile;
import com.macmario.io.file.SecFile;
import static com.macmario.net.tcp.TcpHost.getHostname;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;


/**
 *
 * @author MNO
 */
class TCatCert extends TCatVersion {

    private String alg;
    private int keyLength;
    private KeyPairGenerator keyPairGen;
    private KeyPair pair;
    private String signAlg;
    private Signature sign;
    private Certificate ca;
    private Base64   base64;
    private TCat tcat;
    private String alias;
    private String storeAlg="PKCS12";
    
   TCatCert(TCat tc                                         ) { this(tc,"DSA",2048,"SHA256withDSA"); }
   TCatCert(TCat tc, String alg, int kLength,String signAlg ) { this.tcat=tc; init(alg,kLength,signAlg); } 
   
   private void init(String alg, int kLength,String signAlg) {
      final String func="TCatCert::init()";
      this.alias="server";
      this.alg=alg;
      this.keyLength=kLength;
      this.signAlg=signAlg;
      this.base64= new Base64();
      try {
        CertificateFactory.getInstance("X.509");
        //this.cf.generateCertificate(inStream);
       
        this.keyPairGen=KeyPairGenerator.getInstance(this.alg);
        this.keyPairGen.initialize(kLength);
        this.pair = this.keyPairGen.generateKeyPair();
        this.sign = Signature.getInstance(signAlg);
        this.sign.initSign(getPrivateKey());
        
        this.ca = getSelfSignCert("CN="+getHostname(),365,"SHA256withDSA" );
        log(1, "CA encoded:\n"+getEncoded(this.ca, "-----BEGIN CERTIFICATE-----","-----END CERTIFICATE-----"));
        log(1, "Public  encoded:\n"+getEncoded(this.ca.getPublicKey(), "-----BEGIN PUBLIC KEY-----","-----END PUBLIC KEY-----"));
        log(1, "Private encoded:\n"+getEncoded(this.pair.getPrivate(), "-----BEGIN PRIVATE KEY-----","-----END PRIVATE KEY-----"));
        
      }catch ( NoSuchAlgorithmException 
              | CertificateException 
              | InvalidKeyException 
              | NullPointerException nsa ) {
          printf(func,1,"error initialize - "+nsa.getLocalizedMessage());
      } 
      
      // initKeyStore
      // initTrustStore
   }
   
   public String getDefaultAlias(){ return this.alias; }
   public String setAlias(String alias) {  this.alias=(alias!=null && ! alias.isEmpty())?alias:this.alias; return getDefaultAlias(); }
   public String getStoreAlg() { return this.storeAlg; }
   
   PrivateKey  getPrivateKey(){ return pair.getPrivate() ; }
   PublicKey   getPublicKey() { return pair.getPublic() ; }
   Certificate getRootCA()    { return ca; }
   Certificate[] getChain()   { return new Certificate[] { getRootCA() }; }
   String getRootPemCertificate(){ return getEncoded(this.ca, "-----BEGIN CERTIFICATE-----","-----END CERTIFICATE-----"); }
   String getRootPemPublicKey()  { return getEncoded(this.ca.getPublicKey(), "-----BEGIN DSA PUBLIC KEY-----","-----END DSA PUBLIC KEY-----");}
   String getRootPemPrivateKey() { return getEncoded(this.pair.getPrivate(), "-----BEGIN DSA PRIVATE KEY-----","-----END DSA PRIVATE KEY-----");}
   private String getEncoded(Object o, String begin, String end){
       StringBuilder sw=new StringBuilder();
       log(1, "Encode object "+o);
       sw.append(begin).append("\n");
       
       String content = null;
       if ( o instanceof  Certificate certificate ) { 
           log(1, "Encode Certificate");
           try  {
                content=java.util.Base64.getEncoder().encodeToString(certificate.getEncoded());
           }catch(CertificateEncodingException cee) {
              log(1, "Encode Certificate Exception - "+cee.getMessage()); 
           }     
       }
       else if ( o instanceof  PublicKey publicKey ) {
            content=java.util.Base64.getEncoder().encodeToString(publicKey.getEncoded());
       }
       else if ( o instanceof  PrivateKey privateKey ) {
            content=java.util.Base64.getEncoder().encodeToString(privateKey.getEncoded());
       } else {
          log(1, "ERROR - no Encode object found ");  
       }
       int j=0;
       if ( content != null ) 
         for (int i=0 ; i<content.length(); i++) {
            sw.append(content.charAt(i));
            j++;
            if ( j == 65 ) { sw.append("\n"); j=0; }
       }
       
       sw.append("\n").append(end).append("\n");
       return sw.toString();
   }
   
   public String getSignature(String msg){ return getSignature(msg.getBytes()); }
   public String getSignature(byte[] msg){
       try {
        this.sign.update(msg);
        return new String( this.sign.sign() , "UTF-8");
       } catch( SignatureException |java.io.UnsupportedEncodingException se){} 
       return ""; 
   }
   
   X509Certificate getSelfSignCert(String dn, long ticks, String alg){
       return getSignCert(dn,dn,ticks,alg);
   }
   X509Certificate getSignCert(String dn, String cadn, long ticks, String alg){
       Date from = new Date();
       Date to = new Date(from.getTime() + ticks);
       
       cadn = ( cadn == null || cadn.isEmpty() )?dn:cadn;
       
       X509Certificate cert = null;
       
       final X509v3CertificateBuilder caBuilder = new X509v3CertificateBuilder(new X500Name(dn),
                BigInteger.valueOf(new SecureRandom().nextLong()), from, to, new X500Name(cadn),
                SubjectPublicKeyInfo.getInstance( ((ca==null)?getPublicKey():ca.getPublicKey()).getEncoded()));
       try {
         boolean b = (dn.equals(cadn));
         caBuilder.addExtension(X509Extension.basicConstraints, true, new BasicConstraints(b));
         caBuilder.addExtension(X509Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
       } catch ( CertIOException cio ) {
           log(1, "ERROR - Certificate Extension "+cio.getMessage()); 
       } 
       try {
        org.bouncycastle.jce.provider.BouncyCastleProvider bc = new org.bouncycastle.jce.provider.BouncyCastleProvider();   
        X509CertificateHolder caHolder = caBuilder
                 .build(new JcaContentSignerBuilder(alg).setProvider(bc).build(getPrivateKey()));

           cert = new JcaX509CertificateConverter().setProvider(bc).getCertificate(caHolder);
        
       }catch(CertificateException|OperatorCreationException ce){
           log(1, "ERROR - Certificate Operation -  "+ce.getMessage());  
       } 
      return cert;
   }
   
   X509Certificate getSelfSignCert(String dn, int days, String alg){
       long d = ( days < 1 )? 8*3600001 : days * 86400000l;
       return getSelfSignCert(dn,d,alg);
   } 
   
   public KeyStore openTrustStore(String file, String pass) { return openTrustStore(new File(file),pass); }
   public KeyStore openTrustStore(File file, String pass) {
       KeyStore kst = openKeystore(file, pass);
       File def = getDefaultTrustStore();
       if ( ! file.equals(getDefaultTrustStore())) {
           try { 
                kst.load(new FileInputStream(def), this.getDefaultTrustStorePass().toCharArray());
           } catch(IOException|NoSuchAlgorithmException|CertificateException|NullPointerException ne){
               log(1,"ERROR:"+ne.getMessage()+" - openTrustStore");
           }       
       }
       return kst;
   }
   
   public void updateKeyStoreWithDefault(KeyStore ks, File keystore, String KeysPW) {
       ks=( ks == null )? openKeystore(keystore,KeysPW ):ks ;
       try {
            if ( ks.size() == 0 ) {
                ks.setKeyEntry(getDefaultAlias(), this.getPrivateKey(), KeysPW.toCharArray() ,getChain());
                ks.store(new FileOutputStream(keystore), KeysPW.toCharArray());
            } 
       } catch ( KeyStoreException|IOException|NoSuchAlgorithmException|CertificateException|NullPointerException ne){
           log(1,"ERROR:"+ne.getMessage()+" - openKeystore fail");
       }
   }
   public KeyStore openKeystore(String file, String pass) { return openKeystore(new File(file),pass); }
   public KeyStore openKeystore(File file, String pass) {
       file=(file == null )? new File(".keystore"):file;
       pass=(pass == null || pass.isEmpty() )? "changeit":pass;
       KeyStore kst = null;
       log(1,"INFO: Keystore:"+file.getAbsolutePath()+":  =>|"+pass+"|<=");
       try {
            kst = KeyStore.getInstance(this.getStoreAlg());
       
            if ( file.exists() ) {
                log(1,"INFO: Keystore:"+file.getAbsolutePath()+":  like to load");
                kst.load(new FileInputStream(file), pass.toCharArray());
            } else {
                log(1,"INFO: Keystore:"+file.getAbsolutePath()+":  empty - create ");
                kst.load(null,null);
                updateKeyStoreWithDefault(kst,file, pass);
                kst.store(new FileOutputStream(file), pass.toCharArray());
            }
       } catch(KeyStoreException|IOException|NoSuchAlgorithmException|CertificateException|NullPointerException ne){
           log(1,"ERROR:"+ne.getMessage()+" - openKeystore fail");
       }     
       return kst;
   }
   
   public Certificate getCertificate(File fkst, String pass, String alias){
       return getCertificate(openKeystore(fkst,pass),alias);
   } 
   public Certificate getCertificate(KeyStore kst, String alias) {
        try {
            return kst.getCertificate(alias);
        } catch (KeyStoreException|NullPointerException ex) {
           
        }
        return null;
    }
 
    private File keyStore=null;
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
            return tcat.getDefaultPass();
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
            return tcat.getDefaultPass();
        }else {
             SecFile sn = new SecFile( keys.getAbsolutePath().replaceAll(".jks$", "")+".pw");
             if ( sn.isReadableFile() ) { return sn.readOut().toString(); }
        }
        return "changit";
    }
    File   getDefaultKeyStore()   { return new File( getTempDir()+File.separator+".keystore.jks"); }
    File   getDefaultTrustStore() { return getJavaCacerts(); }
    String getDefaultTrustStorePass() { return "changeit"; }
    
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
    }
    
    void createKeyStore(ReadFile fn, String pass) {
      try  {
        KeyStore ks = KeyStore.getInstance("JKS");
        if ( ! fn.isReadableFile() ) {
            ks.store(fn.getOutStream(), pass.toCharArray());
        }
      }catch ( KeyStoreException
              | java.io.IOException 
              |java.security.NoSuchAlgorithmException 
              |java.security.cert.CertificateException kse) {}  
        
    }
    
    KeyStore loadKeyStore(ReadFile fn, String pass) {
        try {
         KeyStore ks = KeyStore.getInstance("JKS");
                  ks.load(fn.getInputStream(), pass.toCharArray());
         return ks;
        } catch( KeyStoreException
              | java.io.IOException 
              |java.security.NoSuchAlgorithmException 
              |java.security.cert.CertificateException kse ) { return null; }
    }
    
   public void log(int deb, String msg){ super.log(deb, "TCATCert::"+msg); }
}
