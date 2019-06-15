/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tcvcog.tcvce.application;

import com.tcvcog.tcvce.coordinators.BlobCoordinator;
import com.tcvcog.tcvce.domain.IntegrationException;
import com.tcvcog.tcvce.entities.Blob;
import com.tcvcog.tcvce.integration.BlobIntegrator;
import com.tcvcog.tcvce.integration.PropertyIntegrator;
import com.tcvcog.tcvce.integration.ViolationIntegrator;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;

/**
 *
 * @author noah
 */
public class linkBlobBB extends BackingBeanUtils implements Serializable{
    
    private Blob selectedBlob;
    private int codeViolationID, propertyID;

    /**
     * Creates a new instance of linkBlobBB
     */
    public linkBlobBB() {
    }
    
    @PostConstruct
    public void initBean() {
    }
    
    public void linkBlobToCodeViolation() {
        BlobIntegrator bi = getBlobIntegrator();
        ViolationIntegrator vi = getCodeViolationIntegrator();
        
        try{
            vi.getCodeViolation(codeViolationID);
        }catch(IntegrationException e){
            getFacesContext().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR
                            ,"Unable to find Code Violation with that ID. " , ""));
            return;
        }
        
        try {
            bi.linkBlobToCodeViolation(selectedBlob.getBlobID(), propertyID);
        } catch (IntegrationException ex) {
            getFacesContext().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR
                            ,"Failed to link file to violation. Sorry! " , ""));
        }
    }
    
    public void linkBlobToProperty() {
        BlobIntegrator bi = getBlobIntegrator();
        PropertyIntegrator pi = getPropertyIntegrator();
        
        try{
            pi.getProperty(propertyID);
        }catch(IntegrationException e){
            getFacesContext().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR
                            ,"Unable to find Property with that ID. " , ""));
            return;
        }
        
        try {
            bi.linkBlobToProperty(selectedBlob.getBlobID(), propertyID);
        } catch (IntegrationException ex) {
            getFacesContext().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR
                            ,"Failed to link file to Property. Sorry! " , ""));
        }
    }
    
    public String navToDash() {
        return "missionControl";
    }

    /**
     * @return the codeViolationID
     */
    public int getCodeViolationID() {
        return codeViolationID;
    }

    /**
     * @param codeViolationID the codeViolationID to set
     */
    public void setCodeViolationID(int codeViolationID) {
        this.codeViolationID = codeViolationID;
    }

    /**
     * @return the propertyID
     */
    public int getPropertyID() {
        return propertyID;
    }

    /**
     * @param propertyID the propertyID to set
     */
    public void setPropertyID(int propertyID) {
        this.propertyID = propertyID;
    }

    /**
     * @return the selectedBlob
     */
    public Blob getSelectedBlob() {
        return selectedBlob;
    }

    /**
     * @param selectedBlob the selectedBlob to set
     */
    public void setSelectedBlob(Blob selectedBlob) {
        this.selectedBlob = selectedBlob;
    }
}
