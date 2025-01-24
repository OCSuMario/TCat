/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.macmario.services.web.jproxy;

import com.macmario.io.account.User;

/**
 *
 * @author sumario
 */
public class jHttpProxyUser extends User{

    private final jHttpProxyServer server;
    
    public jHttpProxyUser(String user, String pass, jHttpProxyServer server) {
        super(user, pass);
        this.server = server;
    }

    boolean isValidAuthenticate(String u, String p) {
          //String ug=getCrypted(u);  String pg=getCrypted(p);
          
          if ( u != null  && u.equals(getUsername()) &&
               p != null  && p.equals(getPassword())   ) {
                    return true;
          }
          return false;
    }
    
}
