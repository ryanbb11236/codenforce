/*
 * Copyright (C) 2017 cedba
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.tcvcog.tcvce.entities;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author cedba
 */
public class User implements Serializable{

    protected int userID;
    protected String username;
    
    protected Person person;
    protected int personID;

    protected String notes;
    
    protected String badgeNumber;
    protected String oriNumber;
    
    protected boolean active;
    protected int createdByUserId;
    protected LocalDateTime createdTS;
    protected boolean noLoginVirtualUser;
    protected LocalDateTime pswdLastUpdated;    
    protected LocalDateTime forcePasswordResetTS;
    
    
    /**
     * Creates a new instance of User
     */
    public User() {
    }
    
    /**
     * Creates a new instance of User
     * @param id
     */
    public User(int id) {
        userID = id;
    }
    
    public void setUserID(int uid){
        userID = uid;
    }

    /**
     * @return the userID
     */
    public int getUserID() {
        return userID;
    }

  
    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    
   
    /**
     * @return the notes
     */
    public String getNotes() {
        return notes;
    }

    /**
     * @param notes the notes to set
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }
    // no setters for access permissions private variables!!
    /**
     * @return the keyCard
     */
    /**
     * @param keyCard the keyCard to set
     */   
    
    

   
    /**
     * @return the person
     */
    public Person getPerson() {
        return person;
    }

    /**
     * @param person the person to set
     */
    public void setPerson(Person person) {
        this.person = person;
    }


    /**
     * @return the badgeNumber
     */
    public String getBadgeNumber() {
        return badgeNumber;
    }

    /**
     * @return the oriNumber
     */
    public String getOriNumber() {
        return oriNumber;
    }

    /**
     * @param badgeNumber the badgeNumber to set
     */
    public void setBadgeNumber(String badgeNumber) {
        this.badgeNumber = badgeNumber;
    }

    /**
     * @param oriNumber the oriNumber to set
     */
    public void setOriNumber(String oriNumber) {
        this.oriNumber = oriNumber;
    }

    /**
     * @return the personID
     */
    public int getPersonID() {
        if(person != null){
            personID = person.getPersonID();
        }
        return personID;
    }

    /**
     * @param personID the personID to set
     */
    public void setPersonID(int personID) {
        this.personID = personID;
    }

  

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 43 * hash + this.userID;
        hash = 43 * hash + Objects.hashCode(this.username);
        hash = 43 * hash + Objects.hashCode(this.person);
        hash = 43 * hash + this.personID;
        hash = 43 * hash + Objects.hashCode(this.notes);
        hash = 43 * hash + Objects.hashCode(this.badgeNumber);
        hash = 43 * hash + Objects.hashCode(this.oriNumber);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final User other = (User) obj;
        if (this.userID != other.userID) {
            return false;
        }
       
        return true;
    }

    /**
     * @return the createdByUserId
     */
    public int getCreatedByUserId() {
        return createdByUserId;
    }

    /**
     * @return the createdTS
     */
    public LocalDateTime getCreatedTS() {
        return createdTS;
    }

    /**
     * @return the pswdLastUpdated
     */
    public LocalDateTime getPswdLastUpdated() {
        return pswdLastUpdated;
    }

    /**
     * @return the active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * @return the noLoginVirtualUser
     */
    public boolean isNoLoginVirtualUser() {
        return noLoginVirtualUser;
    }

    /**
     * @param active the active to set
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * @param createdByUserId the createdByUserId to set
     */
    public void setCreatedByUserId(int createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    /**
     * @param createdTS the createdTS to set
     */
    public void setCreatedTS(LocalDateTime createdTS) {
        this.createdTS = createdTS;
    }

    /**
     * @param noLoginVirtualUser the noLoginVirtualUser to set
     */
    public void setNoLoginVirtualUser(boolean noLoginVirtualUser) {
        this.noLoginVirtualUser = noLoginVirtualUser;
    }

    /**
     * @param pswdLastUpdated the pswdLastUpdated to set
     */
    public void setPswdLastUpdated(LocalDateTime pswdLastUpdated) {
        this.pswdLastUpdated = pswdLastUpdated;
    }

    

    /**
     * @return the forcePasswordResetTS
     */
    public LocalDateTime getForcePasswordResetTS() {
        return forcePasswordResetTS;
    }

    /**
     * @param forcePasswordResetTS the forcePasswordResetTS to set
     */
    public void setForcePasswordResetTS(LocalDateTime forcePasswordResetTS) {
        this.forcePasswordResetTS = forcePasswordResetTS;
    }
    /**
     * @return the accessRecord
     */
    /**
     * @param accessRecord the accessRecord to set
     */

}
