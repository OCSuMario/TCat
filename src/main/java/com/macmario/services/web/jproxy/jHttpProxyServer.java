package com.macmario.services.web.jproxy;


import java.util.Vector;

public class jHttpProxyServer extends jHttpProxyServerRessource {

	public jHttpProxyServer()  {
                super();
                this.server=this;
                init();
	}

	public jHttpProxyServer(boolean b) {
                this();
                console=b;
                if ( b )
		     System.out.println(version.SERVERNAME+" HTTP Proxy Server Version "
                                                  + version.getServerVersion() + "\r\n"
						  + "Copyright (c) 2025 "+"\r\n");
		
	}

		
	public void AuthenticateUser(String u, String p) {
                if ( confAdmin.isValidAuthenticate(u,p) ) {
			confAuth = 1;
		} else {
			confAuth = 0;
                }
	}

	
	public jHttpProxyURLMatch findMatch(String url) {
		jHttpProxyURLMatch b = (jHttpProxyURLMatch) dic.get(url);
                log(1, "jHttpProxyServer:: jHttpProxyURLMatch ->"+b);
                return b;
	}

	public WildcardDictionary getWildcardDictionary() {
		return dic;
	}

	public Vector<OnURLAction> getURLActions() {
		return urlactions;
	}

	

	

}