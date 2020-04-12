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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.tcvcog.tcvce.integration;

import com.tcvcog.tcvce.application.BackingBeanUtils;
import com.tcvcog.tcvce.coordinators.CaseCoordinator;
import com.tcvcog.tcvce.coordinators.SearchCoordinator;
import com.tcvcog.tcvce.domain.BObStatusException;
import com.tcvcog.tcvce.domain.IntegrationException;
import com.tcvcog.tcvce.entities.CECaseDataHeavy;
import com.tcvcog.tcvce.entities.CECase;
import com.tcvcog.tcvce.entities.search.SearchParamsCECase;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ellen bascomb of apt 31y
 */
public class CaseIntegrator extends BackingBeanUtils implements Serializable{
    
    final String ACTIVE_FIELD = "cecase.active";
    /**
     * Creates a new instance of CaseIntegrator
     */
    public CaseIntegrator() {
    }
   
    
    /**
     * Single focal point of serach method for Code Enforcement case using a SearchParam
     * subclass. Outsiders will use runQueryCECase or runQueryCECase
     * @param params
     * @return a list of CECase IDs
     * @throws IntegrationException
     * @throws BObStatusException 
     */
    public List<Integer> searchForCECases(SearchParamsCECase params) throws IntegrationException, BObStatusException{
        SearchCoordinator sc = getSearchCoordinator();
        List<Integer> cseidlst = new ArrayList<>();
        Connection con = getPostgresCon();
        ResultSet rs = null;
        PreparedStatement stmt = null;
        
        params.appendSQL("SELECT DISTINCT caseid \n");
        params.appendSQL("FROM public.cecase \n");
        params.appendSQL("INNER JOIN public.property ON (cecase.property_propertyid = property.propertyid) \n");
        params.appendSQL("WHERE caseid IS NOT NULL ");
        
        // *******************************
        // **         BOb ID            **
        // *******************************
         if (!params.isBobID_ctl()) {

            //*******************************
            // **   MUNI,DATES,USER,ACTIVE  **
            // *******************************
            params = (SearchParamsCECase) sc.assembleBObSearchSQL_muniDatesUserActive(
                                                                params, 
                                                                SearchParamsCECase.MUNI_DBFIELD,
                                                                ACTIVE_FIELD);
            
            // *******************************
            // **       1:OPEN/CLOSED       **
            // *******************************
            if (params.isCaseOpen_ctl()) {
                if(params.isCaseOpen_val()){
                    params.appendSQL("AND closingdate IS NULL ");
                } else {
                    params.appendSQL("AND closingdate IS NOT NULL ");
                }
            }
            
            // *******************************
            // **      2:PROPERTY           **
            // *******************************
             if (params.isProperty_ctl()) {
                if(params.getProperty_val()!= null){
                    params.appendSQL("AND property_propertyid=? ");
                } else {
                    params.setProperty_ctl(false);
                    params.appendToParamLog("PROPERTY: no Property object; prop filter disabled");
                }
            }
            
            // *******************************
            // **     3:PROPERTY UNIT       **
            // *******************************
             if (params.isPropertyUnit_ctl()) {
                if(params.getPropertyUnit_val()!= null){
                    params.appendSQL("AND propertyunit_unitid=? ");
                } else {
                    params.setPropertyUnit_ctl(false);
                    params.appendToParamLog("PROPERTY UNIT: no PropertyUnit object; propunit filter disabled");
                }
            }
            
            // *******************************
            // **    4:PROP INFO CASES      **
            // *******************************
            if (params.isPropInfoCase_ctl()) {
                if (params.isPropInfoCase_val()) {
                    params.appendSQL("AND propertyinfocase = TRUE ");
                } else {
                    params.appendSQL("AND propertyinfocase = FALSE ");
                }
            }
            
            // **********************************
            // ** 5. PERSONS INFO CASES BOOL   **
            // **********************************
            if (params.isPersonInfoCase_ctl()) {
                if (params.isPersonInfoCase_val()) {
                    params.appendSQL("AND personinfocase_personid IS NOT NULL ");
                } else {
                    params.appendSQL("AND personinfocase_personid IS NULL ");
                }
            }
            
            // ********************************
            // ** 6.PERSONS INFO CASES ID    **
            // ********************************
            if (params.isPersonInfoCaseID_ctl()) {
                if(params.getPersonInfoCaseID_val() != null){
                    params.appendSQL("AND personinfocase_personid=? ");
                } else {
                    params.setPersonInfoCaseID_ctl(false);
                    params.appendToParamLog("PERSONINFO: no Person object; Person Info case filter disabled");
                }
            }
            
            // *******************************
            // **     7.BOb SOURCE          **
            // *******************************
             if (params.isSource_ctl()) {
                if(params.getSource_val() != null){
                    params.appendSQL("AND bobsource_sourceid=? ");
                } else {
                    params.setSource_ctl(false);
                    params.appendToParamLog("SOURCE: no BOb source object; source filter disabled");
                }
            }
           
            // *******************************
            // **        9. PACC            **
            // *******************************
             if (params.isPacc_ctl()) {
                if(params.isPacc_val()){
                    params.appendSQL("AND paccenabled = TRUE ");
                } else {
                    params.appendSQL("AND paccenabled = TRUE ");
                }
            }
            
            
        } else {
            params.appendSQL("caseid = ? "); // will be param 1 with ID search
        }

        int paramCounter = 0;
            
        try {
            stmt = con.prepareStatement(params.extractRawSQL());

            if (!params.isBobID_ctl()) {
                if (params.isMuni_ctl()) {
                     stmt.setInt(++paramCounter, params.getMuni_val().getMuniCode());
                }
                
                if(params.isDate_startEnd_ctl()){
                    stmt.setTimestamp(++paramCounter, params.getDateStart_val_sql());
                    stmt.setTimestamp(++paramCounter, params.getDateEnd_val_sql());
                 }
                
                if (params.isUser_ctl()) {
                   stmt.setInt(++paramCounter, params.getUser_val().getUserID());
                }
                
                if (params.isProperty_ctl()) {
                    stmt.setInt(++paramCounter, params.getProperty_val().getPropertyID());
                }
                
                if (params.isPropertyUnit_ctl()) {
                    stmt.setInt(++paramCounter, params.getPropertyUnit_val().getUnitID());
                }
                
                 if (params.isPersonInfoCaseID_ctl()) {
                    stmt.setInt(++paramCounter, params.getPersonInfoCaseID_val().getPersonID());
                }
                 
                if(params.isSource_ctl()){
                    stmt.setInt(++paramCounter, params.getSource_val().getSourceid());
                }

            } else {
                stmt.setInt(++paramCounter, params.getBobID_val());
            }
            
            rs = stmt.executeQuery();

            int counter = 0;
            int maxResults;
            if (params.isLimitResultCount_ctl()) {
                maxResults = params.getLimitResultCount_val();
            } else {
                maxResults = Integer.MAX_VALUE;
            }
            while (rs.next() && counter < maxResults) {
                cseidlst.add(rs.getInt("caseid"));
                counter++;
            }
            
        } catch (SQLException ex) {
            System.out.println(ex.toString());
            throw new IntegrationException("Cannot search for code enf cases, sorry!", ex);
            
        } finally{
             if (con != null) { try { con.close(); } catch (SQLException e) { /* ignored */} }
             if (stmt != null) { try { stmt.close(); } catch (SQLException e) { /* ignored */} }
             if (rs != null) { try { rs.close(); } catch (SQLException ex) { /* ignored */ } }
        } // close finally
        
        return cseidlst;
        
    }
    
    
    /**
     * Generates a CECaseDataHeavy without the big, fat lists
     * @param ceCaseID
     * @return
     * @throws IntegrationException 
     * @throws com.tcvcog.tcvce.domain.BObStatusException 
     */
    public CECase getCECase(int ceCaseID) throws IntegrationException, BObStatusException{
        String query = "SELECT caseid, cecasepubliccc, property_propertyid, propertyunit_unitid, \n" +
            "            login_userid, casename, casephase, originationdate, closingdate, \n" +
            "            creationtimestamp, notes, paccenabled, allowuplinkaccess, active \n" +
            "  FROM public.cecase WHERE caseid = ?;";
        ResultSet rs = null;
        CaseCoordinator cc = getCaseCoordinator();
        PreparedStatement stmt = null;
        Connection con = null;
        CECase c = null;
        
        try {
            
            con = getPostgresCon();
            stmt = con.prepareStatement(query);
            stmt.setInt(1, ceCaseID);
            //System.out.println("CaseIntegrator.getCECase| sql: " + stmt.toString());
            rs = stmt.executeQuery();
            
            while(rs.next()){
                c = generateCECase(rs);
            }
            
        } catch (SQLException ex) {
            System.out.println(ex.toString());
            throw new IntegrationException("Cannot get cecase by id", ex);
            
        } finally{
             if (con != null) { try { con.close(); } catch (SQLException e) { /* ignored */} }
             if (stmt != null) { try { stmt.close(); } catch (SQLException e) { /* ignored */} }
             if (rs != null) { try { rs.close(); } catch (SQLException ex) { /* ignored */ } }
        } // close finally
        
        return c;
    }
    
    
  
    
  
    
    /**
     * Internal populator for CECase objects
     * @param rs
     * @return
     * @throws SQLException
     * @throws IntegrationException 
     */
     private CECase generateCECase(ResultSet rs) throws SQLException, IntegrationException{
        UserIntegrator ui = getUserIntegrator();
        
        CECase cse = new CECase();

        cse.setCaseID(rs.getInt("caseid"));
        cse.setPublicControlCode(rs.getInt("cecasepubliccc"));

        cse.setPropertyID(rs.getInt("property_propertyid"));
        cse.setPropertyUnitID(rs.getInt("propertyunit_unitid"));
        
        cse.setCaseManager(ui.getUser(rs.getInt("login_userid")));

        cse.setCaseName(rs.getString("casename"));
        
        cse.setOriginationDate(rs.getTimestamp("originationdate")
                .toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());

        if(rs.getTimestamp("closingdate") != null){
            cse.setClosingDate(rs.getTimestamp("closingdate")
                    .toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        
        cse.setNotes(rs.getString("notes"));
        
        if(rs.getTimestamp("creationtimestamp") != null){
            cse.setCreationTimestamp(rs.getTimestamp("creationtimestamp")
                    .toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }

        cse.setPaccEnabled(rs.getBoolean("paccenabled"));
        cse.setAllowForwardLinkedPublicAccess(rs.getBoolean("allowuplinkaccess"));
        cse.setActive(rs.getBoolean("active"));
        

        return cse;
    }
    
     /**
      * First gen search method to be deprecated in Beta
      * @param pacc
      * @return
      * @throws IntegrationException
      * @throws BObStatusException 
      */
    public List<CECase> getCECasesByPACC(int pacc) throws IntegrationException, BObStatusException{
        
        ArrayList<CECase> caseList = new ArrayList();
        String query = "SELECT caseid FROM public.cecase WHERE cecasepubliccc = ?;";
        Connection con = getPostgresCon();
        ResultSet rs = null;
        PreparedStatement stmt = null;
        
        try {
            
            stmt = con.prepareStatement(query);
            stmt.setInt(1, pacc);
            System.out.println("CaseIntegrator.getCECasesByPacc | sql: " + stmt.toString());
            rs = stmt.executeQuery();
            
            while(rs.next()){
                caseList.add(getCECase(rs.getInt("caseid")));
                
            }
            
        } catch (SQLException ex) {
            System.out.println(ex.toString());
            throw new IntegrationException("Cannot search for cases by PACC, sorry", ex);
            
        } finally{
             if (con != null) { try { con.close(); } catch (SQLException e) { /* ignored */} }
             if (stmt != null) { try { stmt.close(); } catch (SQLException e) { /* ignored */} }
             if (rs != null) { try { rs.close(); } catch (SQLException ex) { /* ignored */ } }
        } // close finally
        
        return caseList;
    }
    
  
    /**
     * Insertion point for CECaseDataHeavy objects; must be called by Coordinator who checks 
 logic before sending to the DB. This method only copies from the passed in CECaseDataHeavy
 into the SQL INSERT
     * 
     * @param ceCase
     * @return
     * @throws IntegrationException
     * @throws BObStatusException 
     */
    public int insertNewCECase(CECase ceCase) throws IntegrationException, BObStatusException{
        CaseCoordinator cc = getCaseCoordinator();
        
        String query = "INSERT INTO public.cecase(\n" +
                        "            caseid, cecasepubliccc, property_propertyid, propertyunit_unitid, \n" +
                        "            login_userid, casename, originationdate, closingdate, \n" +
                        "            notes, creationTimestamp, active) \n" +
                        "    VALUES (DEFAULT, ?, ?, ?, \n" +
                        "            ?, ?, ?, ?, \n" +
                        "            ?, now(), ?);";
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int insertedCaseID = 0;
        CECase freshlyInsertedCase = null;
        Connection con = null;
        
        try {
            
            con = getPostgresCon();
            stmt = con.prepareStatement(query);
            stmt.setInt(1, ceCase.getPublicControlCode());
            stmt.setInt(2, ceCase.getPropertyID());
            if(ceCase.getPropertyUnitID() != 0) {
                stmt.setInt(3, ceCase.getPropertyUnitID());
            } else { stmt.setNull(3, java.sql.Types.NULL); }
            
            stmt.setInt(4, ceCase.getCaseManager().getUserID());
            stmt.setString(5, ceCase.getCaseName());
            stmt.setTimestamp(6, java.sql.Timestamp
                    .valueOf(ceCase.getOriginationDate()));
            // closing date
            stmt.setNull(7, java.sql.Types.NULL); 
            
            stmt.setString(8, ceCase.getNotes());
            stmt.setBoolean(9, ceCase.isActive());
            
            System.out.println("CaseIntegrator.insertNewCase| sql: " + stmt.toString());
            
            stmt.execute();
            
            String retrievalQuery = "SELECT currval('cecase_caseID_seq');";
            stmt = con.prepareStatement(retrievalQuery);
            rs = stmt.executeQuery();
            
            while(rs.next()){
                insertedCaseID = rs.getInt(1);
            }
            
        } catch (SQLException ex) {
            System.out.println(ex.toString());
            throw new IntegrationException("Case Integrator: cannot insert new case, sorry", ex);
            
        } finally{
             if (con != null) { try { con.close(); } catch (SQLException e) { /* ignored */} }
             if (stmt != null) { try { stmt.close(); } catch (SQLException e) { /* ignored */} }
             if (rs != null) { try { rs.close(); } catch (SQLException ex) { /* ignored */ } }
        } // close finally
        
        return insertedCaseID;
        
    }
    
    /**
     * Updates the values in the CECaseDataHeavy in the DB but does NOT
 edit the data in connected tables, namely CodeViolation, EventCnF, and Person
 Use calls to other add methods in this class for adding additional
 violations, events, and people to a CE case.
     * 
     * @param ceCase the case to updated, with updated member variables
     * @throws com.tcvcog.tcvce.domain.IntegrationException
     */
    public void updateCECaseMetadata(CECaseDataHeavy ceCase) throws IntegrationException{
        String query =  "UPDATE public.cecase\n" +
                        "   SET cecasepubliccc=?, \n" +
                        "       casename=?, originationdate=?, closingdate=?, notes=?, \n" +
                        " paccenabled=?, allowuplinkaccess=?, active=? " +
                        " WHERE caseid=?;";
        PreparedStatement stmt = null;
        Connection con = null;
        
        try {
            
            con = getPostgresCon();
            stmt = con.prepareStatement(query);
            stmt.setInt(1, ceCase.getPublicControlCode());
            stmt.setString(2, ceCase.getCaseName());
            stmt.setTimestamp(3, java.sql.Timestamp
                    .valueOf(ceCase.getOriginationDate()));
            if(ceCase.getClosingDate() != null){
                stmt.setTimestamp(4, java.sql.Timestamp
                        .valueOf(ceCase.getClosingDate()));
                
            } else {
                stmt.setNull(4, java.sql.Types.NULL);
            }
            stmt.setString(5, ceCase.getNotes());
            stmt.setBoolean(6, ceCase.isPaccEnabled());
            stmt.setBoolean(7, ceCase.isAllowForwardLinkedPublicAccess());
            stmt.setBoolean(8, ceCase.isActive());
            
            stmt.setInt(9, ceCase.getCaseID());
            stmt.execute();
            
            
        } catch (SQLException ex) {
            System.out.println(ex.toString());
            throw new IntegrationException("Cannot udpate case due to a database storage issue", ex);
            
        } finally{
             if (con != null) { try { con.close(); } catch (SQLException e) { /* ignored */} }
             if (stmt != null) { try { stmt.close(); } catch (SQLException e) { /* ignored */} }
        } // close finally
        
    }
    
    public void deleteCECase(CECaseDataHeavy cecase){
        
        
    }
    
  
    
    public List<Integer> getCECaseHistoryList(int userID) 
            throws IntegrationException, BObStatusException{
        List<Integer> cseidl = new ArrayList<>();
        Connection con = getPostgresCon();
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String s = "SELECT cecase_caseid, entrytimestamp FROM loginobjecthistory "
                    + "WHERE login_userid = ? "
                    + "AND cecase_caseid IS NOT NULL "
                    + "ORDER BY entrytimestamp DESC;";
            stmt = con.prepareStatement(s);
            stmt.setInt(1, userID);

            rs = stmt.executeQuery();
            
            while (rs.next()) {
                cseidl.add(rs.getInt("cecase_caseid"));
            }

        } catch (SQLException ex) {
            System.out.println(ex.toString());
            throw new IntegrationException("Unable to generate case history list", ex);
        } finally {
            if (con != null) { try { con.close(); } catch (SQLException e) { /* ignored */} }
            if (stmt != null) { try { stmt.close(); } catch (SQLException e) { /* ignored */} }
            if (rs != null) { try { rs.close(); } catch (SQLException ex) { /* ignored */ } }
        } // close finally
        
        return cseidl;
    }
     
}