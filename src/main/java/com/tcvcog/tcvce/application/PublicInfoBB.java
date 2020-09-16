/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tcvcog.tcvce.application;

import com.tcvcog.tcvce.coordinators.PublicInfoCoordinator;
import com.tcvcog.tcvce.domain.AuthorizationException;
import com.tcvcog.tcvce.domain.BObStatusException;
import com.tcvcog.tcvce.domain.EventException;
import com.tcvcog.tcvce.domain.IntegrationException;
import com.tcvcog.tcvce.domain.SearchException;
import com.tcvcog.tcvce.domain.ViolationException;
import com.tcvcog.tcvce.entities.PublicInfoBundle;
import com.tcvcog.tcvce.entities.PublicInfoBundleCECase;
import java.io.Serializable;
import java.util.List;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;

/**
 *
 * @author sylvia
 */
public class PublicInfoBB extends BackingBeanUtils implements Serializable{

    private List<PublicInfoBundle> publicInfoBundleList;
    private PublicInfoBundle selectedBundle;
    private int submittedPACC;
    private String publicMessage;
    
    
    /**
     * Creates a new instance of CEPublicAccessBB
     */
    public PublicInfoBB() {
    }
    
    public void initBean(){
        
    }
    
    public String submitPACC(){
        PublicInfoCoordinator pic = getPublicInfoCoordinator();
        try {
            publicInfoBundleList = pic.getPublicInfoBundles(submittedPACC);
            if(!publicInfoBundleList.isEmpty()){
                getFacesContext().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO,
                                "Retrieved " + String.valueOf(publicInfoBundleList.size()) + " bundles!",""));
                
                //Now look through the bundles and see which interface we need to send the user to
                for(PublicInfoBundle bundle : publicInfoBundleList){
                    
                    if(bundle.getTypeName().equalsIgnoreCase("CECASE") 
                            || bundle.getTypeName().equalsIgnoreCase("CEAR")) {
                        //Code enforcement it is!
                        return "cePaccView";
                    }
                    
                    if(bundle.getTypeName().equalsIgnoreCase("OccPermitApplication")
                            || bundle.getTypeName().equalsIgnoreCase("OccInspection") ){
                        //Occupancy it is!
                        return "occPaccView";
                    }
                }
            } else {
                
                getFacesContext().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO,
                                "No info bundles found for this control code",""));
                publicInfoBundleList = null;
            }
        } catch (IntegrationException ex) {
            System.out.println("PublicInfoBB.submitPacc() | ERROR: " + ex.toString());  
            getFacesContext().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Unable to search for info bundles, sorry!", "This is a system error."));
        } catch ( SearchException 
                | EventException 
                | AuthorizationException 
                | ViolationException ex) {
            System.out.println("PublicInfoBB.submitPacc() | ERROR: " +ex);
        } catch (BObStatusException ex){
            getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage(), ""));
        }
        
        //something went wrong, try again
        return "";
        
    }
    
    public String viewPACCRecordDetails(PublicInfoBundle pib){
        if(pib instanceof PublicInfoBundleCECase){
            PublicInfoBundleCECase pibCase = (PublicInfoBundleCECase) pib;
            getSessionBean().setPibCECase(pibCase);
            return "publicInfoCECase";
            
        }
        return "";
    }
    
    public void attachMessage(ActionEvent ev){
        PublicInfoCoordinator pic = getPublicInfoCoordinator();
        try {
            pic.attachMessageToBundle(selectedBundle, publicMessage);
            getFacesContext().addMessage(null,
                  new FacesMessage(FacesMessage.SEVERITY_INFO,
                          "Public case note added", ""));

        } catch (IntegrationException ex) {
            System.out.println(ex);
              getFacesContext().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Unable to attach messages at this time, sorry!", 
                            "This is a system error and has been logged for debugging."));
        
        }
        publicMessage = null;
        
        
    }
    
    

    /**
     * @return the publicInfoBundleList
     */
    public List<PublicInfoBundle> getPublicInfoBundleList() {
        return publicInfoBundleList;
    }

    /**
     * @return the submittedPACC
     */
    public int getSubmittedPACC() {
        return submittedPACC;
    }

    /**
     * @param publicInfoBundleList the publicInfoBundleList to set
     */
    public void setPublicInfoBundleList(List<PublicInfoBundle> publicInfoBundleList) {
        this.publicInfoBundleList = publicInfoBundleList;
    }

    /**
     * @param submittedPACC the submittedPACC to set
     */
    public void setSubmittedPACC(int submittedPACC) {
        this.submittedPACC = submittedPACC;
    }

    /**
     * @return the selectedBundle
     */
    public PublicInfoBundle getSelectedBundle() {
        return selectedBundle;
    }

    /**
     * @param sb
     */
    public void setSelectedBundle(PublicInfoBundle sb) {
        System.out.println("PublicInfoBB.setSelectedBundle | Bundle type: " + sb.getTypeName());
        this.selectedBundle = sb;
    }

    /**
     * @return the publicMessage
     */
    public String getPublicMessage() {
        return publicMessage;
    }

    /**
     * @param publicMessage the publicMessage to set
     */
    public void setPublicMessage(String publicMessage) {
        this.publicMessage = publicMessage;
    }
    
    
    public String goToIntensityManage(){
        
        return "intensityManage";
    }
    
    
}
