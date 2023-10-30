/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.macmario.services.web.tcat;

/**
 *
 * @author SuMario
 */
abstract public class TCatVersion extends com.macmario.general.Version {
    
     public int debug=1;    
 
     
    public void log(int level, StringBuilder sw) { log(level, sw.toString()); }
    public void log(int level, String sw) {
       if ( level <= debug )
            System.out.println("DEBUG["+level+"/"+debug+"] - "+sw );
    }
    
    public boolean getBoolean(String s){
        return ( s!= null && ( s.equals("1") || s.toLowerCase().equals("true") ) );
    }
    
    public int getInt(String s){
        int ret=-1;
        try { ret=Integer.getInteger(s); }catch(NullPointerException|NumberFormatException ne){}
        return ret;
    }
}
