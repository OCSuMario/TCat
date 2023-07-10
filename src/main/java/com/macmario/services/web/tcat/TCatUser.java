/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.macmario.services.web.tcat;

import java.util.ArrayList;
import java.util.Iterator;
import org.apache.catalina.Group;
import org.apache.catalina.Role;

/**
 *
 * @author SuMario
 */
public class TCatUser extends org.apache.catalina.users.AbstractUser {
    final private TCatUserDb db;
    
    TCatUser(TCatUserDb db){
        this.db=db;
    }

    private Iterator<Group> filterUserGroups(Iterator<Group> gr) {
        
        ArrayList<Group> ar = new ArrayList<>();
        while( gr.hasNext()){
            Group g = gr.next();            
            if ( isInGroup(g) ) { ar.add(g); }
        }
        
        return (Iterator<Group>) ar.iterator();
    }
    
    @Override
    public Iterator<Group> getGroups() {
        Iterator<Group> gr = filterUserGroups(db.getGroups());
    
        return gr;
    }

    @Override
    public Iterator<Role> getRoles() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void addGroup(Group group) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void addRole(Role role) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean isInGroup(Group group) {
        return db.getUserGroupTable().contains(group.getGroupname());
    }

    @Override
    public boolean isInRole(Role role) {
        return db.getUserRoleTable().contains(role.getRolename());
    }

    @Override
    public void removeGroup(Group group) {
        db.removeGroup(group);
    }

    @Override
    public void removeGroups() {
        Iterator<Group> itter = db.getGroups();
        while (itter.hasNext() ) {
              removeGroup(itter.next());
        }
    }

    @Override
    public void removeRole(Role role) {
         db.removeRole(role);
    }

    @Override
    public void removeRoles() {
        Iterator<Role> itter = db.getRoles();
        while (itter.hasNext() ) {
              removeRole(itter.next());
        }
    }

    @Override
    public TCatUserDb getUserDatabase() {
        return this.db;
    }
    
}
