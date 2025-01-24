package com.macmario.services.web.jproxy;

/**
 * Title:        jHttpProxyServer: Java HTTP Proxy
 * Description: starts proxy server
 */

public class jHttpProxyLauncher extends jHttpProxyVersion {

  static jHttpProxyServer server;

  public static void main(String[] args)
  {
	server = new jHttpProxyServer(true);
    	if (server.fatalError) {
    		System.out.println("Error: " +  server.getErrorMessage());
		}
    	else {
            if ( server.updateParameter(args) )  {
    		 server.starting();
    	   	 System.out.println("Running on port " + server.port);
            }  else {
                System.out.println("done.");
            }   
    	}
  }
}