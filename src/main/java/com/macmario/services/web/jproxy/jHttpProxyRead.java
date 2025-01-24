package com.macmario.services.web.jproxy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;


public class jHttpProxyRead extends jHttpProxyRunnableT {
	private final int BUFFER_SIZE = 65535;
	private BufferedInputStream in;
	private BufferedOutputStream out;
	private jHttpSession conn;
	private jHttpProxyServer server;
        private int id=0;
        
	public jHttpProxyRead(jHttpProxyServer server, jHttpSession conn,
			       BufferedInputStream in, BufferedOutputStream out ) {
                this.in     = in;
		this.out    = out;
		this.conn   = conn;
		this.server = server;
                
                this.id = conn.id; 
		setPriority(Thread.MIN_PRIORITY);
		start();
	}

        @Override
	public void run() {
                setRunning();

                int read = 0;
		byte[] buf = new byte[BUFFER_SIZE];
                int pread=read;
		try {
		    while ( (read = in.read(buf)) > -1 ) {
			out.write(buf, 0, read);
			out.flush();
			server.addBytesRead(read);
                        pread += read;
                        log(2,"read [ "+id+" ] "+read+"/"+pread+" bytes to target");
		    }
		} catch (java.io.IOException io) {
                    log(1,"read [ "+id+" ] with exception - "+io.getMessage());
		}
                log(1,"read/write [ "+id+" ] "+pread+" bytes to target");
		try {
		    if (conn.getStatus() != conn.SC_CONNECTING_TO_HOST) {
			conn.getLocalSocket().close();
                    }    
		} catch (java.io.IOException io) {
                    log(1,"read socket close [ "+id+" ] with exception - "+io.getMessage());
		}
		
		buf     = null;
		server  = null;
		conn    = null;
		in      = null;
		out     = null;
		
	}

	public void close() {
		try {
			in.close();
		} catch (java.io.IOException|NullPointerException io) {
		}
                setClosed();
	}
}
