package com.macmario.services.web.jproxy;

import javax.sql.DataSource;

/**
 *
 * @author SuMario
 */
public class jHttpProxyUserDB extends org.apache.catalina.users.DataSourceUserDatabase {
    
    public jHttpProxyUserDB(DataSource dataSource, String id) {
        super(dataSource, id);
        init();
    }
 
    private void init() {
        this.createGroup("admingrp", "administration group");
        this.createGroup("everyone", "anonymouse group");
        this.createRole("adminrole", "admin role");
        this.createRole("everyone", "everyone role");
        this.createUser("guest", "guest", "erveryone");
    }
}
