/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.macmario.services.web.tcat;

import com.macmario.io.file.ReadFile;
import com.macmario.io.thread.RunnableT;
import jakarta.servlet.ServletContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.catalina.Context;

/**
 *
 * @author MNO
 */
class TCatAppChecker extends RunnableT {
    
    private final HashMap<String, ArrayList<Object>> map = new HashMap<>();
    private final String lock="TCatAppCheckerLock";
    private final TCat tc;
    
    TCatAppChecker(TCat tc) {
        this.tc=tc;
    }
    
    void register( ReadFile f, Context c) {
        String m = c.getServletContext().getContextPath();
        ArrayList<Object> ar = new ArrayList<>();
                          ar.add(f); ar.add(c);
        synchronized(lock) {
            map.put(m, ar);
            System.out.println("like to check:"+c.getPath()+" with:"+f.getFQDNFileName());
        }
    }

    @Override
    public void run() {
        setRunning();
        long d = System.currentTimeMillis();
        long m = d;
        while( ! isClosed() ) {
            sleep(3000);
            synchronized(lock) {
               long l = System.currentTimeMillis();
               if ( ! map.isEmpty() && (d+30000) < l ) {
                    //System.out.println("start: d:"+d+" l:"+l+"  ="+(l-d));
                    Iterator<String> itter =map.keySet().iterator();
                    while( itter.hasNext() ) {
                         ArrayList<Object> ar = map.get(itter.next());
                         ReadFile f = (ReadFile) ar.get(0);
                         if ( f.isModified(d) ) {
                              Context c =(Context)  ar.get(1);
                              if ( tc.allowRedeploy() ) {
                                System.out.println("INFO: auto redeployment for "+c.getName());
                                tc.redeployApp(f,  c );
                              } else {
                                System.out.println("WARN: need deployment for "+c.getName());
                              }
                              m=System.currentTimeMillis();
                         }
                    }
                    if ( m > d ) { d=m; }
               } 
            }
        }
        setClosed();
    }
    
}
