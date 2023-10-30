/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.macmario.services.web.tcat;

import com.macmario.general.Version;
import java.security.cert.X509Certificate;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Date;


/**
 *
 * @author MNO
 */
class TCatCert extends Version {

    private String alg;
    private int keyLength;
    private KeyPairGenerator keyPairGen;
    private KeyPair pair;
    private String signAlg;
    private Signature sign;
    private CertificateFactory cf;
    
   TCatCert(                        ) { this("DSA",512,"SHA256withDSA"); }
   TCatCert(String alg, int kLength,String signAlg ) { init(alg,kLength,signAlg);} 
   
   private void init(String alg, int kLength,String signAlg) {
      final String func="TCatCert::init()";
      this.alg=alg;
      this.keyLength=kLength;
      this.signAlg=signAlg;
      try {
        this.cf =  CertificateFactory.getInstance("X.509");
        //this.cf.generateCertificate(inStream);
       
        this.keyPairGen=KeyPairGenerator.getInstance(this.alg);
        this.keyPairGen.initialize(kLength);
        this.pair = this.keyPairGen.generateKeyPair();
        this.sign = Signature.getInstance(signAlg);
        this.sign.initSign(getPrivateKey());
        
      }catch ( NoSuchAlgorithmException 
              | CertificateException 
              | InvalidKeyException 
              | NullPointerException nsa ) {
          printf(func,1,"error initialize - "+nsa.getLocalizedMessage());
      }  
   }
   
   PrivateKey getPrivateKey(){ return pair.getPrivate() ; }
   PublicKey  getPublicKey(){ return pair.getPublic() ; }
   
   public String getSignature(String msg){ return getSignature(msg.getBytes()); }
   public String getSignature(byte[] msg){
       try {
        this.sign.update(msg);
        return new String( this.sign.sign() , "UTF-8");
       } catch( SignatureException |java.io.UnsupportedEncodingException se){} 
       return ""; 
   }
   
   X509Certificate getSelfSignCert(String dn, int days, String alg){
       long d = ( days < 1 )? 8*3600001 : days * 86400000l;
       Date from = new Date();
       Date to = new Date(from.getTime() + d);
      /* PrivateKey  privkey = getPrivateKey();
       CertInfo info = new CertInfo(); 
       
       
       Date from = new Date();
       Date to = new Date(from.getTime() + days * 86400000l);
       
        CertValidator interval = new CertValidator(from, to);
        BigInteger sn = new BigInteger(64, new SecureRandom());
        X500Name owner = new X500Name(dn);
 
        info.set(CertInfo.VALIDITY, interval);
        info.set(CertInfo.SERIAL_NUMBER, new CertSerialNumber(sn));
        info.set(CertInfo.SUBJECT, new CertSubjectName(owner));
        info.set(CertInfo.ISSUER, new CertificateIssuerName(owner));
        info.set(CertInfo.KEY, new CertificateX509Key(pair.getPublic()));
        info.set(CertInfo.VERSION, new CertVersion(CertVersion.V3));
  AlgorithmId algo = new AlgorithmId(AlgorithmId.md5WithRSAEncryption_oid);
        info.set(CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));
 
  // Sign the cert to identify the algorithm that's used.
  CertImpl cert = new CertImpl(info);
  cert.sign(privkey, algorithm);
 
  // Update the algorith, and resign.
  algo = (AlgorithmId)cert.get(X509CertImpl.SIG_ALG);
  info.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, algo);
  cert = new CertImpl(info);
  cert.sign(privkey, algorithm); */
        X509Certificate cert = null;
        X509Certificate[] serverChain = new X509Certificate[1];
        
        //X509V3CertificateGenerator servCertGen = new X509V3CertificateGenerator();
        
        return cert;
   } 
}
