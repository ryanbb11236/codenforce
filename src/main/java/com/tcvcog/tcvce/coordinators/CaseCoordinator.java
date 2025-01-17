/*
 * Copyright (C) 2017 Turtle Creek Valley
Council of Governments, PA
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for mo re details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.tcvcog.tcvce.coordinators;

import com.tcvcog.tcvce.entities.reports.ReportConfigCECaseList;
import com.tcvcog.tcvce.entities.reports.ReportConfigCECase;
import com.tcvcog.tcvce.entities.reports.ReportCEARList;
import com.tcvcog.tcvce.application.BackingBeanUtils;
import com.tcvcog.tcvce.application.LegendItem;
import com.tcvcog.tcvce.application.interfaces.IFace_EventRuleGoverned;
import com.tcvcog.tcvce.domain.AuthorizationException;
import com.tcvcog.tcvce.domain.BObStatusException;
import com.tcvcog.tcvce.domain.BlobException;
import com.tcvcog.tcvce.domain.BlobTypeException;
import com.tcvcog.tcvce.domain.EventException;
import com.tcvcog.tcvce.domain.IntegrationException;
import com.tcvcog.tcvce.domain.SearchException;
import com.tcvcog.tcvce.domain.ViolationException;
import com.tcvcog.tcvce.entities.*;
import com.tcvcog.tcvce.entities.reports.ReportCECaseListCatEnum;
import com.tcvcog.tcvce.entities.reports.ReportCECaseListStreetCECaseContainer;
import com.tcvcog.tcvce.entities.search.QueryCEAR;
import com.tcvcog.tcvce.entities.search.QueryCEAREnum;
import com.tcvcog.tcvce.entities.search.QueryCECase;
import com.tcvcog.tcvce.entities.search.QueryCECaseEnum;
import com.tcvcog.tcvce.entities.search.QueryCodeViolation;
import com.tcvcog.tcvce.entities.search.QueryCodeViolationEnum;
import com.tcvcog.tcvce.entities.search.QueryEvent;
import com.tcvcog.tcvce.entities.search.QueryEventEnum;
import com.tcvcog.tcvce.entities.search.SearchParamsCECase;
import com.tcvcog.tcvce.entities.search.SearchParamsCECaseDateFieldsEnum;
import com.tcvcog.tcvce.entities.search.SearchParamsCodeViolation;
import com.tcvcog.tcvce.entities.search.SearchParamsEvent;
import com.tcvcog.tcvce.integration.BlobIntegrator;
import com.tcvcog.tcvce.integration.CEActionRequestIntegrator;
import com.tcvcog.tcvce.integration.CaseIntegrator;
import com.tcvcog.tcvce.integration.CodeIntegrator;
import com.tcvcog.tcvce.integration.EventIntegrator;
import com.tcvcog.tcvce.integration.PersonIntegrator;
import com.tcvcog.tcvce.integration.SystemIntegrator;
import com.tcvcog.tcvce.occupancy.integration.PaymentIntegrator;
import com.tcvcog.tcvce.util.Constants;
import com.tcvcog.tcvce.util.DateTimeUtil;
import com.tcvcog.tcvce.util.MessageBuilderParams;
import com.tcvcog.tcvce.util.viewoptions.ViewOptionsActiveHiddenListsEnum;
import com.tcvcog.tcvce.util.viewoptions.ViewOptionsActiveListsEnum;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.axes.cartesian.CartesianScales;
import org.primefaces.model.charts.axes.cartesian.linear.CartesianLinearAxes;
import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartOptions;
import org.primefaces.model.charts.hbar.HorizontalBarChartModel;
import org.primefaces.model.charts.optionconfig.title.Title;
import org.primefaces.model.charts.optionconfig.tooltip.Tooltip;
import org.primefaces.model.charts.pie.PieChartDataSet;
import org.primefaces.model.charts.pie.PieChartModel;

/**
 *
 * @author ellen bascomb of apt 31y
 */
public class CaseCoordinator extends BackingBeanUtils implements Serializable {

    final CasePhaseEnum initialCECasePphase = CasePhaseEnum.PrelimInvestigationPending;
    final int FALLBACK_DAYSTOCOMPLY = 30;
    public final static int DEFAULT_EXTENSIONDAYS = 14;

    /**
     * Creates a new instance of CaseCoordinator
     */
    public CaseCoordinator() {

    }

    @PostConstruct
    public void initBean() {

    }

    // *************************************************************************
    // *                     CODE ENF CASE GENERAL                             *
    // *************************************************************************
    /**
     * Called at the very end of the CECaseDataHeavy creation process by the
     * CaseIntegrator and simply checks for events that have a required
     * eventcategory attached and places a copy of the event in the Case's
     * member variable.
     *
     * This means that every time we refresh the case, the list is automatically
     * updated. DESIGN NOTE: A competing possible location for this method would
     * be on the CECaseDataHeavy object itself--in its getEventListActionRequest
     * method
     *
     * @param c
     * @param ua
     * @return the CECaseDataHeavy with the action request list ready to roll
     * @throws com.tcvcog.tcvce.domain.BObStatusException
     * @throws com.tcvcog.tcvce.domain.IntegrationException
     * @throws com.tcvcog.tcvce.domain.SearchException
     */
    public CECaseDataHeavy cecase_assembleCECaseDataHeavy(CECase c, UserAuthorized ua)
            throws BObStatusException, 
            IntegrationException, 
            SearchException
           {

        Credential cred = null;
        if (ua != null && c != null) {
            cred = ua.getKeyCard();

        } else {
            throw new BObStatusException("Cannot construct cecaseDH with null case input");
        }
        SearchCoordinator sc = getSearchCoordinator();
        WorkflowCoordinator wc = getWorkflowCoordinator();
        PaymentIntegrator pi = getPaymentIntegrator();
        PropertyCoordinator pc = getPropertyCoordinator();
        BlobIntegrator bi = getBlobIntegrator();
        BlobCoordinator bc = getBlobCoordinator();

        // Wrap our base class in the subclass wrapper--an odd design structure, indeed
        CECaseDataHeavy cse = new CECaseDataHeavy(cecase_getCECase(c.getCaseID()));

        try {

            cse.setProperty(pc.getProperty(c.getPropertyID()));
            cse.setPropertyUnit(pc.getPropertyUnit(c.getPropertyUnitID()));

            // PROPOSAL LIST
            cse.setProposalList(wc.getProposalList(cse, cred));

            // EVENT RULE LIST
            cse.setEventRuleList(wc.rules_getEventRuleImpList(cse, cred));

            // CEAR LIST
            QueryCEAR qcear = sc.initQuery(QueryCEAREnum.ATTACHED_TO_CECASE, cred);
            qcear.getPrimaryParams().setCecase_ctl(true);
            qcear.getPrimaryParams().setCecase_val(c);

            cse.setCeActionRequestList(sc.runQuery(qcear).getBOBResultList());
            
            // BLOB
            // TODO: Debug endless recursion
            cse.setBlobList(bc.getBlobLightList(bi.getBlobIDs(c)));

        } catch (SearchException | BlobException  ex) {
            System.out.println(ex);
        }

        // Skip payment stuff for now--i totally broke this stuff

//        try {
//            cse.setFeeList(pi.getFeeAssigned(c));
//
//            cse.setPaymentListGeneral(pi.getPaymentList(c));
//        } catch (IntegrationException ex) {
//            System.out.println(ex);
//        }
        return cse;
    }

    /**
     * Utility method for calling cecase_assembleCECaseDataHeavy() for each base
     * CECase in the given list
     *
     * @param cseList
     * @param ua
     * @return
     */
    public List<CECaseDataHeavy> cecase_assembleCECaseDataHeavyList(List<CECase> cseList, UserAuthorized ua) {
        List<CECaseDataHeavy> cseDHList = new ArrayList<>();
        if (cseList != null && !cseList.isEmpty()) {
            for (CECase cse : cseList) {
                try {
                    cseDHList.add(cecase_assembleCECaseDataHeavy(cse, ua));
                } catch (BObStatusException | IntegrationException | SearchException ex) {
                    System.out.println("CaseCoordinator.assembleCECaseDataHeavy" + ex.toString());
                }

            }
        }
        return cseDHList;

    }

    /**
     * Utility for downcasting a list of CECasePropertyUnitDataHeavy to the base
     * class
     *
     * @param cspudhList
     * @return
     */
    public List<CECase> cecase_downcastCECasePropertyUnitHeavyList(List<CECasePropertyUnitHeavy> cspudhList) {
        List<CECase> cslist = new ArrayList<>();
        if (cspudhList != null && !cspudhList.isEmpty()) {
            for (CECasePropertyUnitHeavy c : cspudhList) {
                cslist.add((CECase) c);
            }

        }
        return cslist;
    }

    /**
     * Utility for assembling a list of data heavy cases from a list of base
     * class instances
     *
     * @param cseList
     * @param ua
     * @return
     */
    public List<CECaseDataHeavy> cecase_getCECaseDataHeavyList(List<CECase> cseList, UserAuthorized ua) {
        List<CECaseDataHeavy> heavyList = new ArrayList<>();
        for (CECase cse : cseList) {
            try {
                heavyList.add(cecase_assembleCECaseDataHeavy(cse, ua));
            } catch (BObStatusException | IntegrationException | SearchException ex) {
                System.out.println(ex);
            }
        }
        return heavyList;

    }
    
    /**
     * Manages the closing of a case prior to full violation compliance.
     * Will nullify any existing violations on the CECase
     * and attach a close case event based on the inputted value
     * 
     * @param cse the case to force close
     * @param closeEventCat the EventCategory associated with the closure reason
     * @param ua the user doing the force closing
     * @throws com.tcvcog.tcvce.domain.BObStatusException
     * @throws com.tcvcog.tcvce.domain.IntegrationException
     * @throws com.tcvcog.tcvce.domain.EventException
     */
    public void cecase_forceclose(CECaseDataHeavy cse, EventCategory closeEventCat, UserAuthorized ua) throws BObStatusException, IntegrationException, EventException{
        CaseIntegrator ci = getCaseIntegrator();
        EventCoordinator ec = getEventCoordinator();
        if(cse == null){
            throw new BObStatusException("Cannot close a null case");
        }
//        if(cse.getClosingDate() != null){
//            throw new BObStatusException("Cannot force close an already closed case");
//        }
        if(closeEventCat == null){
            throw new BObStatusException("Cannot close case with null closing event");
        }
        
        if(closeEventCat.getEventType() != EventType.Closing){
            throw new BObStatusException("Cannot close case with an event that's not of type: Closing");
        }
        
        // Nullify violations on case
        if(cse.getViolationList() != null && !cse.getViolationList().isEmpty()){
            for(CodeViolation cv: cse.getViolationList()){
                if(cv.getActualComplianceDate() == null){
                    violation_NullifyCodeViolation(cv, ua);
                }
            }
        }
        
//        cse.setClosingDate(LocalDateTime.now());
        ci.updateCECaseMetadata(cse);
        events_processClosingEvent(cse, ec.initEvent(cse, closeEventCat), ua);
        
    }

    /**
     * Primary pathway for retrieving the data-light superclass CECase.
     * Implements business logic.
     * 
     * @param caseID
     * @return
     * @throws IntegrationException
     */
    public CECase cecase_getCECase(int caseID) 
            throws IntegrationException {
        CaseIntegrator ci = getCaseIntegrator();
        EventCoordinator ec = getEventCoordinator();
        
        CECase cse = null;
        try {
            cse = ci.getCECase(caseID);
            if (cse != null) {

                // Inject all cases with lists of their violations, notices, citations, 
                // and events, which will be used to determine case phase
                cse.setNoticeList(nov_getNoticeOfViolationList(ci.novGetList(cse)));
                Collections.sort(cse.getNoticeList());
                Collections.reverse(cse.getNoticeList());

                cse.setCitationList(citation_getCitationList(cse));

                cse.setViolationList(violation_getCodeViolations(ci.getCodeViolations(cse)));
                Collections.sort(cse.getViolationList());
                
                cse.setEventList(ec.getEventList(cse));
                Collections.sort(cse.getEventList());
                Collections.reverse(cse.getEventList());

                cse = cecase_configureCECaseStageAndPhase(cse);
            }
        } catch (BObStatusException ex) {
            System.out.println(ex);
        }

        return cse;

    }

    /**
     * Builds a property and propertyUnit heavy subclass of our CECase object
     *
     * @param cse
     * @return the data-rich subclass with Property and possible PropertyUnit
     * @throws IntegrationException
     * @throws com.tcvcog.tcvce.domain.SearchException
     */
    public CECasePropertyUnitHeavy cecase_assembleCECasePropertyUnitHeavy(CECase cse) throws IntegrationException, SearchException {
        PropertyCoordinator pc = getPropertyCoordinator();

        CECasePropertyUnitHeavy csepuh = null;

        if (cse != null) {
            csepuh = new CECasePropertyUnitHeavy(cse);
            csepuh.setProperty(pc.getProperty(cse.getPropertyID()));
            if (cse.getPropertyUnitID() != 0) {
                csepuh.setPropUnit(pc.getPropertyUnit(cse.getPropertyUnitID()));
            }

        }
        return csepuh;
    }

    /**
     * Utility method for converting a List of simple CECase objects into the a
     * case that contains information about its Property and any associated
     * Units
     *
     * @param cseList
     * @return
     */
    public List<CECasePropertyUnitHeavy> cecase_assembleCECasePropertyUnitHeavyList(List<CECase> cseList) {

        List<CECasePropertyUnitHeavy> cspudhList = new ArrayList<>();

        if (cseList != null && !cseList.isEmpty()) {
            if (getSessionBean().getSessUser() != null) {

                for (CECase cse : cseList) {
                    try {
                        cspudhList.add(cecase_assembleCECasePropertyUnitHeavy(cse));
                    } catch (IntegrationException | SearchException ex) {
                        System.out.println(ex);

                    }
                }
            } else {
                //This session must be public
                UserCoordinator uc = getUserCoordinator();
                for (CECase cse : cseList) {
                    try {
                        cspudhList.add(cecase_assembleCECasePropertyUnitHeavy(cse));
                    } catch (IntegrationException | SearchException ex) {
                        System.out.println(ex);
                    }
                }
            }

        }

        return cspudhList;
    }
    /**
     * Utility method for converting a List of simple CECase objects into the a
     * case that contains information about its Property and any associated
     * Units
     *
     * @param cseList
     * @return
     */
    public List<CECasePropertyUnitHeavy> cecase_refreshCECasePropertyUnitHeavyList(List<CECasePropertyUnitHeavy> cseList) {

        List<CECasePropertyUnitHeavy> cspudhList = new ArrayList<>();

        if (cseList != null && !cseList.isEmpty()) {
            if (getSessionBean().getSessUser() != null) {

                for (CECase cse : cseList) {
                    try {
                        cspudhList.add(cecase_assembleCECasePropertyUnitHeavy(cse));
                    } catch (IntegrationException | SearchException ex) {
                        System.out.println(ex);

                    }
                }
            } else {
                //This session must be public
                UserCoordinator uc = getUserCoordinator();
                for (CECase cse : cseList) {
                    try {
                        cspudhList.add(cecase_assembleCECasePropertyUnitHeavy(cse));
                    } catch (IntegrationException | SearchException ex) {
                        System.out.println(ex);
                    }
                }
            }

        }

        return cspudhList;
    }

    /**
     * Asks the Integrator for an icon based on case phase
     *
     * @param phase
     * @return
     * @throws IntegrationException
     */
    public Icon cecase_getIconByCasePhase(CasePhaseEnum phase) throws IntegrationException {
        CaseIntegrator ci = getCaseIntegrator();
        return ci.getIcon(phase);
    }

    /**
     * A CECaseDataHeavy's Stage is derived from its Phase based on the set of
     * business rules encoded in this method.
     *
     * @param cse which needs its StageConfigured
     * @return the same CECas passed in with the CaseStageEnum configured
     * @throws BObStatusException
     * @throws com.tcvcog.tcvce.domain.IntegrationException
     */
    private CECase cecase_configureCECaseStageAndPhase(CECase cse) throws BObStatusException, IntegrationException {

        if (cse == null || cse.getCaseID() == 0) {
            throw new BObStatusException("cannot configure null case or one with ID 0");

        }

        // First determine case stage, then dig around as needed in the case
        // data to determine the appropriate phase
        // case stage basically emerges from violation status assessment
        SystemIntegrator si = getSystemIntegrator();
        CECaseStatus statusBundle = new CECaseStatus();

        if (cse.getPersonInfoPersonID() != 0 || cse.isPropertyInfoCase()) {
            statusBundle.setPhase(CasePhaseEnum.Container);
        } else if (cse.getClosingDate() != null) {
            statusBundle.setPhase(CasePhaseEnum.Closed);
            // jump right to court-based phase assignment if we have at least 1 elegible citation
        } else if (!cecase_buildCitationListForPhaseAssignment(cse).isEmpty()){
            statusBundle.setPhase(cecase_determineAndSetPhase_stageCITATION(cse));
        } else {

            // find overriding factors to have a closed 
            if (cse.getViolationList().isEmpty()) {
                // Open case, no violations yet: only one mapping

                statusBundle.setPhase(CasePhaseEnum.PrelimInvestigationPending);

                // we have at least one violation attached  
            } else {

                // If we don't have a mailed notice, then we're in Notice Delivery phase
                if (!cecase_determineIfNoticeHasBeenMailed(cse)) {
                    statusBundle.setPhase(CasePhaseEnum.IssueNotice);

                    // notice has been sent so we're in CaseStageEnum.Enforcement or beyond
                } else {
                    int maxVStage = violation_determineMaxViolationStatus(cse.getViolationList());
                    switch (maxVStage) {
                        case 0:  // all violations resolved
                            statusBundle.setPhase(CasePhaseEnum.Closed);
                            break;
                        case 1: // all violations within compliance window
                            statusBundle.setPhase(CasePhaseEnum.InsideComplianceWindow);
                            break;
                        case 2: // one or more EXPIRED compliance timeframes but no citations
                            statusBundle.setPhase(CasePhaseEnum.TimeframeExpiredNotCited);
                            break;
                            // we shouldn't hit this...
                        case 3: // at least 1 violation used in a citation that's attached to case
                            statusBundle.setPhase(cecase_determineAndSetPhase_stageCITATION(cse));
                            //                            cecase_determineAndSetPhase_stageCITATION(cse);
                            break;
                        default: // unintentional dumping ground 
                            statusBundle.setPhase(CasePhaseEnum.InactiveHolding);
                    } // close violation and citation block 
                } // close sent-notice-dependent block
            } // close violation-present-only block
        } // close non-container, non-closed block
        
        cse.setStatusBundle(statusBundle);

        if (cse.getStatusBundle().getPhase() != null) {
            //now set the icon based on what phase we just assigned the case to
            statusBundle.setPhaseIcon(si.getIcon(Integer.parseInt(getResourceBundle(Constants.DB_FIXED_VALUE_BUNDLE)
                    .getString(cse.getStatusBundle().getPhase().getCaseStage().getIconPropertyLookup()))));
        }

        return cse;
    }
    
    /**
     * Utility method for culling out inactive citations, deactivated, etc. 
     * @param cse
     * @return 
     */
    private List<Citation> cecase_buildCitationListForPhaseAssignment(CECase cse){
        List<Citation> citList = new ArrayList<>();
        if(cse == null || cse.getCitationList() == null || cse.getCitationList().isEmpty()){
            return citList;
        } else {
            for(Citation cit: cse.getCitationList()){
                if(cit.getStatus().isNonStatusEditsForbidden()){
                    citList.add(cit);
                }
            }
        }
        
        
        return citList;
        
    }

    /**
     * Logic container for case phase assignment for cases with at least 1 citation
     * 
     * Assesses the list of events and citations on the case to determine the 
     * appropriate post-hearing related case phase
     * 
     * 
     *
     * @param cse
     * @return
     */
    private CasePhaseEnum cecase_determineAndSetPhase_stageCITATION(CECase cse) {
        CasePhaseEnum courtPhase = null;
        
        int catIDCitationIssued = Integer.parseInt(getResourceBundle(Constants.EVENT_CATEGORY_BUNDLE).getString("court_citationissued"));
        int catIDHearingSched = Integer.parseInt(getResourceBundle(Constants.EVENT_CATEGORY_BUNDLE).getString("court_scheduledhearing"));
        int catIDHearingAttended = Integer.parseInt(getResourceBundle(Constants.EVENT_CATEGORY_BUNDLE).getString("court_hearingattended"));

        List<EventCnF> courtEvList = cecase_getEventListSubset(cse.getEventList(), EventType.Court);
        if(courtEvList == null || courtEvList.isEmpty()){
            courtPhase = CasePhaseEnum.TimeframeExpiredNotCited;
            cse.logStatusNote("COURT: GAP IN EVENT RECORDING! No court events found, but We should have citation issued events; falling back to " + courtPhase.getLabel() );
            return courtPhase;
        }
       
        
        // if we have a citation but no hearing scheduled, we're AwaitingAHearingDate
        LocalDateTime rightNow = LocalDateTime.now();
        LocalDateTime rightMostScheduledHearingDate = determineRightmostDateByEventCategoryID(courtEvList, catIDHearingSched);
        LocalDateTime rightMostHearingDate = determineRightmostDateByEventCategoryID(courtEvList, catIDHearingAttended);
        if(rightMostScheduledHearingDate == null){
            courtPhase = CasePhaseEnum.AwaitingHearingDate;
            cse.logStatusNote("COURT: No hearing scheduled; assigned " + courtPhase.getLabel() );
        } else {
            // if a hearing is scheduled in the future, we're HEARING PREP
            if(rightMostScheduledHearingDate.isAfter(rightNow)){
                courtPhase = CasePhaseEnum.HearingPreparation;
                cse.logStatusNote("COURT: Found hearing schduled in the future; assigned " + courtPhase.getLabel() );
            // if we've attended court and violations are still in the window, we're at InsideCourt....
            } else if(rightMostHearingDate.isBefore(rightNow)){
                
                int maxVStage = violation_determineMaxViolationStatus(cse.getViolationList());
                switch (maxVStage) {
                    case 0:  // all violations resolved
                        courtPhase = CasePhaseEnum.Closed;
                        break;
                    case 1: // all violations within compliance window
                        courtPhase = CasePhaseEnum.InsideCourtOrderedComplianceTimeframe;
                        break;
                    case 2: // one or more EXPIRED compliance timeframes but no citations
                        courtPhase = CasePhaseEnum.CourtOrderedComplainceTimeframeExpired;
                        break;
                        // we shouldn't hit this...
                    default: // unintentional dumping ground 
                        courtPhase = CasePhaseEnum.CourtOrderedComplainceTimeframeExpired;
                } // close violation and citation block 
            }
        }
        return courtPhase;
    }
    
    
    /**
     * Utility method for extracting an event subset
     * @param evList
     * @param et
     * @return 
     */
    private List<EventCnF> cecase_getEventListSubset(List<EventCnF> evList, EventType et){
         // get the events we want
        List<EventCnF> selectedEvents;
        selectedEvents = new ArrayList<>();
        if(evList != null && !evList.isEmpty()){
            for(EventCnF ev: evList){
                if(ev.getCategory().getEventType() == EventType.Court){
                    selectedEvents.add(ev);
                }
            }
        }
        
        return selectedEvents;
    }
    
    /**
     * Utility method for searching through a list of events and finding the "highest" 
     * (i.e. right-most) event of a certain category
     * 
     * @param evList complete event list to search
     * @param catID the category ID by which to create a comparison subset
     * @return if no event of this type is found, null is returned
     */
    private LocalDateTime determineRightmostDateByEventCategoryID(List<EventCnF> evList, int catID){
        LocalDateTime highestDate = null;
        if(evList != null && !evList.isEmpty()){
            for(EventCnF ev: evList){
                if(ev.getCategory().getCategoryID() == catID && ev.getTimeStart() != null){
                    if(highestDate == null){
                        highestDate = ev.getTimeStart();
                    } else if (highestDate.isBefore(ev.getTimeStart())){
                        highestDate = ev.getTimeStart();
                    }
                }
            }
        }
        
        return highestDate;
    }

    /**
     * TODO: Finish my guts
     *
     * @param cse
     * @return
     */
    public boolean cecase_determineIfNoticeHasBeenMailed(CECase cse) {
        return true;
    }

    /**
     * TODO: Old logic but good method sig. This method should get the
     * PropertyInfoCase associated with the Muni's property
     *
     * @param ua
     * @return
     * @throws IntegrationException
     * @throws BObStatusException
     */
    public CECase cecase_selectDefaultCECase(UserAuthorized ua) throws IntegrationException, BObStatusException {
        PropertyCoordinator pc = getPropertyCoordinator();
        MunicipalityCoordinator mc = getMuniCoordinator();
        CECase cse = null;
        try {
            MunicipalityDataHeavy mdh = mc.assembleMuniDataHeavy(ua.getKeyCard().getGoverningAuthPeriod().getMuni(), ua);
            PropertyDataHeavy pdh = mdh.getMuniPropertyDH();
            if (pdh.getPropInfoCaseList() != null && !pdh.getPropInfoCaseList().isEmpty()) {
                cse = pdh.getPropInfoCaseList().get(0);
            }

        } catch (AuthorizationException | BObStatusException | EventException | IntegrationException ex) {
            System.out.println("CaseCoordinator: Exception selecting default CECase from MuniPropDH; using arbitrary case");
        }
        if (cse != null) {
            return cse;
        }
        return cecase_getCECase(Integer.parseInt(getResourceBundle(Constants.DB_FIXED_VALUE_BUNDLE).getString("arbitraryPlaceholderCaseID")));
    }

    /**
     * Asks the DB for a case history list and converts the list of IDs into
     * actual CECase objects
     *
     * @param ua
     * @return
     * @throws IntegrationException
     * @throws BObStatusException
     */
    public List<CECase> cecase_getCECaseHistory(UserAuthorized ua) throws IntegrationException, BObStatusException {
        CaseIntegrator caseInt = getCaseIntegrator();
        List<CECase> cl = new ArrayList<>();
        if (ua != null) {
            List<Integer> cseidl = caseInt.getCECaseHistoryList(ua.getMyCredential().getGoverningAuthPeriod().getUserID());
            if (!cseidl.isEmpty()) {
                for (Integer i : cseidl) {
                    cl.add(cecase_getCECase(i));
                }
            }
        }
        return cl;
    }

    /**
     * Factory method for creating new CECases
     *
     * @param p
     * @param ua
     * @return
     * @throws com.tcvcog.tcvce.domain.IntegrationException If an error occurs while generating a control code
     */
    public CECase cecase_initCECase(Property p, UserAuthorized ua) throws IntegrationException {
        UserCoordinator uc = getUserCoordinator();
        CECase newCase = new CECase();

        // removed inputted muni here
        int casePCC = generateControlCodeFromTime(0);
        // caseID set by postgres sequence
        // timestamp set by postgres
        // no closing date, by design of case flow
        newCase.setPublicControlCode(casePCC);
        newCase.setPropertyID(p.getPropertyID());
        newCase.setCaseManager(ua);

        return newCase;
    }

    /**
     * Logic intermediary for determining of a case can be deactivated
     *
     * @param cse
     * @throws IntegrationException
     * @throws BObStatusException
     */
    public void cecase_deactivateCase(CECaseDataHeavy cse) throws IntegrationException, BObStatusException {
        CaseIntegrator ci = getCaseIntegrator();
        if (cse != null) {
            if (!cse.isPropertyInfoCase()) {
                if (cse.getViolationList() == null || cse.getViolationList().isEmpty()) {
                    cse.setActive(false);
                    ci.updateCECaseMetadata(cse);
                } else {
                    throw new BObStatusException("Cannot deactivate a case with 1 or more violations attached");
                }
            }
        }

    }

    /**
     * Primary entry point for inserting new code enf cases. Two major pathways
     * exist through this method: - creating cases as a result of an action
     * request submission - creating cases from some other source than an action
     * request Depending on the source, an appropriately note-ified case
     * origination event is built and attached to the case that was just
     * created.
     *
     * @param freshCase
     * @param ua
     * @param cear
     * @return 
     * @throws IntegrationException
     * @throws BObStatusException
     * @throws ViolationException
     * @throws com.tcvcog.tcvce.domain.EventException
     * @throws com.tcvcog.tcvce.domain.SearchException
     */
    public int cecase_insertNewCECase(CECase freshCase, UserAuthorized ua, CEActionRequest cear) 
            throws IntegrationException, 
            BObStatusException, 
            ViolationException, 
            EventException, 
            SearchException {

        CaseIntegrator ci = getCaseIntegrator();
        CEActionRequestIntegrator ceari = getcEActionRequestIntegrator();
        EventCoordinator ec = getEventCoordinator();
        EventCategory originationCategory;
        EventCnF originationEvent;
        UserCoordinator uc = getUserCoordinator();

        freshCase.setActive(true);
        freshCase.setLastUpdatedBy(ua);

        if (freshCase.getCaseManager() == null) {
            freshCase.setCaseManager(ua);
        }

        cecase_auditCaseForInsert(freshCase);

        // the integrator returns to us a CECaseDataHeavy with the correct ID after it has
        // been written into the DB
        int freshID = ci.insertNewCECase(freshCase);
        CECaseDataHeavy cedh = cecase_assembleCECaseDataHeavy(freshCase, ua);

        // If we were passed in an action request, connect it to the new case we just made
        if (cear != null) {
            ceari.connectActionRequestToCECase(cear.getRequestID(), freshID, ua.getUserID());
            originationCategory = ec.initEventCategory(
                    Integer.parseInt(getResourceBundle(
                            Constants.EVENT_CATEGORY_BUNDLE).getString("originiationByActionRequest")));
            originationEvent = ec.initEvent(cedh, originationCategory);
            StringBuilder sb = new StringBuilder();
            originationEvent.setNotes(sb.toString());
        } else if(freshCase.isPropertyInfoCase()){
            // This is a property info case, it originated to store info
            originationCategory = ec.initEventCategory(
                    Integer.parseInt(getResourceBundle(
                            Constants.EVENT_CATEGORY_BUNDLE).getString("originiationByObservation")));
            originationEvent = ec.initEvent(cedh, originationCategory);
            StringBuilder sb = new StringBuilder();
            sb.append("This case was created to contain information pertaining to a property");
        } else {
            // since there's no action request, the assumed method is called "observation"
            originationCategory = ec.initEventCategory(
                    Integer.parseInt(getResourceBundle(
                            Constants.EVENT_CATEGORY_BUNDLE).getString("originiationByObservation")));
            originationEvent = ec.initEvent(cedh, originationCategory);
            StringBuilder sb = new StringBuilder();
            sb.append("Case opened directly on property by code officer assigned to this event");
//            originationEvent.setNotes(generateCaseInitNoteFromCEAR(cear));

        }
        originationEvent.setUserCreator(uc.user_getUser(ua.getUserID()));

        cedh.setCaseID(freshID);

        ec.addEvent(originationEvent, cedh, ua);
        
        return freshID;
    }
    
    /**
     * Checks for violation status and closes case
     * @param cse
     * @param ua 
     * @throws com.tcvcog.tcvce.domain.BObStatusException 
     * @throws com.tcvcog.tcvce.domain.IntegrationException 
     */
    public void cecase_closeCase(CECase cse, UserAuthorized ua) throws BObStatusException, IntegrationException{
        CaseIntegrator ci = getCaseIntegrator();
        if(cse == null || ua == null){
            throw new BObStatusException("Cannot close a case with null Case or User");
        }
        cse.setClosingDate(LocalDateTime.now());
        cse.setLastUpdatedBy(ua);
        
        ci.updateCECaseMetadata(cse);
        
    }

    private String cecase_generateCaseInitNoteFromCEAR(CEActionRequest cear) {
        StringBuilder sb = new StringBuilder();

        sb.append("Case generated from the submission of a Code Enforcement Action Request");
        sb.append("<br />");
        sb.append("ID#:");
        sb.append(cear.getRequestID());
        sb.append(" submitted by ");
        sb.append(cear.getRequestor().getFirstName());
        sb.append(" ");
        sb.append(cear.getRequestor().getLastName());
        sb.append(" on ");
        sb.append(DateTimeUtil.getPrettyDate(cear.getDateOfRecord()));
        sb.append(" with a database timestamp of ");
        sb.append(DateTimeUtil.getPrettyDate(cear.getSubmittedTimeStamp()));
        return sb.toString();
    }

    private void cecase_auditCaseForInsert(CECase cse) throws BObStatusException {
        if (cse == null) {
            throw new BObStatusException("Cannot insert a null case");
        }

        if (cse.getOriginationDate() == null) {
            cse.setOriginationDate(LocalDateTime.now());
        }

        if (cse.getPropertyID() == 0) {
            throw new BObStatusException("Cases must have a nonzero property id");
        }

        if (cse.getClosingDate() != null && cse.getOriginationDate() != null) {

            if (cse.getClosingDate().isBefore(cse.getOriginationDate())) {
                throw new BObStatusException("Cases cannot close before they open");
            }
        }
    }

    /**
     * Implements business logic before updating a CECaseDataHeavy's core data
     * (opening date, closing date, etc.). If all is well, pass to integrator.
     *
     * @param c the CECaseDataHeavy to be updated
     * @param ua the user doing the updating
     * @throws BObStatusException
     * @throws IntegrationException
     */
    public void cecase_updateCECaseMetadata(CECaseDataHeavy c, UserAuthorized ua) throws BObStatusException, IntegrationException {
        CaseIntegrator ci = getCaseIntegrator();
        if(ua == null || c == null) throw new BObStatusException("Cannot update case with null case or user");
        // the supplied user is the updator
        c.setLastUpdatedBy(ua);
        // Logic check for changes to the case closing date
        if (c.getClosingDate() != null) {
            if (c.getClosingDate().isBefore(c.getOriginationDate())) {
                throw new BObStatusException("You cannot update a case's origination date to be after its closing date");
            }
        }
        ci.updateCECaseMetadata(c);
    }

    /**
     * Updates only the notes field on CECase
     *
     * @param mbp
     * @param c the CECaseDataHeavy to be updated
     * @throws BObStatusException
     * @throws IntegrationException
     */
    public void cecase_updateCECaseNotes(MessageBuilderParams mbp, CECase c) throws BObStatusException, IntegrationException {
        CaseIntegrator ci = getCaseIntegrator();
        SystemCoordinator sc = getSystemCoordinator();
        if (c == null || mbp == null) {
            throw new BObStatusException("Cannot append if notes, case, or user are null");
        }

        c.setNotes(sc.appendNoteBlock(mbp));
        c.setLastUpdatedBy(mbp.getUser());
        ci.updateCECaseNotes(c);
    }

    // *************************************************************************
    // *                     REPORTING                                         *
    // *************************************************************************
    /**
     * Factory method for creating new reports listing action requests
     *
     * @param u
     * @param m
     * @return
     */
    public ReportCEARList report_getInitializedReportConficCEARs(User u, Municipality m) {
        ReportCEARList rpt = new ReportCEARList();
        rpt.setIncludePhotos(true);
        rpt.setPrintFullCEARQueue(false);
        rpt.setCreator(u);
        rpt.setMuni(m);
        rpt.setGenerationTimestamp(LocalDateTime.now());
        return rpt;
    }

    /**
     * Factory method for CECase reports with sensible initial settings
     *
     * @param c
     * @return
     */
    public ReportConfigCECase report_getDefaultReportConfigCECase(CECaseDataHeavy c) {
        ReportConfigCECase rpt = new ReportConfigCECase();

        // general
        rpt.setIncludeCaseName(false);

        // events
        rpt.setIncludeEventNotes(true);
        rpt.setIncludeHiddenEvents(false);
        rpt.setIncludeInactiveEvents(false);
        rpt.setIncludeRequestedActionFields(false);
        rpt.setIncludeMunicipalityDiclosedEvents(true);
        rpt.setIncludeOfficerOnlyEvents(false);

        // notices of violation
        rpt.setIncludeAllNotices(false);
        rpt.setIncludeNoticeFullText(true);
        // violations
        rpt.setIncludeFullOrdinanceText(true);
        rpt.setIncludeViolationPhotos(true);

        return rpt;
    }

    /**
     * Factory method for reports on a list of cases
     *
     * @return
     */
    public ReportConfigCECaseList report_getDefaultReportConfigCECaseList() {
        ReportConfigCECaseList listRpt = new ReportConfigCECaseList();
        listRpt.setIncludeListSummaryFigures(true);
        listRpt.setIncludeCaseNames(true);
        listRpt.setIncludeFullOwnerContactInfo(true);
        listRpt.setIncludeViolationList(true);
        listRpt.setIncludeEventSummaryByCase(false);
        return listRpt;

    }
    
    /**
     * Prepares a report of municipal case activity for a given time period
     * @param rpt the report config object with dates set for searching
     * @param ua the user requesting the report
     * @return the configured report
     * @throws SearchException 
     */
    public ReportConfigCECaseList report_buildCECaseListReport(ReportConfigCECaseList rpt, UserAuthorized ua) throws SearchException{
        SearchCoordinator sc = getSearchCoordinator();
        if(rpt == null || ua == null){
            return null;
        }
        
        QueryCECase query_opened = sc.initQuery(QueryCECaseEnum.OPENED_INDATERANGE, ua.getKeyCard());
        SearchParamsCECase spcse = query_opened.getPrimaryParams();
        spcse.setDate_startEnd_ctl(true);
        spcse.setDate_field(SearchParamsCECaseDateFieldsEnum.ORIGINATION_DOFRECORD);
        spcse.setDate_start_val(rpt.getDate_start_val());
        spcse.setDate_end_val(rpt.getDate_end_val());
        
        rpt.setCaseListOpenedInDateRange(cecase_assembleCECaseDataHeavyList(sc.runQuery(query_opened).getBOBResultList(), ua));
        if(rpt.getCaseListOpenedInDateRange() != null){
            System.out.println("CaseCoordinator.report_buildCECaseListReport: Opened List size " + rpt.getCaseListOpenedInDateRange().size());
        }
        
        QueryCECase query_active_asof = sc.initQuery(QueryCECaseEnum.OPEN_ASOFENDDATE, ua.getKeyCard());
        
        if(query_active_asof.getParamsList() != null && query_active_asof.getParamsList().size() == 2){

            // setup params
            spcse = query_active_asof.getPrimaryParams();

            // client field injection: cases open as of EOR but closed between EOR+1 and now()
            if(spcse.getDateRuleList() != null && spcse.getDateRuleList().size() == 2){
                spcse.getDateRuleList().get(0).setDate_end_val(rpt.getDate_end_val());
                spcse.getDateRuleList().get(1).setDate_start_val(rpt.getDate_end_val().plusDays(1));
                System.out.println("CaseCoordinator.report_buildCECaseListReport " + spcse.getDateRuleList()  );
            }

            // client field injection: cases open as of EOR and not closed, even today
            if(query_active_asof.getParamsList() != null && !query_active_asof.getParamsList().isEmpty()){
                SearchParamsCECase sponc = query_active_asof.getParamsList().get(1);
                sponc.getDateRuleList().get(0).setDate_end_val(rpt.getDate_start_val());
                System.out.println("CaseCoordinator.report_buildCECaseListReport " + sponc.getDateRuleList()  );
            }
        }
        
        
        List<CECase> cseList = sc.runQuery(query_active_asof).getBOBResultList();
         System.out.println("Query: " + query_active_asof.getQueryLog());
        
        if(cseList != null && !cseList.isEmpty()){
            System.out.println("CaseCoordinator.assembleCECaseListReport: OPEN AS OF CASE LIST");
            for(CECase cse: cseList){
                System.out.print("CID: " + cse.getCaseID());
                System.out.println("| name: " + cse.getCaseName());
            }
        }
        
        rpt.setCaseListOpenAsOfDateEnd(cecase_assembleCECaseDataHeavyList(cseList, ua));
        
        if(rpt.getCaseListOpenAsOfDateEnd() != null && !rpt.getCaseListOpenAsOfDateEnd().isEmpty()){
            System.out.println("CaseCoordinator.report_buildCECaseListReport: Opened List size " + rpt.getCaseListOpenAsOfDateEnd().size());
            // compute average case age for opened as of date list
            long sumOfAges = 0l;
            for(CECase cse: rpt.getCaseListOpenAsOfDateEnd()){
                sumOfAges += cse.getCaseAgeAsOf(rpt.getDate_end_val());
            }
            double avg = (double) sumOfAges / (double) rpt.getCaseListOpenAsOfDateEnd().size();
            rpt.setAverageAgeOfCasesOpenAsOfReportEndDate(avg);
        }
        
        QueryCECase query_closed = sc.initQuery(QueryCECaseEnum.CLOSED_CASES, ua.getKeyCard());
        spcse = query_closed.getPrimaryParams();
        spcse.setDate_field(SearchParamsCECaseDateFieldsEnum.CLOSE);
        spcse.setDate_startEnd_ctl(true);
        spcse.setDate_start_val(rpt.getDate_start_val());
        spcse.setDate_end_val(rpt.getDate_end_val());
        rpt.setCaseListClosedInDateRange(cecase_assembleCECaseDataHeavyList(sc.runQuery(query_closed).getBOBResultList(), ua));
        if(rpt.getCaseListClosedInDateRange() != null){
            System.out.println("CaseCoordinator.report_buildCECaseListReport: Current List size " + rpt.getCaseListClosedInDateRange().size());
        }
        
        // BUILD BY STREET
        rpt = report_ceCaseList_organizeByStreet(rpt);
        
        // EVENTS
        
        QueryEvent query_ev = sc.initQuery(QueryEventEnum.MUNI_MONTHYACTIVITY, ua.getKeyCard());
        SearchParamsEvent spev = query_ev.getPrimaryParams();
        spev.setDate_startEnd_ctl(true);
        spev.setDate_start_val(rpt.getDate_start_val());
        spev.setDate_end_val(rpt.getDate_end_val());
        rpt.setEventList(sc.runQuery(query_ev).getBOBResultList());
        
        // VIOLATIONS
//        QueryCodeViolation qcv = sc.initQuery(QueryCodeViolationEnum.LOGGED_PAST30_NOV_CITMAYBE, ua.getKeyCard());
//        SearchParamsCodeViolation params = qcv.getPrimaryParams();
//        params.setDate_relativeDates_ctl(false);
//        params.setDate_start_val(rpt.getDate_start_val());
//        params.setDate_end_val(rpt.getDate_end_val());
//        rpt.setViolationsLoggedNOVDateRange(sc.runQuery(qcv).getResults());
//        
//        qcv = sc.initQuery(QueryCodeViolationEnum.COMP_PAST30, ua.getKeyCard());
//        params = qcv.getPrimaryParams();
//        params.setDate_relativeDates_ctl(false);
//        params.setDate_start_val(rpt.getDate_start_val());
//        params.setDate_end_val(rpt.getDate_end_val());
//        rpt.setViolationsLoggedComplianceDateRange(sc.runQuery(qcv).getResults());
//        
//        qcv = sc.initQuery(QueryCodeViolationEnum.CITED_PAST30, ua.getKeyCard());
//        params = qcv.getPrimaryParams();
//        params.setDate_relativeDates_ctl(false);
//        params.setDate_start_val(rpt.getDate_start_val());
//        params.setDate_end_val(rpt.getDate_end_val());
//        rpt.setViolationsCitedDateRange(sc.runQuery(qcv).getResults());
//        
//        initBarViolationsPastMonth(rpt);
        initPieEnforcement(rpt);
        initPieViols(rpt);
        initPieCitation(rpt);
        initPieClosure(rpt);

        
        return rpt;
        
    }
    
    /**
     * Internal logic for taking apart case lists and turning them into street-categorized
     * maps
     * @param rptCseList
     * @return the inputted report config object with its street container list assembled
     */
    private ReportConfigCECaseList report_ceCaseList_organizeByStreet(ReportConfigCECaseList rptCseList){
        
        if(rptCseList != null){
            Map<String, ReportCECaseListStreetCECaseContainer> streetCaseMap = new HashMap<>();
            List<ReportCECaseListCatEnum> reportListsList = Arrays.asList(ReportCECaseListCatEnum.values());
            for(ReportCECaseListCatEnum enm: reportListsList){
                List<CECaseDataHeavy> cseList = null;
                switch(enm){
                    case CLOSED:
                        cseList = rptCseList.getCaseListClosedInDateRange();
                        break;
                    case CONTINUING:
                        cseList = rptCseList.getCaseListOpenAsOfDateEnd();
                        break;
                    case OPENED:
                        cseList = rptCseList.getCaseListOpenedInDateRange();
                        break;
                }

                if(cseList != null && !cseList.isEmpty()){
                    ReportCECaseListStreetCECaseContainer ssc;

                    for(CECaseDataHeavy cse: cseList){
                        if(!streetCaseMap.containsKey(cse.getProperty().getAddressStreet())){
                             ssc = new ReportCECaseListStreetCECaseContainer();
                             ssc.setStreetName(cse.getProperty().getAddressStreet());
                            switch(enm){
                                case CLOSED:
                                    ssc.getCaseClosedList().add(cse);
                                    break;
                                case CONTINUING:
                                    ssc.getCaseContinuingList().add(cse);
                                    break;
                                case OPENED:
                                    ssc.getCaseOpenedList().add(cse);
                                    break;
                            } // close switch
                        } else {
                            ssc = streetCaseMap.get(cse.getProperty().getAddressStreet());
                            switch(enm){
                                case CLOSED:
                                    ssc.getCaseClosedList().add(cse);
                                    break;
                                case CONTINUING:
                                    ssc.getCaseContinuingList().add(cse);
                                    break;
                                case OPENED:
                                    ssc.getCaseOpenedList().add(cse);
                                    break;
                            } // close switch
                        } // close if/else
                        // Write new or updated Street Case continaer to our map
                        streetCaseMap.put(cse.getProperty().getAddressStreet(), ssc);
                    } // close for over case list
                } // close null/emtpy check
            } // close for over enum vals
            
            report_ceCaseList_removeDupsByStreet(streetCaseMap);
            rptCseList.setStreetSCC(streetCaseMap);
            rptCseList.assembleStreetList();
            
        
        } // close param null check
        
        return rptCseList;
        
    }
    
    
    
    /**
     * Removes cases which were opened and closed in the same period
     * and leaves them in the Closed group
     * @param rpt 
     */
    private void report_ceCaseList_removeDupsByStreet(Map<String, ReportCECaseListStreetCECaseContainer> streetCaseMap){
        for(String rclsc: streetCaseMap.keySet()){
            ReportCECaseListStreetCECaseContainer cont = streetCaseMap.get(rclsc);
            for(CECaseDataHeavy cse: cont.getCaseOpenedList()){
                if(cont.getCaseClosedList().contains(cse)){
                    System.out.println("CaseCoordinator.removeDupes: removing case ID " + cse.getCaseID());
                    cont.getCaseClosedList().remove(cse);
                }
            }
        }
    }
    
    
    /**
     * Builds a violation pie chart
     * @param rpt 
     */
      private void initPieViols(ReportConfigCECaseList rpt){
          CaseCoordinator cc = getCaseCoordinator();
        rpt.setPieViol(new PieChartModel());
        ChartData pieData = new ChartData();
        
        PieChartDataSet dataSet = new PieChartDataSet();
        List<Number> violationCounts = new ArrayList<>();
        
        List<CodeViolation> vl = new ArrayList<>();
        for(CECaseDataHeavy cdh: rpt.assembleFullCaseLiset()){
            vl.addAll(cdh.assembleViolationList(ViewOptionsActiveListsEnum.VIEW_ACTIVE));
        }
        
        // build our count map
        Map<ViolationStatusEnum, Integer> violMap = new HashMap<>();
        violMap.put(ViolationStatusEnum.UNRESOLVED_WITHINCOMPTIMEFRAME, 0);
        violMap.put(ViolationStatusEnum.UNRESOLVED_EXPIREDCOMPLIANCETIMEFRAME, 0);
        violMap.put(ViolationStatusEnum.RESOLVED, 0);
        violMap.put(ViolationStatusEnum.UNRESOLVED_CITED, 0);
        violMap.put(ViolationStatusEnum.NULLIFIED, 0);
        
        for(CodeViolation cv: vl){
            ViolationStatusEnum vs = cv.getStatus();
            
            Integer catCount = violMap.get(vs);
            catCount += 1;
            violMap.put(cv.getStatus(), catCount);
        }
        
        rpt.setPieViolStatMap(violMap);
        rpt.setPieViolCompCount(violMap.get(ViolationStatusEnum.RESOLVED));
        Set<ViolationStatusEnum> keySet = violMap.keySet();
        List<String> pieColors = new ArrayList<>();
        List<LegendItem> legend = new ArrayList<>();

        SystemCoordinator sc = getSystemCoordinator();
        
        for(ViolationStatusEnum vse: keySet){
            Integer i = violMap.get(vse);
            violationCounts.add(i);
            String color = sc.generateRandomRGBColor();
            pieColors.add(color);
            LegendItem li = new LegendItem();
            li.setColorRGBString(color);
            li.setValue(i);
            li.setTitle(vse.getLabel());
            legend.add(li);
            
        }
        
        rpt.setPieViolLegend(legend);
        dataSet.setData(violationCounts);
        dataSet.setBackgroundColor(pieColors);
        pieData.addChartDataSet(dataSet);
        rpt.getPieViol().setData(pieData);
    }
      
      
    /**
     * Builds a pie chart about case enforcement status
     * @param rpt 
     */
      private void initPieEnforcement(ReportConfigCECaseList rpt){
          
        if(rpt != null){
            
            rpt.setPieEnforcement(new PieChartModel());
            ChartData pieData = new ChartData();

            PieChartDataSet dataSet = new PieChartDataSet();
            List<Number> enfStatusNumList = new ArrayList<>();

            Map<String, Integer> statMap = new HashMap<>();
            
            statMap.put(CaseStageEnum.Investigation.getLabel(), 0);
            statMap.put(CasePhaseEnum.InsideComplianceWindow.getLabel(), 0);
            statMap.put(CasePhaseEnum.TimeframeExpiredNotCited.getLabel(), 0);
            statMap.put(CaseStageEnum.Citation.getLabel(), 0);
            
            for(CECaseDataHeavy cse: rpt.assembleNonClosedCaseList()){
                if(cse.getStatusBundle().getPhase().getStage() == CaseStageEnum.Investigation){
                    Integer statCount = statMap.get(CaseStageEnum.Investigation.getLabel());
                    statCount += 1;
                    statMap.put(CaseStageEnum.Investigation.getLabel(), statCount);
                    
                }
                if(cse.getStatusBundle().getPhase().getCaseStage() == CaseStageEnum.Enforcement){
                    if(cse.getStatusBundle().getPhase() == CasePhaseEnum.InsideComplianceWindow){
                        Integer statCount = statMap.get(CasePhaseEnum.InsideComplianceWindow.getLabel());
                        statCount += 1;
                        statMap.put(CasePhaseEnum.InsideComplianceWindow.getLabel(), statCount);
                    }
                    if(cse.getStatusBundle().getPhase() == CasePhaseEnum.TimeframeExpiredNotCited){
                        Integer statCount = statMap.get(CasePhaseEnum.TimeframeExpiredNotCited.getLabel());
                        statCount += 1;
                        statMap.put(CasePhaseEnum.TimeframeExpiredNotCited.getLabel(), statCount);
                    }
                }
                if(cse.getStatusBundle().getPhase().getStage() == CaseStageEnum.Citation){
                    Integer statCount = statMap.get(CaseStageEnum.Citation.getLabel());
                    statCount += 1;
                    statMap.put(CaseStageEnum.Citation.getLabel(), statCount);
                }
            }

            
            Set<String> keySet = statMap.keySet();

            List<String> pieColors = new ArrayList<>();
            List<LegendItem> legend = new ArrayList<>();

            SystemCoordinator sc = getSystemCoordinator();

            for(String k: keySet){
                Integer i = statMap.get(k);
                enfStatusNumList.add(i);
                String color = sc.generateRandomRGBColor();
                pieColors.add(color);
                LegendItem li = new LegendItem();
                li.setColorRGBString(color);
                li.setValue(i);
                li.setTitle(k);
                legend.add(li);

            }
            rpt.setPieEnforcementLegend(legend);
            
            dataSet.setBackgroundColor(pieColors);
            dataSet.setData(enfStatusNumList);
            pieData.addChartDataSet(dataSet);

            rpt.getPieEnforcement().setData(pieData);
        }
    }
      
      
    /**
     * Builds a pie chart about citations
     * @param rpt 
     */
      private void initPieCitation(ReportConfigCECaseList rpt){
        if(rpt != null){

            rpt.setPieCitation(new PieChartModel());
            ChartData pieData = new ChartData();

            PieChartDataSet dataSet = new PieChartDataSet();
            List<Number> statusCounts = new ArrayList<>();
            
            List<CECaseDataHeavy> nccl = rpt.assembleNonClosedCaseList();
            List<Citation> citl = new ArrayList<>();
            for(CECaseDataHeavy cse: nccl){
                citl.addAll(cse.assembleCitationListNonDrafts());
            }
            
            rpt.setCitationList(citl);
           
            Map<String, Integer> citStatusMap = new HashMap<>();

            // COUNT BY Citation status
            if(!citl.isEmpty()){
                for(Citation cit: citl){
                    String statTitle = cit.getStatus().getStatusTitle();
                    if(citStatusMap.containsKey(statTitle)){
                        Integer statCount = citStatusMap.get(statTitle) + 1;
                        citStatusMap.put(statTitle, statCount);
                    } else {
                        citStatusMap.put(statTitle, 1);
                    }
                }
            }

            Set<String> keySet = citStatusMap.keySet();

            List<String> pieColors = new ArrayList<>();
            List<LegendItem> legend = new ArrayList<>();

            SystemCoordinator sc = getSystemCoordinator();

            for(String k: keySet){
                Integer i = citStatusMap.get(k);
                statusCounts.add(i);
                String color = sc.generateRandomRGBColor();
                pieColors.add(color);
                LegendItem li = new LegendItem();
                li.setColorRGBString(color);
                li.setValue(i);
                li.setTitle(k);
                legend.add(li);

            }
            rpt.setPieCitationLegend(legend);
            dataSet.setData(statusCounts);
            dataSet.setBackgroundColor(pieColors);
            pieData.addChartDataSet(dataSet);
            rpt.getPieCitation().setData(pieData);
        }
    }
      
      
    /**
     * Builds a pie chart about case closures
     * @param rpt 
     */
      private void initPieClosure(ReportConfigCECaseList rpt){
        rpt.setPieClosure(new PieChartModel());
        ChartData pieData = new ChartData();
        
        PieChartDataSet dataSet = new PieChartDataSet();
        List<Number> closingEventCountList = new ArrayList<>();
        
        List<EventCnF> evList = new ArrayList<>();
        
        // GET ONLY CLOSING EVENTS
        if(rpt.getCaseListClosedInDateRange() != null && !rpt.getCaseListClosedInDateRange().isEmpty()){
            for(CECaseDataHeavy cse: rpt.getCaseListClosedInDateRange()){
                List<EventCnF> evl = cse.getEventList(ViewOptionsActiveHiddenListsEnum.VIEW_ALL);
                if(!evl.isEmpty()){
                    for(EventCnF ev: evl){
                        if(ev.getCategory().getEventType() == EventType.Closing){
                            evList.add(ev);
                            break; // only add the first closing event encountered
                        }
                    }
                }
            }
        }
        
        Map<String, Integer> closeEventMap = new HashMap<>();
        
        // COUNT BY CLOSE EVENT CATEGORY TITLE
        if(!evList.isEmpty()){
            for(EventCnF ce: evList){
                if(closeEventMap.containsKey(ce.getCategory().getEventCategoryTitle())){
                    Integer catCount = closeEventMap.get(ce.getCategory().getEventCategoryTitle()) + 1;
                    closeEventMap.put(ce.getCategory().getEventCategoryTitle(), catCount);
                } else {
                    closeEventMap.put(ce.getCategory().getEventCategoryTitle(), 1);
                }
            }
        }
        
        Set<String> keySet = closeEventMap.keySet();
        
        List<String> pieColors = new ArrayList<>();
        List<LegendItem> legend = new ArrayList<>();
        
        SystemCoordinator sc = getSystemCoordinator();
        
        for(String k: keySet){
            Integer i = closeEventMap.get(k);
            closingEventCountList.add(i);
            String color = sc.generateRandomRGBColor();
            pieColors.add(color);
            LegendItem li = new LegendItem();
            li.setColorRGBString(color);
            li.setValue(i);
            li.setTitle(k);
            legend.add(li);
            
        }
        
        rpt.setPieClosureLegend(legend);
        
        // Inject into our pie
        dataSet.setData(closingEventCountList);
        dataSet.setBackgroundColor(pieColors);
        pieData.addChartDataSet(dataSet);
        rpt.getPieClosure().setData(pieData);
    }
      
      /**
       * In-process logic for violations: Horizontal cannot be stacked?
       * @param rpt 
       */
    private void initBarViolationsPastMonth(ReportConfigCECaseList rpt){
          
          
          HorizontalBarChartModel hbcm = new HorizontalBarChartModel();
          ChartData barData = new ChartData();
          
          BarChartDataSet dsNewViols = new BarChartDataSet();
          
          dsNewViols.setLabel("New Violations");
          dsNewViols.setBackgroundColor("rgb(255,9,122)");
          List<Number> dsNewViolsVals = new ArrayList<>();
//          dsNewViolsVals.add(rpt.getViolationsLoggedNOVDateRange().size());
          dsNewViolsVals.add(6);
          dsNewViols.setData(dsNewViolsVals);
          
          BarChartDataSet dsCompliance = new BarChartDataSet();
          dsCompliance.setLabel("Compliance");
          dsCompliance.setBackgroundColor("rgb(60,9,122)");
          List<Number> dsComplianceVals = new ArrayList<>();
//          dsComplianceVals.add(rpt.getViolationsLoggedComplianceDateRange().size());
          dsComplianceVals.add(3);
          dsCompliance.setData(dsComplianceVals);
          
          BarChartDataSet dsCited = new BarChartDataSet();
          dsCited.setLabel("Citation");
          dsCited.setBackgroundColor("rgb(255,9,3)");
          List<Number> dsCitedVals = new ArrayList<>();
//          dsCitedVals.add(rpt.getViolationsCitedDateRange().size());
          dsCitedVals.add(12);
          dsCited.setData(dsCitedVals);
          
          barData.addChartDataSet(dsNewViols);
          barData.addChartDataSet(dsCompliance);
          barData.addChartDataSet(dsCited);
          
          List<String> labels = new ArrayList<>();
          labels.add("Reporting Period");
          barData.setLabels(labels);
          
          hbcm.setData(barData);
          
        BarChartOptions options = new BarChartOptions();
        CartesianScales cScales = new CartesianScales();
        CartesianLinearAxes linearAxes = new CartesianLinearAxes();
        linearAxes.setStacked(true);    
        cScales.addXAxesData(linearAxes);
        cScales.addYAxesData(linearAxes);
        options.setScales(cScales);
        
        Title title = new Title();
        title.setDisplay(true);
        title.setText("Violation Statistics");
        options.setTitle(title);
        
        Tooltip tooltip = new Tooltip();
        tooltip.setMode("index");
        tooltip.setIntersect(false);
        options.setTooltip(tooltip);  
        
        hbcm.setOptions(options);
        
        rpt.setBarViolationsReport(hbcm);
          
          
      }

    /**
     * Primary configuration mechanism for customizing report data from the
     * ceCases.xhtml display. The logic inside me makes sure that hidden events
     * don't make it out to the report, etc. So the ReportConfig object is ready
     * for display and printing
     *
     * @param rptCse
     * @return
     * @throws IntegrationException
     * @throws com.tcvcog.tcvce.domain.BObStatusException
     */
    public ReportConfigCECase report_transformCECaseForReport(ReportConfigCECase rptCse) throws IntegrationException, BObStatusException {
        CaseIntegrator ci = getCaseIntegrator();
        // we actually get an entirely new object instead of editing the 
        // one we used throughout the ceCases.xhtml
        CECaseDataHeavy c = rptCse.getCse();

        List<EventCnF> evList = new ArrayList<>();
        Iterator<EventCnF> iter = c.getEventList(ViewOptionsActiveHiddenListsEnum.VIEW_ALL).iterator();
        while (iter.hasNext()) {
            EventCnF ev = iter.next();

            // toss out hidden events unless the user wants them
            if (ev.isHidden() && !rptCse.isIncludeHiddenEvents()) {
                continue;
            }
            // toss out inactive events unless user wants them
            if (!ev.isActive() && !rptCse.isIncludeInactiveEvents()) {
                continue;
            }
            // toss out events only available internally to the muni users unless user wants them
            evList.add(ev);
        }
        rptCse.setEventListForReport(evList);

        List<NoticeOfViolation> noticeList = new ArrayList<>();
        Iterator<NoticeOfViolation> iterNotice = c.getNoticeList().iterator();
        while (iterNotice.hasNext()) {
            NoticeOfViolation nov = iterNotice.next();
            // skip unsent notices
            if (nov.getSentTS() == null) {
                continue;
            }
            // if the user dones't want all notices, skip returned notices
            if (!rptCse.isIncludeAllNotices() && nov.getReturnedTS() != null) {
                continue;
            }
            noticeList.add(nov);
        }
        rptCse.setNoticeListForReport(noticeList);
        return rptCse;
    }

    // *************************************************************************
    // *                     PUBLIC FACING                                     *
    // *************************************************************************
    /**
     * Called by the PIBCECaseBB when a public user wishes to add an event to
     * the case they are viewing online. This method stitches together the
     * message text, messenger name, and messenger phone number before passing
     * the info back to the EventCoordinator
     *
     * @param caseID can be extracted from the public info bundle
     * @param msg the text of the message the user wants to add to the case
     * @param messagerName the first and last name of the person submitting the
     * message Note that this submission info is not YET wired into the actual
     * Person objects in the system.
     *
     * @param messagerPhone a simple String rendering of whatever the user types
     * in. Length validation only.
     * @throws com.tcvcog.tcvce.domain.IntegrationException
     * @throws com.tcvcog.tcvce.domain.BObStatusException
     * @throws com.tcvcog.tcvce.domain.EventException
     */
    public void public_attachPublicMessage(int caseID, String msg, String messagerName, String messagerPhone) throws IntegrationException, BObStatusException, EventException {
        StringBuilder sb = new StringBuilder();
        sb.append("Case note added by ");
        sb.append(messagerName);
        sb.append(" with contact number: ");
        sb.append(messagerPhone);
        sb.append(": ");
        sb.append("<br /><br />");
        sb.append(msg);

        EventCoordinator ec = getEventCoordinator();
        ec.attachPublicMessagToCECase(caseID, sb.toString());

    }

    // *************************************************************************
    // *                     EVENTS                                            *
    // *************************************************************************
    /**
     * For inter-coordinator use only! Not called by backing beans. Primary
     * event life cycle control method which is called by EventCoordinator each
     * time an event is added to a code enf case. The primary business logic
     * related to which events can be attached to a case at any given case phase
     * is implemented in this coordinator method.
     *
     * Its core operation is to check case and event related qualities and
     * delegate further processing to event-type specific methods also found in
     * this coordinator
     *
     * @param evList
     * @param cse the case to which the event should be added
     * @param ev the event to add to the case also included in this call
     * @return a reference to the same incoming List of EventCnF objects that is
     * passed in. Any additional events that need to be attached should be
     * appended to the end of this list
     *
     * @throws com.tcvcog.tcvce.domain.BObStatusException
     * @throws com.tcvcog.tcvce.domain.IntegrationException
     * @throws com.tcvcog.tcvce.domain.ViolationException
     * @throws com.tcvcog.tcvce.domain.EventException
     */
    protected List<EventCnF> events_addEvent_processForCECaseDomain(
            List<EventCnF> evList,
            CECaseDataHeavy cse,
            EventCnF ev)
            throws BObStatusException,
            IntegrationException,
            ViolationException,
            EventException {
        EventType eventType = ev.getCategory().getEventType();
        EventIntegrator ei = getEventIntegrator();
        if (evList == null || cse == null || ev == null) {
            throw new BObStatusException("Null argument to addEvent_ceCaseDomain");
        }

        switch (eventType) {
            case Action:
                break;
            case Compliance:
                // TODO: Finish me
                break;
            case Closing:
//                insertedEventID = processClosingEvent(cse, ev);
                break;
            default:
                ev.setCeCaseID(cse.getCaseID());
        } // close switch
        return evList;
    } // close method

    /**
     * Logic container for assessing if any additinal events, etc should be
     * created as a result of an EventRule pass
     *
     * @param cse
     * @param rule
     * @throws IntegrationException
     * @throws BObStatusException
     * @throws ViolationException
     */
    protected void events_processCaseOnEventRulePass(CECaseDataHeavy cse, EventRuleAbstract rule)
            throws IntegrationException, BObStatusException, ViolationException {

        CaseIntegrator ci = getCaseIntegrator();
        EventCoordinator ec = getEventCoordinator();
        EventCnF newEvent = null;

//        CasePhaseEnum oldCP = cse.getCasePhase();
//        ec.generateAndInsertPhaseChangeEvent(cse, oldCP, rule);
//        if(rule.getTriggeredEventCategoryID() != 0){
//            newEvent = ec.initEvent(cse, ec.initEventCategory(rule.getTriggeredEventCategoryID()));
//            
//            events_addEvent_processForCECaseDomain(cse, newEvent, null);
//            System.out.println("CaseCoordinator.events_processCaseOnEventRulePass "  + newEvent.getCategory().getEventCategoryTitle());
//        }
//        
    }

    /**
     * Implements business rules for determining which event types are allowed
     * to be attached to the given CECaseDataHeavy based on the case's phase and
     * the user's permissions in the system.
     *
     * Used for displaying the appropriate event types to the user on the
     * cecases.xhtml page
     *
     * @param c the CECaseDataHeavy on which the event would be attached
     * @param u the User doing the attaching
     * @return allowed EventTypes for attaching to the given case
     */
    public List<EventType> events_getPermittedEventTypesForCECase(CECaseDataHeavy c, User u) {
        List<EventType> typeList = new ArrayList<>();
        RoleType role = u.getRoleType();

        if (role == RoleType.EnforcementOfficial
                || u.getRoleType() == RoleType.Developer) {
            typeList.add(EventType.Action);
            typeList.add(EventType.Timeline);
        }

        if (role != RoleType.MuniReader) {
            typeList.add(EventType.Communication);
            typeList.add(EventType.Meeting);
            typeList.add(EventType.Custom);
        }
        return typeList;
    }

    /**
     * Logic intermediary event for creating events documenting a CECase's
     * closure
     *
     * @param c
     * @param e
     * @return
     * @throws IntegrationException
     * @throws BObStatusException
     */
    private int events_processClosingEvent(CECaseDataHeavy c, EventCnF e, UserAuthorized ua) throws IntegrationException, BObStatusException {
        EventIntegrator ei = getEventIntegrator();

        CasePhaseEnum closedPhase = CasePhaseEnum.Closed;
//        c.setCasePhase(closedPhase);

        if(c.getClosingDate() == null){
            c.setClosingDate(LocalDateTime.now());
            cecase_updateCECaseMetadata(c, ua);
        }
        // now load up the closing event before inserting it
        // we'll probably want to get this text from a resource file instead of
        // hardcoding it down here in the Java
        e.setUserCreator(getSessionBean().getSessUser());
        e.setDescription(getResourceBundle(Constants.MESSAGE_TEXT).getString("automaticClosingEventDescription"));
        e.setNotes(getResourceBundle(Constants.MESSAGE_TEXT).getString("automaticClosingEventNotes"));
        return ei.insertEvent(e);
    }

    // *************************************************************************
    
    // *                     NOTICES OF VIOLATION                              *
    // *************************************************************************
    /**
     * Sets mailing fields to null] Params changed to take in UserAuthorized
     * during corruption recovery
     *
     * @param nov
     * @param user
     * @throws IntegrationException
     * @throws com.tcvcog.tcvce.domain.AuthorizationException
     */
    public void nov_ResetMailing(NoticeOfViolation nov, UserAuthorized user) throws IntegrationException, AuthorizationException {
        CaseIntegrator ci = getCaseIntegrator();
        if (user.getMyCredential().isHasSysAdminPermissions()) {
            ci.novResetMailingFieldsToNull(nov);
        } else {
            throw new AuthorizationException("User does not have sufficient acces righst to clear notice mailing fields");
        }
    }

    /**
     * 
     * @param noticeID
     * @return
     * @throws IntegrationException
     */
    public NoticeOfViolation nov_getNoticeOfViolation(int noticeID) 
            throws IntegrationException, BObStatusException {
        CaseIntegrator ci = getCaseIntegrator();
        NoticeOfViolation nov = ci.novGet(noticeID);
        if(nov != null){
            if(nov.getNotifyingOfficer() == null && nov.getCreationBy() != null){
                nov.setNotifyingOfficer(nov.getCreationBy());
            } 
        }
        
        return nov;
    }
    
    
    /**
     * Utility method for extracting a list of NOV ID's from the db
     * @param idl
     * @return
     * @throws IntegrationException
     * @throws com.tcvcog.tcvce.domain.BObStatusException
     */
    public List<NoticeOfViolation> nov_getNoticeOfViolationList(List<Integer> idl) 
            throws IntegrationException, BObStatusException {
        List<NoticeOfViolation> novl = new ArrayList<>();
        if(idl != null && !idl.isEmpty()){
            for(Integer i: idl){
                novl.add(nov_getNoticeOfViolation(i));
            }
        }
        return novl;
    }
    
    
    
    
    
    /**
     * Called when first creating a notice of violation
     *
     * @param cse
     * @param mdh
     * @param ua
     * @return
     * @throws AuthorizationException
     */
    public NoticeOfViolation nov_GetNewNOVSkeleton(CECaseDataHeavy cse, MunicipalityDataHeavy mdh, UserAuthorized ua) throws AuthorizationException {
        SystemIntegrator si = getSystemIntegrator();
        NoticeOfViolation nov = new NoticeOfViolation();
        
        nov.setViolationList(new ArrayList<>());
        nov.setDateOfRecord(LocalDateTime.now());
        nov.setBlocksBeforeViolations(new ArrayList<>());
        nov.setBlocksAfterViolations(new ArrayList<>());

        try {
            nov.setStyle(si.getPrintStyle(mdh.getDefaultNOVStyleID()));
        } catch (IntegrationException ex) {
            System.out.println(ex);
        }

        // loop over unresolved violations on case and generate CodeViolationDisplayable obects
        Iterator<CodeViolation> iter = cse.getViolationListUnresolved().iterator();
        while (iter.hasNext()) {
            CodeViolation cv = iter.next();
            CodeViolationDisplayable cvd = new CodeViolationDisplayable(cv);
            // start with sensible default values
            cvd.setIncludeHumanFriendlyText(false);
            cvd.setIncludeOrdinanceText(true);
            cvd.setIncludeViolationPhotos(false);
            nov.getViolationList().add(cvd);
        }
        
        nov.setCreationBy(ua);
        nov.setNotifyingOfficer(ua);
        
        
        
        return nov;

    }

    /**
     * NOV is ready to send - And coordinate creating an event to document this
     *
     * @param c
     * @param nov
     * @param ua
     * @throws BObStatusException
     * @throws IntegrationException
     * @throws EventException
     * @throws ViolationException
     */
    public void nov_LockAndQueue(CECaseDataHeavy c, NoticeOfViolation nov, UserAuthorized ua)
            throws BObStatusException, IntegrationException, EventException, ViolationException {

        if(c == null || nov == null || ua == null){
            throw new BObStatusException("Cannot lock notice with null case, nov, or user");
        }
        CaseIntegrator ci = getCaseIntegrator();
        PersonCoordinator pc = getPersonCoordinator();
        PersonIntegrator pi = getPersonIntegrator();

        if (nov.getLockedAndqueuedTS() == null) {
            int ghostID = pc.createChostPerson(nov.getRecipient(), ua);
            nov.setRecipient(pi.getPerson(ghostID));
            nov.setLockedAndqueuedTS(LocalDateTime.now());
            nov.setLockedAndQueuedBy(ua);
            ci.novLockAndQueueForMailing(nov);
            System.out.println("CaseCoordinator.novLockAndQueue | NOV locked in integrator");
        } else {
            throw new BObStatusException("Notice is already locked and queued for sending");
        }
     
        
    }
    
    
    /**
     * Create a new text block skeleton with injectrable set to true
     * @param muni
     * @return 
     */
    public TextBlock nov_getTemplateBlockSekeleton(Municipality muni){
        TextBlock tb = new TextBlock();
        tb.setMuni(muni);
        tb.setInjectableTemplate(true);
        tb.setTextBlockCategoryID(Integer.parseInt(getResourceBundle(Constants.DB_FIXED_VALUE_BUNDLE)
                .getString("nov_textblocktemplate_categoryid")));
        tb.setTextBlockText("inject violations with: ***VIOLATIONS***");
        
        return tb;
                
    }

    /**
     * Called when the NOV is ready to get written to the DB --but before
     * queuing for sending
     *
     * @param nov
     * @param cse
     * @param usr
     * @return
     * @throws IntegrationException
     */
    public int nov_InsertNotice(NoticeOfViolation nov, CECaseDataHeavy cse, User usr) throws IntegrationException {
        CaseIntegrator ci = getCaseIntegrator();
        System.out.println("CaseCoordinator.novInsertNotice");
        return ci.novInsert(cse, nov);
    }

    /**
     * Logic pass-through for NOV updates
     *
     * @param nov
     * @throws IntegrationException
     * @throws com.tcvcog.tcvce.domain.BObStatusException
     */
    public void nov_update(NoticeOfViolation nov) throws IntegrationException, BObStatusException {
        CaseIntegrator ci = getCaseIntegrator();
        if(nov != null){
            if(nov.getLockedAndqueuedTS() == null){
                ci.novUpdateNotice(nov);
            } else {
                throw new BObStatusException("Cannot update a locked notice.");
            }
            
        }
    }

    /**
     * Business logic intermediary method for marking a NOV as having already
     * been sent to the property
     *
     * @param ceCase
     * @param nov
     * @param user
     * @throws BObStatusException
     * @throws EventException
     * @throws IntegrationException
     */
    public void nov_markAsSent(CECaseDataHeavy ceCase, NoticeOfViolation nov, UserAuthorized user) throws BObStatusException, EventException, IntegrationException {
        
        CaseIntegrator ci = getCaseIntegrator();
        
        nov.setSentTS(LocalDateTime.now());
        nov.setSentBy(user);
        ci.novRecordMailing(nov);
        
        nov_logNOVSentEvent(ceCase, nov, user);
        
    }
    
    /**
     * 
     * @param ceCase
     * @param nov
     * @param user
     * @throws IntegrationException
     * @throws BObStatusException 
     */
    private void nov_logNOVSentEvent(CECaseDataHeavy ceCase, NoticeOfViolation nov, UserAuthorized user) 
            throws IntegrationException, BObStatusException, EventException{
        EventCoordinator evCoord = getEventCoordinator();
        
        
        EventCnF noticeEvent = evCoord.initEvent(ceCase, evCoord.initEventCategory(Integer.parseInt(getResourceBundle(
                Constants.EVENT_CATEGORY_BUNDLE).getString("noticeMailed"))));
        
        String queuedNoticeEventNotes = getResourceBundle(Constants.MESSAGE_TEXT).getString("noticeMailedEventDesc");
        
        noticeEvent.setDescription(queuedNoticeEventNotes);
        noticeEvent.setUserCreator(user);
        
        evCoord.addEvent(noticeEvent, ceCase, user);
        
        
        
    }

    /**
     * Logic container for configuring a NOV as returned by the postal service
     *
     * @param c
     * @param nov
     * @param user
     * @throws IntegrationException
     * @throws BObStatusException
     */
    public void nov_markAsReturned(CECaseDataHeavy c, NoticeOfViolation nov, User user) throws IntegrationException, BObStatusException {
        CaseIntegrator ci = getCaseIntegrator();
        nov.setReturnedTS(LocalDateTime.now());
        nov.setReturnedBy(user);
        ci.novRecordReturnedNotice(nov);
    }

    /**
     * Checks logic before deleting NOVs. It's rare to be able to delete
     * something but an unmailed NOV is a very low impact loss
     *
     * @param nov
     * @throws BObStatusException
     */
    public void nov_delete(NoticeOfViolation nov) throws BObStatusException {
        CaseIntegrator ci = getCaseIntegrator();

        //cannot delete a letter that was already sent
        if (nov != null && nov.getSentTS() != null) {
            throw new BObStatusException("Cannot delete a letter that has been sent");
        } else {
            try {
                ci.novDelete(nov);
            } catch (IntegrationException ex) {
                System.out.println(ex);
                getFacesContext().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Unable to delete notice of violation due to a database error", ""));
            }
        }
    }

    
    /**
     * @param ps
     * @param blob
     * @return the PrintStyle with the header image set
     * @throws IntegrationException
     * @throws BObStatusException 
     */
      public PrintStyle nov_updateStyleHeaderImage(PrintStyle ps, BlobLight blob) throws IntegrationException, BObStatusException {
        CaseIntegrator ci = getCaseIntegrator();
        if(ps == null || blob == null){
            throw new BObStatusException("Cannot update header image with null style or blob");
            
        }

        ps.setHeader_img_id(blob.getPhotoDocID());
        ci.novUpdateHeaderImage(ps);
     
        return ps;
        
    }
      
    /**
     * Logic container for creating a NOV from a given template/category ID
     * 
     * @param nov
     * @param categoryID 
     * @return  with text blocks
     * loaded up into before and after based on default sort order
     * @throws com.tcvcog.tcvce.domain.IntegrationException  
     */
    public NoticeOfViolation nov_assembleNOVFromBlocks(NoticeOfViolation nov, int categoryID) throws IntegrationException{
        CaseIntegrator ci = getCaseIntegrator();
        List<TextBlock> blockList = new ArrayList<>();
        
        
        if(categoryID != 0){
            blockList.addAll(ci.getTextBlocksByCategory(categoryID));
        }
        
        if(!blockList.isEmpty()){
            for(TextBlock tb: blockList){
                if(tb.getPlacementOrder() < 0){
                    nov.getBlocksBeforeViolations().add(tb);
                    Collections.sort(nov.getBlocksBeforeViolations());
                } else if (tb.getPlacementOrder() > 0){
                    nov.getBlocksAfterViolations().add(tb);
                    Collections.sort(nov.getBlocksAfterViolations());
                }
            }
            
        }
        return nov;
    }
    
    
    /**
     * Connects with the EventCoordinator to create a follow-up event for NOVs
     * @param cse
     * @param nov
     * @param ua
     * @return the generated event
     * @throws com.tcvcog.tcvce.domain.BObStatusException
     * @throws com.tcvcog.tcvce.domain.IntegrationException
     * @throws com.tcvcog.tcvce.domain.EventException
     */
    public EventCnF nov_createFollowupEvent(CECase cse, NoticeOfViolation nov, UserAuthorized ua) throws BObStatusException, IntegrationException, EventException{
        if(cse == null || nov == null || ua ==null){
            throw new BObStatusException("Cannot create followup event for NOV with null Case, NOV, or User");
        }
        
        EventCoordinator ec = getEventCoordinator();
        EventCategory cat = ec.getEventCategory(Integer.parseInt(getResourceBundle(Constants.DB_FIXED_VALUE_BUNDLE)
                .getString("nov_followupeventcatid")));
        EventCnF fuev = ec.initEvent((IFace_EventRuleGoverned) cse,cat );
        
        fuev.setUserCreator(ua);
        fuev.setTimeStart(LocalDateTime.now().plusDays(nov.getFollowupEventDaysRequest()));
        fuev.setTimeEnd(fuev.getTimeStart().minusMinutes(cat.getDefaultDurationMins()));
        
        List<EventCnF> evlist = ec.addEvent(fuev, (IFace_EventRuleGoverned) cse, ua);
        return evlist.get(0);
        
    }
    
    /**
     * Tool for building an NOV from a template block
     * @param nov
     * @param temp
     * @param cse
     * @return
     * @throws BObStatusException 
     */
    public NoticeOfViolation nov_assembleNOVFromTemplate(NoticeOfViolation nov, TextBlock temp, CECase cse) throws BObStatusException{
        
        CaseIntegrator ci = getCaseIntegrator();
        if(nov == null || temp == null || cse ==null){
            throw new BObStatusException("Cannot build a notice with null NOV, template or case");
        }
        
        String template = temp.getTextBlockText();
        int startOfInjectionPoint = template.indexOf(Constants.NOV_VIOLATIONS_INJECTION_POINT);
        System.out.println("CaseCoor.nov_assembleNOVFromTemplate: Injection point found starts at: " + startOfInjectionPoint);
        // If the injection point is not found, put the entire template block as text before Violations
        if(startOfInjectionPoint != -1){
            nov.setNoticeTextBeforeViolations(template.substring(0, startOfInjectionPoint));
            nov.setNoticeTextAfterViolations(template.substring(startOfInjectionPoint + Constants.NOV_VIOLATIONS_INJECTION_POINT.length()));
        } else {
            nov.setNoticeTextBeforeViolations(template);
        }
        
        
        return nov;
    }
      
    /**
     * Updates only the notes field on Notice of Violation
     *
     * @param mbp
     * @param nov
     * @throws BObStatusException
     * @throws IntegrationException
     */
    public void nov_updateNotes(MessageBuilderParams mbp, NoticeOfViolation nov) throws BObStatusException, IntegrationException {
        CaseIntegrator ci = getCaseIntegrator();
        SystemCoordinator sc = getSystemCoordinator();
        if (nov == null || mbp == null) {
            throw new BObStatusException("Cannot append if notes, case, or user are null");
        }

        nov.setNotes(sc.appendNoteBlock(mbp));

        ci.novUpdateNotes(nov);
    }

    
    
    
    
    // *************************************************************************
    // *                     BLOBS                                             *
    // *************************************************************************
    
    /**
     * Primary pathway for inserting blobs for attachment to a CECase
     * Liaises with the BlobCoordinator and Integrator as needed
     * @param ua
     * @param blob
     * @param cse
     * @return
     * @throws BlobException
     * @throws IOException
     * @throws IntegrationException
     * @throws BlobTypeException 
     */
    public Blob blob_ceCase_attachBlob(UserAuthorized ua, Blob blob, CECase cse) 
            throws BlobException, IOException, IntegrationException, BlobTypeException{
        BlobCoordinator bc = getBlobCoordinator();
        BlobIntegrator bi = getBlobIntegrator();
        
        Blob freshBlob = bc.storeBlob(blob);
        bi.linkBlobToCECase(blob, cse);
        return freshBlob;
        
    }
    
    
    
    // *************************************************************************
    // *                     CITATIONS                                         *
    // *************************************************************************
    
    /**
     * Logic container for assembling a list of citations by a CECase
     * @param cse
     * @return fully-baked citation objects
     * @throws IntegrationException 
     */
    private List<Citation> citation_getCitationList(CECase cse) throws IntegrationException{
        CaseIntegrator ci = getCaseIntegrator();
        List<Citation> citList = new ArrayList<>();
        if(cse!= null){
            List<Integer> citIDList = ci.getCitations(cse);
            if(citIDList != null && !citIDList.isEmpty()){
                for(Integer i: citIDList){
                    citList.add(citation_getCitation(i));
                }
            }
        }
        return citList;
    }
    
    /**
     * Getter for Citation objects
     *
     * @param citationID
     * @return
     * @throws IntegrationException
     */
    public Citation citation_getCitation(int citationID) throws IntegrationException {
        CaseIntegrator ci = getCaseIntegrator();
        Citation cit = ci.getCitation(citationID);
        cit.setViolationList(ci.getCodeViolations(ci.getCodeViolationsByCitation(cit)));

        return cit;

    }

    /**
     * Logic container for retrieving the default citation status
     * @return
     * @throws IntegrationException 
     */
    private CitationStatus citation_getStartingCitationStatus() throws IntegrationException {
        CaseIntegrator ci = getCaseIntegrator();
        CitationStatus cs = ci.getCitationStatus(Integer.parseInt(getResourceBundle(Constants.DB_FIXED_VALUE_BUNDLE)
                .getString("citationInitialStatusID")));
        return cs;
    }

    /**
     * Initializes a Citation object with a default start status
     *
     * @param ua
     * @param cse
     * @return
     * @throws BObStatusException
     * @throws com.tcvcog.tcvce.domain.IntegrationException
     */
    public Citation citation_getCitationSkeleton(UserAuthorized ua, CECase cse) throws BObStatusException, IntegrationException {
        if (!ua.getKeyCard().isHasEnfOfficialPermissions()) {
            throw new BObStatusException("Users must have enforcement office permissions to create a citation");
        }
        Citation cit = new Citation();
        cit.setCeCaseNoLists(cse);
        cit.setDateOfRecord(LocalDateTime.now());
        cit.setUserOwner(getSessionBean().getSessUser());
        cit.setIsActive(true);
        cit.setStatus(citation_getStartingCitationStatus());
        cit.setOrigin_courtentity(getSessionBean().getSessMuni().getCourtEntities().get(0));
        List<CodeViolation> cvlst = new ArrayList<>();
        if (cse.getViolationList() != null && !cse.getViolationList().isEmpty()) {
            for (CodeViolation v : cse.getViolationList()) {
                if (v.getActualComplianceDate() == null) {
                    cvlst.add(v);
                    System.out.println("CaseCoordinator.citation_getCitationSkeleton: Adding ViolID: " + v.getViolationID());
                }
            }
        }
        cit.setViolationList(cvlst);

        return cit;

    }

    /**
     * Called to create a new citation for a given List of CodeViolation objects
     *
     * @param violationList
     * @return
     */
    public Citation citation_generateNewCitation(List<CodeViolation> violationList) {
        Citation newCitation = new Citation();
        List<CodeViolation> al = new ArrayList<>();
        ListIterator<CodeViolation> li = violationList.listIterator();
        CodeViolation cv;

        StringBuilder notesBuilder = new StringBuilder();
        notesBuilder.append("Failure to comply with the following ordinances:\n");

        while (li.hasNext()) {

            cv = li.next();
            System.out.println("CaseCoordinator.generateNewCitation | linked list item: "
                    + cv.getDescription());

            // build a nice note section that lists the elements cited
            notesBuilder.append("* Chapter ");
            notesBuilder.append(cv.getCodeViolated().getOrdchapterNo());
            notesBuilder.append(":");
            notesBuilder.append(cv.getCodeViolated().getOrdchapterTitle());
            notesBuilder.append(", Section ");
            notesBuilder.append(cv.getCodeViolated().getOrdSecNum());
            notesBuilder.append(":");
            notesBuilder.append(cv.getCodeViolated().getOrdSecTitle());
            notesBuilder.append(", Subsection ");
            notesBuilder.append(cv.getCodeViolated().getOrdSubSecNum());
            notesBuilder.append(": ");
            notesBuilder.append(cv.getCodeViolated().getOrdSubSecTitle());
            notesBuilder.append("\n\n");

            al.add(cv);

        }
        newCitation.setViolationList(al);
        newCitation.setNotes(notesBuilder.toString());
        newCitation.setIsActive(true);

        return newCitation;
    }

    /**
     * Logic intermediary for recording that a Citation has been issued, meaning
     * sent to the proper magisterial authority
     *
     * @param c
     * @throws IntegrationException
     */
    public void citation_issueCitation(Citation c) throws IntegrationException {
        CaseIntegrator ci = getCaseIntegrator();
        ci.insertCitation(c);

    }

    /**
     * Logic intermediary for updating fields on Citations in the DB
     *
     * @param c
     * @throws IntegrationException
     * @throws com.tcvcog.tcvce.domain.BObStatusException
     */
    public void citation_updateCitation(Citation c) throws IntegrationException, BObStatusException {
        CaseIntegrator ci = getCaseIntegrator();
        if (c.getStatus() != null) {
            ci.updateCitation(c);
        } else {
            throw new BObStatusException("Cannot update this citation at its current status");
        }

    }

    /**
     * Logic intermediary for updating fields on Citations in the DB
     *
     * @param c
     * @throws IntegrationException
     * @throws com.tcvcog.tcvce.domain.BObStatusException
     */
    public void citation_removeCitation(Citation c) throws IntegrationException, BObStatusException {
        CaseIntegrator ci = getCaseIntegrator();
        
        if(c != null){
            c.setIsActive(false);
            ci.updateCitation(c);
        }
        // Deactivated to allow for any citation to be removed
        

//        if (c.getStatus() != null && !c.getStatus().isNonStatusEditsForbidden()) {
//            c.setIsActive(false);
//            ci.updateCitation(c);
//        } else {
//            throw new BObStatusException("Cannot remove this citation at its current status");
//        }

    }

    /**
     * Logic intermediary for updating citation status only
     *
     * @param c
     * @throws IntegrationException
     */
    public void citation_updateCitationStatus(Citation c) throws IntegrationException {
        CaseIntegrator ci = getCaseIntegrator();
        ci.updateCitation(c);

    }

    /**
     * Updates only the notes field on Citation
     *
     * @param mbp
     * @param cit
     * @throws BObStatusException
     * @throws IntegrationException
     */
    public void citation_updateNotes(MessageBuilderParams mbp, Citation cit) throws BObStatusException, IntegrationException {
        CaseIntegrator ci = getCaseIntegrator();
        SystemCoordinator sc = getSystemCoordinator();
        if (cit == null || mbp == null) {
            throw new BObStatusException("Cannot append if notes, case, or user are null");
        }

        cit.setNotes(sc.appendNoteBlock(mbp));

        ci.updateCitationNotes(cit);
    }

//    --------------------------------------------------------------------------
//    ********************* CE Action Requests *********************************
//    --------------------------------------------------------------------------
    /**
     * Factory method for our CEActionRequests - initializes the date as well
     *
     * @return The CEActionRequest ready for populating with user values
     */
    public CEActionRequest cear_getInititalizedCEActionRequest() {
        System.out.println("CaseCoordinator.getNewActionRequest");
        CEActionRequest cear = new CEActionRequest();
        // start by writing in the current date
        cear.setDateOfRecord(LocalDateTime.now());

        return new CEActionRequest();

    }

    /**
     * Business logic intermediary method for CEActionRequests. Calls the
     * CEAction Integrator
     *
     * @param cearid
     * @return
     * @throws IntegrationException
     */
    public CEActionRequest cear_getCEActionRequest(int cearid) throws IntegrationException {
        CEActionRequestIntegrator ceari = getcEActionRequestIntegrator();
        return ceari.getActionRequestByRequestID(cearid);
    }

    /**
     * Utility method for determining whether or not the panel of Code
     * Enforcement request routing buttons can be pressed. Used by the view for
     * setting disabled properties on buttons requests
     *
     * @param req the current CE Request
     * @param u current user
     * @return True if the current user can route the given ce request
     */
    public boolean cear_determineCEActionRequestRoutingActionEnabledStatus(
            CEActionRequest req,
            UserAuthorized u) {
        if (req != null && u.getMyCredential() != null) {
            if ((req.getRequestStatus().getStatusID()
                    == Integer.parseInt(getResourceBundle(Constants.DB_FIXED_VALUE_BUNDLE)
                            .getString("actionRequestInitialStatusCode")))
                    && u.getMyCredential().isHasEnfOfficialPermissions()) {
                return true;
            }
        }
        return false;
    }

    public List<CEActionRequestIssueType> cear_getIssueTypes(Municipality muni) throws IntegrationException {

        CEActionRequestIntegrator ceari = getcEActionRequestIntegrator();

        List<CEActionRequestIssueType> typeList = ceari.getRequestIssueTypeList(muni);

        return typeList;

    }

    public List<CEActionRequestIssueType> cear_getIssueTypes() throws IntegrationException {

        CEActionRequestIntegrator ceari = getcEActionRequestIntegrator();

        List<CEActionRequestIssueType> typeList = ceari.getRequestIssueTypeList();

        return typeList;

    }

    // *************************************************************************
    // *                     VIOLATIONS                                        *
    // *************************************************************************
    /**
     * Violation status enum values have associated ordinal values which this
     * method looks at to determine the highest one, which it returns and then
     * goes on break
     *
     * @param vList
     * @return
     */
    public int violation_determineMaxViolationStatus(List<CodeViolation> vList) {
        int maxStatus = -1;
        if (vList != null) {
            for (CodeViolation cv : vList) {
                if (cv.getStatus().getOrder() > maxStatus) {
                    maxStatus = cv.getStatus().getOrder();
                }
            }
        }
        return maxStatus;
    }

    /**
     * Iterates over all of a case's violations and checks for compliance. If
     * all of the violations have a compliance date, the case is automatically
     * closed and a case closing event is generated and added to the case
     *
     * @param c the case whose violations should be checked for compliance
     * @param ua
     * @throws IntegrationException
     * @throws BObStatusException
     * @throws ViolationException
     * @throws com.tcvcog.tcvce.domain.EventException
     */
    public void violation_checkForFullComplianceAndCloseCaseIfTriggered(CECaseDataHeavy c, UserAuthorized ua)
            throws IntegrationException, BObStatusException, ViolationException, EventException {

        EventCoordinator ec = getEventCoordinator();
        EventIntegrator ei = getEventIntegrator();

        boolean complianceWithAllViolations = false;

        ListIterator<CodeViolation> fullViolationLi = c.getViolationList().listIterator();

        CodeViolation cv = null;
        while (fullViolationLi.hasNext()) {
            cv = fullViolationLi.next();
            // if there are any outstanding code violations, toggle switch to 
            // false and exit the loop. Phase change will not occur
            if (cv.getActualComplianceDate() != null) {
                complianceWithAllViolations = true;
            } else {
                complianceWithAllViolations = false;
                break;
            }
        } // close while

        EventCnF complianceClosingEvent;

        if (complianceWithAllViolations) {
            complianceClosingEvent = ec.initEvent(c, ei.getEventCategory(Integer.parseInt(getResourceBundle(
                    Constants.EVENT_CATEGORY_BUNDLE).getString("closingAfterFullCompliance"))));
            ec.addEvent(complianceClosingEvent, c, ua);
            

        } // close if

    }

    /**
     * Factory method for starting the process of making a new CodeViolation
     * object
     *
     * @param c
     * @return
     * @throws com.tcvcog.tcvce.domain.BObStatusException
     */
    public CodeViolation violation_getCodeViolationSkeleton(CECaseDataHeavy c) throws BObStatusException {
        CodeViolation v = new CodeViolation();

        v.setDateOfRecord(LocalDateTime.now());
        if (c != null) {
            v.setCeCaseID(c.getCaseID());
        } else {
            throw new BObStatusException("Cannot attach violation to null case");
        }

        // control is passed back to the violationAddBB which stores this 
        // generated violation under teh activeCodeViolation in the session
        // which the ViolationAddBB then picks up and edits
        return v;
    }

    /**
     * Standard coordinator method which calls the integration method after
     * checking business rules. ALSO coordinates creating a corresponding
     * proposal to match the stipulated compliance date on the violation that's
     * added.
     *
     * @param cv
     * @param cse
     * @param ua
     * @return the database key assigned to the inserted violation
     * @throws IntegrationException
     * @throws ViolationException
     * @throws com.tcvcog.tcvce.domain.BObStatusException
     * @throws com.tcvcog.tcvce.domain.EventException
     * @throws com.tcvcog.tcvce.domain.SearchException
     */
    public int violation_attachViolationToCase(CodeViolation cv, CECaseDataHeavy cse, UserAuthorized ua)
            throws IntegrationException, ViolationException, BObStatusException, EventException, SearchException {

        if(cv == null || cse == null || ua == null) throw new BObStatusException("Cannot attach violation with null viol, case, or user");
        
        
        CaseIntegrator ci = getCaseIntegrator();
        EventCoordinator ec = getEventCoordinator();
        UserCoordinator uc = getUserCoordinator();
        CaseCoordinator cc = getCaseCoordinator();
        EventCnF tfEvent;
        PaymentCoordinator pc = getPaymentCoordinator();
        int insertedViolationID;
        int eventID;
        StringBuilder sb = new StringBuilder();

//        EventCategory eventCat = ec.initEventCategory(
//                Integer.parseInt(getResourceBundle(Constants.EVENT_CATEGORY_BUNDLE)
//                        .getString("complianceTimeframeExpiry")));
//        EventCategory eventCat = ec.initEventCategory(113);
//        tfEvent = ec.initEvent(cse, eventCat);
//        tfEvent.setTimeStart(cv.getStipulatedComplianceDate());
//        tfEvent.setUserCreator(cse.getCaseManager());
//
//        sb.append(getResourceBundle(Constants.MESSAGE_TEXT)
//                .getString("complianceTimeframeEndEventDesc"));
//        sb.append("Case: ");
//        sb.append(cse.getCaseName());
//        sb.append(" at ");
//        sb.append(cc.cecase_assembleCECasePropertyUnitHeavy(cse).getProperty().getAddress());
//        sb.append("(");
//        sb.append(cc.cecase_assembleCECasePropertyUnitHeavy(cse).getProperty().getMuni().getMuniName());
//        sb.append(")");
//        sb.append("; Violation: ");
//        sb.append(cv.getViolatedEnfElement().getHeaderString());
//        tfEvent.setDescription(sb.toString());
        violation_verifyCodeViolationAttributes(cse, cv);
        cv.setCreatedBy(ua);
        insertedViolationID = ci.insertCodeViolation(cv);
        pc.insertAutoAssignedFees(cse, cv);
        return insertedViolationID;
    }

    /**
     * 
     * @param vid
     * @return
     * @throws IntegrationException
     */
    public CodeViolation violation_getCodeViolation(int vid) 
            throws IntegrationException {
        CaseIntegrator ci = getCaseIntegrator();
        CodeViolation cv = ci.getCodeViolation(vid);
        return violation_configureCodeViolation(cv);
    }
    
    
    
    public CodeViolationPropCECaseHeavy violation_getCodeViolationPropCECaseHeavy(int vid) throws ViolationException, IntegrationException, SearchException{
        PropertyCoordinator pc = getPropertyCoordinator();
        CodeViolationPropCECaseHeavy cvpcdh = null;
        if(vid != 0){
            CodeViolation cv = violation_getCodeViolation(vid);
            cvpcdh = new CodeViolationPropCECaseHeavy(cv);
            
        } else {
            throw new ViolationException("Cannot construct a CodeViolationPropCECaseHeavy with violation ID of 0");
        }
        
        return cvpcdh;
        
    }
    
    

    /**
     * Uses date fields on the populated CodeViolation to determine a status
     * string and icon for UI Called by the integrator when creating a code
     * violation
     *
     * @param cv
     * @return the CodeViolation with correct icon and status
     * @throws com.tcvcog.tcvce.domain.IntegrationException
     */
    private CodeViolation violation_configureCodeViolation(CodeViolation cv) throws IntegrationException {
        SystemIntegrator si = getSystemIntegrator();
        CaseIntegrator ci = getCaseIntegrator();
        
        // TODO: NADGIT photo stuff
//        cv.setBlobList(ci.loadViolationPhotoList(cv));
        
        cv.setCitationIDList(ci.getCitations(cv.getViolationID()));
        cv.setNoticeIDList(ci.novGetNOVIDList(cv));
        
        // nullification overrides all
        if(cv.getNullifiedTS() != null){
                cv.setStatus(ViolationStatusEnum.NULLIFIED);
                cv.setIcon(si.getIcon(Integer.parseInt(getResourceBundle(Constants.VIOLATIONS_BUNDLE)
                        .getString(cv.getStatus().getIconPropertyName()))));
            
        } else if (cv.getActualComplianceDate() == null) {
            // violation still within compliance timeframe
            if (cv.getDaysUntilStipulatedComplianceDate() >= 0) {

                cv.setStatus(ViolationStatusEnum.UNRESOLVED_WITHINCOMPTIMEFRAME);
                cv.setIcon(si.getIcon(Integer.parseInt(getResourceBundle(Constants.VIOLATIONS_BUNDLE)
                        .getString(cv.getStatus().getIconPropertyName()))));

                // violation has NOT been cited, but is past compliance timeframe end date
            } else if (cv.getCitationIDList().isEmpty()) {

                cv.setStatus(ViolationStatusEnum.UNRESOLVED_EXPIREDCOMPLIANCETIMEFRAME);
                cv.setIcon(si.getIcon(Integer.parseInt(getResourceBundle(Constants.VIOLATIONS_BUNDLE)
                        .getString(cv.getStatus().getIconPropertyName()))));

                // violation has been cited on at least one citation
            } else {
                cv.setStatus(ViolationStatusEnum.UNRESOLVED_CITED);
                cv.setIcon(si.getIcon(Integer.parseInt(getResourceBundle(Constants.VIOLATIONS_BUNDLE)
                        .getString(cv.getStatus().getIconPropertyName()))));
            }
            // we have a resolved violation
        } else {
            cv.setStatus(ViolationStatusEnum.RESOLVED);
            cv.setIcon(si.getIcon(Integer.parseInt(getResourceBundle(Constants.VIOLATIONS_BUNDLE)
                    .getString(cv.getStatus().getIconPropertyName()))));
        }
        return cv;
    }

    /**
     * Internal logic container for checking status of BObs before allowing
     * updating of the underlying CodeViolation record
     *
     * @param cse
     * @param cv
     * @return
     * @throws ViolationException
     */
    private void violation_verifyCodeViolationAttributes(CECaseDataHeavy cse, CodeViolation cv) throws ViolationException {
        if(cse == null || cv == null) throw new ViolationException("Cannot verify code violation attributes with null case or violation");
        
        if (cse.getStatusBundle().getPhase() == CasePhaseEnum.Closed) {
            throw new ViolationException("Cannot update code violations on closed cases!");
        }
        if (cv.getStipulatedComplianceDate().isBefore(cv.getDateOfRecord())) {
            throw new ViolationException("Stipulated compliance date cannot be before the violation's date of record");
        }
        
        
    }
    
    /**
     * Logic holder for injecting a code element into a code violation and setting sensible default values
     * based on preferences by muni 
     * @param cse
     * @param cv
     * @param ece to be injected
     * @param mdh if not null, will be asked for its auto-config settings (e.g. don't set compliance dates on weekends).
     * @return
     * @throws BObStatusException 
     */
    public CodeViolation violation_injectOrdinance(CECase cse, CodeViolation cv, EnforcableCodeElement ece, MunicipalityDataHeavy mdh ) throws BObStatusException{

        if(cse != null && cv != null && ece != null){
            List<CodeViolation> vlst = new ArrayList<>();
            vlst.addAll(cse.getViolationList());
            if(!vlst.isEmpty()){
                // check to make sure that particular ordinance isn't already on the case
                for (CodeViolation tempCv : vlst) {
                    if(tempCv.getViolatedEnfElement().getCodeSetElementID() == ece.getCodeSetElementID()){
                        throw new BObStatusException("Duplicate Violation of ordiance with ID " + ece.getCodeSetElementID() + "Select a different ordinance, please!");
                    }
                }
            }
            int daysInFuture;
            if(ece.getNormDaysToComply() != 0){
                daysInFuture = ece.getNormDaysToComply();
            } else {
                daysInFuture = FALLBACK_DAYSTOCOMPLY;
            }
            cv.setViolatedEnfElement(ece);
            cv.setDescription(ece.getDefaultViolationDescription());
            cv.setStipulatedComplianceDate(LocalDateTime.now().plusDays(daysInFuture));
            cv.setDateOfRecord(LocalDateTime.now());
            cv.setPenalty(ece.getNormPenalty());
            
            
            
        } else {
            throw new BObStatusException("Cannot inject null ordinance or cannot inject into null code violation");
        }
        return cv;
    }

    /**
     * Access point for updating a CodeViolation record
     *
     * @param cse
     * @param cv
     * @param u
     * @throws ViolationException
     * @throws IntegrationException
     * @throws com.tcvcog.tcvce.domain.BObStatusException
     */
    public void violation_updateCodeViolation(CECaseDataHeavy cse,
            CodeViolation cv,
            UserAuthorized u)
            throws ViolationException,
            IntegrationException,
            BObStatusException {

        EventCoordinator ec = getEventCoordinator();
        CaseIntegrator ci = getCaseIntegrator();
        EventIntegrator ei = getEventIntegrator();

        if(cse == null || cv == null || u == null){
            throw new BObStatusException("Cannot update a code violation given a null case, violation, or user");
        }
        
        cv.setLastUpdatedUser(u);
        ci.updateCodeViolation(cv);

    } // close method

    /**
     * Updates only the notes field on violation. This method takes care of 
     * pulling out existing notes and prepending the new notes
     *
     * @param mbp
     * @param viol
     * @throws BObStatusException
     * @throws IntegrationException
     */
    public void violation_updateNotes(MessageBuilderParams mbp, CodeViolation viol) throws BObStatusException, IntegrationException {
        CaseIntegrator ci = getCaseIntegrator();
        SystemCoordinator sc = getSystemCoordinator();
        if (viol == null || mbp == null) {
            throw new BObStatusException("Cannot append if notes, case, or user are null");
        }

        viol.setNotes(sc.appendNoteBlock(mbp));

        ci.updateCodeViolationNotes(viol);
    }
    
    /**
     * Attempts to deactivate a code violation, but will thow an Exception if
     * the CodeViolation has been used in a notice or in a citation
     *
     * @param cv
     * @param ua
     * @throws com.tcvcog.tcvce.domain.BObStatusException
     * @throws com.tcvcog.tcvce.domain.IntegrationException
     */
    public void violation_deactivateCodeViolation(CodeViolation cv, UserAuthorized ua) throws BObStatusException, IntegrationException {
        CaseIntegrator ci = getCaseIntegrator();

        if (cv.getCitationIDList() != null && !cv.getCitationIDList().isEmpty()) {
            throw new BObStatusException("Cannot deactivate a violation that has been used in a citation");
        }

        if (cv.getNoticeIDList() != null && !cv.getNoticeIDList().isEmpty()) {
            throw new BObStatusException("Cannot deactivate a violation that has been used in a notice");
        }

        cv.setActive(false);
        ci.updateCodeViolation(cv);

    }
    /**
     * Attempts to deactivate a code violation, but will thow an Exception if
     * the CodeViolation has been used in a notice or in a citation
     *
     * @param cv
     * @param ua
     * @throws com.tcvcog.tcvce.domain.BObStatusException
     * @throws com.tcvcog.tcvce.domain.IntegrationException
     */
    public void violation_NullifyCodeViolation(CodeViolation cv, UserAuthorized ua) throws BObStatusException, IntegrationException {
        CaseIntegrator ci = getCaseIntegrator();
//
//        if (cv.getCitationIDList() != null && !cv.getCitationIDList().isEmpty()) {
//            throw new BObStatusException("Cannot deactivate a violation that has been used in a citation");
//        }
        // inject appropriate values into CV before updating
        if(cv != null && ua != null){
            // cannot nullify a violation for which compliance has been achieved
            if(cv.getActualComplianceDate() == null){
                cv.setNullifiedTS(LocalDateTime.now());
                cv.setNullifiedUser(ua);
                System.out.println("CaseCoordinator.violation_NullifyCodeViolation: ready to nullify in integrator");
                ci.updateCodeViolation(cv);
            } else {
                throw new BObStatusException("Cannot nullify a violation that has compliance marked");
                
            }
        } else {
            throw new BObStatusException("Cannot nullify a violation with null CV or User");
            
        }
        

    }
    
    /**
     * Wires up a given violation's findings to be the default findings for violations
     * of the given enforcable code element inside the CodeViolation passed in
     * TODO: Add option of checking that user's permissions and updating all those
     * muni's code sets as well
     * 
     * @param cv
     * @param ua 
     * @param cascadeToAuthCodebooks triggers the calling of an internal method for updating 
     * all code books that are default in any muni in which the UserAuthorized has 
     * enforcement official permissions (or better)
     * @throws com.tcvcog.tcvce.domain.BObStatusException 
     */
    public void violation_makeFindingsDefaultInCodebook(CodeViolation cv, UserAuthorized ua, boolean cascadeToAuthCodebooks) throws BObStatusException, IntegrationException{
       
       if(cv == null || ua == null) throw new BObStatusException("Cannot update default findings with null violation, codeset, or user");
       UserCoordinator uc = getUserCoordinator();
       MunicipalityCoordinator mc = getMuniCoordinator();
        
        
       violation_makeFindingsDefault(cv.getDescription(), cv.getViolatedEnfElement(), ua);
        
       
               
    } 

    /**
     * Internal operational code to determine which muni's the user has enforcement official
     * permissions (or better), and update their codebooks that contain this same
     * ordinance with default findings
     * @param cv
     * @param ua 
     */
    private void violation_makeFindingsDefault(String defFindings, EnforcableCodeElement ece, UserAuthorized ua) throws IntegrationException{

        CodeIntegrator ci = getCodeIntegrator();
        SystemCoordinator sc = getSystemCoordinator();
        ece.setDefaultViolationDescription(defFindings); 
        MessageBuilderParams mbp = new MessageBuilderParams();
        mbp.setUser(ua);
        mbp.setExistingContent(ece.getNotes());
        
        StringBuilder sb = new StringBuilder();
        sb.append("Default findings changed to: ");
        sb.append(defFindings);
        sb.append(" from: ");
        sb.append(ece.getDefaultViolationDescription());
        mbp.setNewMessageContent(sb.toString());
        
        ece.setNotes(sc.appendNoteBlock(mbp));
        ci.updateCodeElement(ece);
        
        
    }
    
    

    /**
     * Logic gateway for updates to a code violation's stipulated compliance date
     * @param cv
     * @param daysToExtend the number of days in the future FROM TODAY to extend the window
     * @param cse
     * @param ua 
     * @throws com.tcvcog.tcvce.domain.BObStatusException 
     * @throws com.tcvcog.tcvce.domain.ViolationException 
     * @throws com.tcvcog.tcvce.domain.IntegrationException 
     */
    public void violation_extendStipulatedComplianceDate(CodeViolation cv, long daysToExtend, CECaseDataHeavy cse, UserAuthorized ua) 
            throws BObStatusException, ViolationException, IntegrationException {

        if(cv == null || cse == null || ua == null){
            throw new BObStatusException("Cannot extend compliance date given a null violation, case, or user");
        }
        
        if(cv.getStipulatedComplianceDate() == null){
            throw new BObStatusException("Cannot extend a null stipulated compliance date");
        }
        
        if(daysToExtend == 0){
            throw new BObStatusException("I, the mighty CaseCoordinator, shall not extend the compliance window by 0 days");
        }
        
//        if(cv.isAllowStipCompDateUpdate()){
            LocalDateTime oldStipDate = cv.getStipulatedComplianceDate();
            cv.setStipulatedComplianceDate(LocalDateTime.now().plusDays(daysToExtend));
            violation_updateCodeViolation(cse, cv, ua);
            
            // now generate a note
            
            MessageBuilderParams mbp = new MessageBuilderParams();
            mbp.setUser(ua);
            mbp.setHeader("Stipulated compliance date extended");
            StringBuilder sb = new StringBuilder();
            sb.append("Previous stipulated compliance date of ");
            sb.append(DateTimeUtil.getPrettyDate(oldStipDate));
            sb.append(" has been changed to ");
            sb.append(DateTimeUtil.getPrettyDate(cv.getStipulatedComplianceDate()));
            sb.append(".");
            
            mbp.setNewMessageContent(sb.toString());
            violation_updateNotes(mbp, cv);
//            
//        } else {
//            throw new BObStatusException("Code violation status does not permit updates to compliance date");
//        }
    }
    
    /**
     * CodeViolation should have the actual compliance date set from the user's
     * event date of record
     *
     * @param cse
     * @param cv
     * @param u
     * @throws com.tcvcog.tcvce.domain.IntegrationException
     * @throws com.tcvcog.tcvce.domain.BObStatusException
     * @throws com.tcvcog.tcvce.domain.ViolationException
     * @throws com.tcvcog.tcvce.domain.EventException
     */
    public void violation_recordCompliance(CECaseDataHeavy cse, CodeViolation cv, UserAuthorized u) 
            throws IntegrationException, BObStatusException, ViolationException, EventException {

        if(cse == null || cv == null || u == null ) throw new BObStatusException("Cannot record compliance on null case, viol, or user");
        if(cv.getActualComplianceDate() == null){
            throw new ViolationException("Cannot set compliance with null actual compliance date");
        }
        
        CaseIntegrator ci = getCaseIntegrator();
        
        // update violation record for compliance
        cv.setComplianceUser(u);
        cv.setLastUpdatedUser(u);

        ci.updateCodeViolationCompliance(cv);
        violation_checkForFullComplianceAndCloseCaseIfTriggered(cse, u);
    }

    /**
     * 
     * @param cvIDList
     * @return
     * @throws IntegrationException
     */
    public List<CodeViolation> violation_getCodeViolations(List<Integer> cvIDList) throws IntegrationException{
        List<CodeViolation> vl = new ArrayList<>();
        
        if(cvIDList != null && !cvIDList.isEmpty()){
            for(Integer i: cvIDList){
                vl.add(violation_getCodeViolation(i));
            }
        }
        
        return vl;
        
    }
    
    /**
     * Utility method for grabbing a list of CodeViolations given a CECase
     * TODO: Currently causes infinite recursion
     * @param ceCase
     * @return
     * @throws IntegrationException
     * @throws com.tcvcog.tcvce.domain.BObStatusException
     */
    public List<CodeViolation> violation_getCodeViolations(CECaseDataHeavy ceCase) throws IntegrationException, BObStatusException {
        CaseIntegrator ci = getCaseIntegrator();
        if (ceCase == null){
            throw new BObStatusException("Cannot get violation list for a null case");
        }
        List<CodeViolation> vlist = violation_getCodeViolations(ceCase);
              
        return vlist;
    }

} // close class
