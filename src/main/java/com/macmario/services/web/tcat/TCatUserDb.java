/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.macmario.services.web.tcat;

import javax.sql.DataSource;

/**
 *
 * @author SuMario
 */
public class TCatUserDb extends org.apache.catalina.users.DataSourceUserDatabase {
    
    public TCatUserDb(DataSource dataSource, String id) {
        super(dataSource, id);
        init();
    }
 
    private void init() {
        this.createGroup("LocalAdmin", "local administration");
        this.createGroup("everyone", "anonymouse group");
        this.createRole("everyone", "everyone role");
        this.createUser("guest", "guest", "erveryone");
        
    }
}
