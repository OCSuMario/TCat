/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.macmario.services.web.tcat;

import com.macmario.general.MyVersion;
import com.macmario.general.Version;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 *
 * @author SuMario
 */
class TCatRessources extends Version {
    
    TCatRessources() {
        System.setProperty("org.apache.catalina.startup.EXIT_ON_INIT_FAILURE",  "true");
        System.setProperty("tomcat.util.scan.StandardJarScanFilter.jarsToSkip", "*.jar");
        
    }
    
    void out(String info) {
         System.out.println(info);
    }
    
    InputStream getFromRessource(String res) {
         return getClass().getClassLoader().getResourceAsStream(res);
    }
    
    String loadStringFromRessource(String res) {
        StringBuilder sw= new StringBuilder();
        InputStream in = getFromRessource(res);
        String line;
        try {    
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                while ((line = br.readLine()) != null) {
                    sw.append(line).append("\n");
                }
            }
        } catch(IOException | NullPointerException  io) {}    
        return sw.toString();
    }
    
    Document getXMLFromStream(InputStream in) {
        Document doc=null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Create the builder and parse the file
            doc = factory.newDocumentBuilder().parse(in);
        } catch(ParserConfigurationException|IOException|SAXException|NullPointerException pce) {
            doc = getNewXmlDocument("error", pce.getMessage());
        }
        return doc;
    }
    Document getXMLFromFile(File f) { 
        try { 
            return getXMLFromStream( new java.io.FileInputStream(f)); 
        }catch(IOException io ) {
            return getNewXmlDocument("error", io.getMessage());
        }
    }
    Document getXMLFromRessource(String res) { return getXMLFromStream( getFromRessource(res) ); }
    
    Document getNewXmlDocument(String l1, String l2) {
        Document doc =null;
        try {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        
        InputStream in  = new ByteArrayInputStream(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\t<info key=\""+l1+"\" value=\""+l2+"\" />\n</xml>\n").getBytes(StandardCharsets.UTF_8));
                       builder.parse(in);
            doc = builder.newDocument();
        }catch(NullPointerException|IOException|ParserConfigurationException|SAXException ne) {}    
        return doc;
    }
    
    String getDefaultPass() {
        
        MyVersion m = new MyVersion();
        
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(  ("Uost:"+super.getHostKey()+": User:"+super.getUserKey()+": Jar:"+m.getLocationMD5() ).getBytes()  );
        
            byte[] mdbytes = md.digest();

            //convert the byte to hex format method 1
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mdbytes.length; i++) {
              sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch( java.security.NoSuchAlgorithmException io ) {}
        return m.getLocationMD5();
        
    }
}
