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
package com.tcvcog.tcvce.coordinators;

import com.tcvcog.tcvce.application.BackingBeanUtils;
import com.tcvcog.tcvce.domain.AuthorizationException;
import com.tcvcog.tcvce.domain.IntegrationException;
import java.io.Serializable;
import com.tcvcog.tcvce.entities.AccessKeyCard;
import com.tcvcog.tcvce.entities.Municipality;
import com.tcvcog.tcvce.entities.RoleType;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import com.tcvcog.tcvce.entities.User;
import com.tcvcog.tcvce.integration.MunicipalityIntegrator;
import com.tcvcog.tcvce.integration.UserIntegrator;
import java.util.ArrayList;

/**
 *
 * @author cedba
 */
@ApplicationScoped
@Named("userCoordinator")
public class UserCoordinator extends BackingBeanUtils implements Serializable {
    
    /**
     * Creates a new instance of UserCoordinator
     */
    public UserCoordinator(){
    
    }    
   
    /**
     * Primary user retrieval method: Note that there aren't as many checks here
     * since the jboss container is managing the lookup of authenticated users. 
     * We are pulling the login name from the already authenticated glassfish user 
     * and just grabbing their profile from the db
     * 
     * @param loginName
     * @return the fully baked cog user
     * @throws IntegrationException 
     * @throws com.tcvcog.tcvce.domain.AuthorizationException occurs if the user
     * has been retrieved from the database but their access has been toggled off
     */
    public User getUser(String loginName) throws IntegrationException, AuthorizationException{
        System.out.println("UserCoordinator.getUser | given: " + loginName );
        User authenticatedUser;
        UserIntegrator ui = getUserIntegrator();
        authenticatedUser = ui.getUser(loginName);
        // integrator sets high level system access permissions
        if(authenticatedUser.isSystemAccessPermitted()){
            // set user permissions with the role type that comes from the DB
            // which the Integrator sets
            authenticatedUser.setKeyCard(acquireAccessKeyCard(authenticatedUser.getRoleType()));
            return authenticatedUser;
            
        } else {
            
            throw new AuthorizationException("User exists but access to system "
                    + "has been switched off. If you believe you are receiving "
                    + "this message in error, please contact system administrator "
                    + "Eric Darsow at 412.923.9907.");
        }
    }
    
    /**
     * Container for all access control mechanism authorization switches
     * 
     * Design goal: have boolean type getters on users for use by the View
     * to turn on and off rendering and enabling as needed.
     * 
     * NOTE: This is the ONLY method system wide that calls any setters for
     * access permissions
     * 
     * @param rt
     * @return a User object whose access controls switches are configured
     */
    private AccessKeyCard acquireAccessKeyCard(RoleType rt){
        AccessKeyCard card = null;
        
        switch(rt){
            case Developer:
                card = new AccessKeyCard( true,   //developer
                                    true,   // sysadmin
                                    true,   // cogstaff
                                    true,   // enfOfficial
                                    true,   // muniStaff
                                    true);  // muniReader
               break;
            
            case SysAdmin:
                card = new AccessKeyCard( false,   //developer
                                    true,   // sysadmin
                                    true,   // cogstaff
                                    true,   // enfOfficial
                                    true,   // muniStaff
                                    true);  // muniReader
               break;               
               
            case CogStaff:
                card = new AccessKeyCard( false,   //developer
                                    false,   // sysadmin
                                    true,   // cogstaff
                                    false,   // enfOfficial
                                    true,   // muniStaff
                                    true);  // muniReader
               break;               
               
            case EnforcementOfficial:
                card = new AccessKeyCard( false,   //developer
                                    false,   // sysadmin
                                    false,   // cogstaff
                                    true,   // enfOfficial
                                    true,   // muniStaff
                                    true);  // muniReader
               break;
               
            case MuniStaff:
                card = new AccessKeyCard( false,   //developer
                                    false,   // sysadmin
                                    false,   // cogstaff
                                    false,   // enfOfficial
                                    true,   // muniStaff
                                    true);  // muniReader
               break;
               
            case MuniReader:
                card = new AccessKeyCard( false,   //developer
                                    false,   // sysadmin
                                    false,   // cogstaff
                                    false,   // enfOfficial
                                    false,   // muniStaff
                                    true);  // muniReader
               break;               
               
            default:
               
        }        
        return card;
    }    
    
    /**
     * Return a list of Municipalities that User u does not currently have the
     * authorization to access.
     * @param u the user in question
     * @return A list of Municipality objects that the user does not have 
     *     access to  
     * @throws IntegrationException 
     */
    public ArrayList<Municipality> getUnauthorizedMunis(User u) throws IntegrationException {
        System.out.println("UserCoordinator.getUnauthorizedMunis | " + u.getUsername());
        
        UserIntegrator ui = getUserIntegrator();
        ArrayList<Municipality> authMunis = ui.getUserAuthMunis(u.getUserID());        
        MunicipalityIntegrator mi = getMunicipalityIntegrator();
        ArrayList<Municipality> munis = mi.getCompleteMuniList();
        
        if(authMunis != null){
            for(Municipality authMuni:authMunis){
                munis.remove(authMuni);
            }
        }        
        return munis;
    }
    
} // close class
