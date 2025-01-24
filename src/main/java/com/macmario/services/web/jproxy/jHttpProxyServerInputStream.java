package com.macmario.services.web.jproxy;

import java.io.BufferedInputStream;
import java.io.InputStream;

public class jHttpProxyServerInputStream extends BufferedInputStream implements jHttpProxyInputStreamInf
{
	private jHttpSession conn;

	public jHttpProxyServerInputStream(jHttpProxyServer server, jHttpSession conn, 
                                                     InputStream a, boolean filter     ) 
        {
		super(a);
		this.conn = conn;
	}

        @Override
	public int read_f(byte[] b) throws java.io.IOException {
		return read(b);
	}
}

