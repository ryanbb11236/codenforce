/*
 * Copyright (C) 2019 Technology Rediscovery LLC
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
package com.tcvcog.tcvce.entities.occupancy;

import com.tcvcog.tcvce.application.interfaces.IFace_EventRuleGoverned;
import com.tcvcog.tcvce.entities.*;
import com.tcvcog.tcvce.util.viewoptions.ViewOptionsEventRulesEnum;
import com.tcvcog.tcvce.util.viewoptions.ViewOptionsProposalsEnum;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The Data Intensive subclass of the OccPeriod tree
 * We want to be able to load info about OccPeriods without having to 
 * initialize and configure each and every event and proposal and rule
 * associated with each occ period on each unit on each property
 * that we browse, so we'll only load the DataHeavy version of this
 * Object if we're actually editing that particular OccPeriod
 * @author sylvia
 */
public  class       OccPeriodDataHeavy 
        extends     OccPeriodPropertyUnitHeavy
        implements  IFace_EventRuleGoverned, 
                    IFace_CredentialSigned,
                    IFace_PaymentHolder {
    
    protected OccPeriodStatusEnum status;

    private List<OccPermitApplication> applicationList;
    private List<PersonOccApplication> personListApplicants;
    private List<Person> personList;
    
    private List<Proposal> proposalList;
    private List<EventRuleImplementation> eventRuleList;
    
    private List<OccInspection> inspectionList;
    private List<OccPermit> permitList;
    
    private List<Integer> blobIDList;
    
    private List<FeeAssigned> feeList;
    private List<Payment> paymentList;

    private LocalDateTime configuredTS;
    private String credentialSignature;
    
    public OccPeriodDataHeavy() {
    }
    
    
    /**
     * Populates superclass members and stamps the 
     * authorizing Credential's signature
     * 
     * @param otherPeriodLight
     * @param cred 
     */
    public OccPeriodDataHeavy(OccPeriod otherPeriodLight, Credential cred) {
        super(otherPeriodLight);

        this.credentialSignature = cred.getSignature();
    }

    /**
     * Populates superclass members and stamps the
     * authorizing Credential's signature
     * This one is for a superclass with property unit info, though.
     *
     * @param otherPeriodLighter
     * @param cred
     */
    public OccPeriodDataHeavy(OccPeriodPropertyUnitHeavy otherPeriodLighter, Credential cred) {
        super(otherPeriodLighter);

        this.credentialSignature = cred.getSignature();
    }

    /**
     * Complete copy of another OccPeriodDataHeavy, including credential signature
     *
     * @param otherPeriod
     */
    public OccPeriodDataHeavy(OccPeriodDataHeavy otherPeriod) {
        super(otherPeriod);

        this.status = otherPeriod.status;

        this.applicationList = otherPeriod.applicationList;
        this.personListApplicants = otherPeriod.personListApplicants;
        this.personList = otherPeriod.personList;

        this.proposalList = otherPeriod.proposalList;
        this.eventRuleList = otherPeriod.eventRuleList;

        this.inspectionList = otherPeriod.inspectionList;
        this.permitList = otherPeriod.permitList;

        this.blobIDList = otherPeriod.blobIDList;

        this.feeList = otherPeriod.feeList;
        this.paymentList = otherPeriod.paymentList;

        this.configuredTS = otherPeriod.configuredTS;
        this.credentialSignature = otherPeriod.credentialSignature;
    }

    /**
     * Pre-credential requiring method for creating detailed subclass
     *
     * @param otherPeriodLight
     */
    public OccPeriodDataHeavy(OccPeriod otherPeriodLight) {
        super(otherPeriodLight);
    }

    
    @Override
    public DomainEnum discloseEventDomain() {
        return DomainEnum.OCCUPANCY;
    }

    
    @Override
    public void setEventRuleList(List<EventRuleImplementation> lst) {
        eventRuleList = lst;
    }
    
    
    @Override
    public boolean isAllRulesPassed() {
        boolean allPassed = true;
        for(EventRuleImplementation er: eventRuleList){
            if(er.getPassedRuleTS() == null){
                allPassed = false;
                break;
            }
        }
        return allPassed;
    }

    

    @Override
    public List assembleEventRuleList(ViewOptionsEventRulesEnum voere) {
        List<EventRuleImplementation> evRuleList = new ArrayList<>();
        if (eventRuleList != null) {
            for (EventRuleImplementation eri : eventRuleList) {
                switch (voere) {
                    case VIEW_ACTIVE_NOT_PASSED:
                        if (eri.isActiveRuleAbstract() && eri.getPassedRuleTS() == null) {
                            evRuleList.add(eri);
                        }
                        break;
                    case VIEW_ACTIVE_PASSED:
                        if (eri.isActiveRuleAbstract() && eri.getPassedRuleTS() != null) {
                            evRuleList.add(eri);
                        }
                        break;
                    case VIEW_ALL:
                        evRuleList.add(eri);
                        break;
                    case VIEW_INACTIVE:
                        if (!eri.isActiveRuleAbstract()) {
                            evRuleList.add(eri);
                        }
                        break;
                    default:
                        evRuleList.add(eri);
                } // close switch
            } // close loop
        } // close null check
        return evRuleList;
    }

    @Override
    public List assembleProposalList(ViewOptionsProposalsEnum vope) {
        List<Proposal> proposalListVisible = new ArrayList<>();
        if (proposalList != null && !proposalList.isEmpty()) {
            for (Proposal p : proposalList) {
                switch (vope) {
                    case VIEW_ALL:
                        proposalListVisible.add(p);
                        break;
                    case VIEW_ACTIVE_HIDDEN:
                        if (p.isActive() && p.isHidden()) {
                            proposalListVisible.add(p);
                        }
                        break;
                    case VIEW_ACTIVE_NOTHIDDEN:
                        if (p.isActive() && !p.isHidden() && !p.getDirective().isRefuseToBeHidden()) {
                            proposalListVisible.add(p);
                        }
                        break;
                    case VIEW_EVALUATED:
                        if (p.getResponseTS() != null) {
                            proposalListVisible.add(p);
                        }
                        break;
                    case VIEW_INACTIVE:
                        if (!p.isActive()) {
                            proposalListVisible.add(p);
                        }
                        break;
                    case VIEW_NOT_EVALUATED:
                        if (p.getResponseTS() == null) {
                            proposalListVisible.add(p);
                        }
                        break;
                    default:
                        proposalListVisible.add(p);
                } // switch
            } // for
        } // if
        return proposalListVisible;
    }
    
      /**
     * @return the paymentList
     */
    @Override
    public List<Payment> getPaymentList() {
        return paymentList;
    }

    /**
     * @param paymentList the paymentList to set
     */
    @Override
    public void setPaymentList(List<Payment> paymentList) {
        this.paymentList = paymentList;
    }
    
    /**
     * Takes the general Payment type and converts it to 
     * @param paymentList the paymentList to set
     */
    @Override
    public void setPaymentListGeneral(List<Payment> paymentList) {
        List<Payment> skeletonHorde = new ArrayList<>();
        
        for (Payment p : paymentList) {
            
            skeletonHorde.add(new Payment(p));
            
        }
        
        this.paymentList = skeletonHorde;
    }

    /**
     * @return the applicationList
     */
    public List<OccPermitApplication> getApplicationList() {
        return applicationList;
    }
    
        /**
     * @param applicationList the applicationList to set
     */
    public void setApplicationList(List<OccPermitApplication> applicationList) {
        this.applicationList = applicationList;
    }


    /**
     * @return the proposalList
     */
    public List getProposalList() {
        return proposalList;
    }

    /**
     * @return the inspectionList
     */
    public List getInspectionList() {
        return inspectionList;
    }

    /**
     * @return the permitList
     */
    public List getPermitList() {
        return permitList;
    }

    /**
     * @return the blobIDList
     */
    public List getBlobIDList() {
        return blobIDList;
    }

    /**
     * @param personListApplicants the personListApplicants to set
     */
    public void setPersonListApplicants(List<PersonOccApplication> personListApplicants) {
        this.personListApplicants = personListApplicants;
    }

    /**
     * @param proposalList the proposalList to set
     */
    @Override
    public void setProposalList(List<Proposal> proposalList) {
        this.proposalList = proposalList;
    }

    /**
     * @param inspectionList the inspectionList to set
     */
    public void setInspectionList(List<OccInspection> inspectionList) {
        this.inspectionList = inspectionList;
    }

    /**
     * @param permitList the permitList to set
     */
    public void setPermitList(List<OccPermit> permitList) {
        this.permitList = permitList;
    }

    /**
     * @param blobIDList the blobIDList to set
     */
    public void setBlobIDList(List<Integer> blobIDList) {
        this.blobIDList = blobIDList;
    }

    /**
     * @return the configuredTS
     */
    public LocalDateTime getConfiguredTS() {
        return configuredTS;
    }

    /**
     * @param configuredTS the configuredTS to set
     */
    public void setConfiguredTS(LocalDateTime configuredTS) {
        this.configuredTS = configuredTS;
    }

    /**
     * @return the feeList
     */
    public List<FeeAssigned> getFeeList() {
        return feeList;
    }

    /**
     * @param feeList the feeList to set
     */
    public void setFeeList(List<FeeAssigned> feeList) {
        this.feeList = feeList;
    }

    /**
     * @return the credentialSignature
     */
    @Override
    public String getCredentialSignature() {
        return credentialSignature;
    }

    /**
     * @param credentialSignature the credentialSignature to set
     */
    public void setCredentialSignature(String credentialSignature) {
        this.credentialSignature = credentialSignature;
    }

    /**
     * @return the status
     */
    public OccPeriodStatusEnum getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(OccPeriodStatusEnum status) {
        this.status = status;
    }

    /**
     * @param personList the personList to set
     */
    public void setPersonList(List<Person> personList) {
        this.personList = personList;
    }
    
    public List<Person> getPersonList(){
        return personList;
    }
    
    public List<PersonOccApplication> getPersonListApplicants(){
        return personListApplicants;
    }

    @Override
    public boolean isOpen() {
        if(status != null){
            return status.isOpenPeriod();
        } else {
            return false;
        }
        
    }

   
   
    
}
