/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.macmario.services.web.tcat;

/**
 *
 * @author MNO
 */
public class TCatConnector extends org.apache.catalina.connector.Connector{

    private final TCat tcat;
    private final TCatCert cert;
    
    public TCatConnector(TCat tcat) {
        this.tcat=tcat;
        this.cert=tcat.cert;
    }
    public TCatConnector(String proto, TCat tcat) {
        super(proto);
        this.tcat=tcat;
        this.cert=tcat.cert;
    }
}
