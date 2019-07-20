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
package com.tcvcog.tcvce.entities;


import com.tcvcog.tcvce.entities.occupancy.OccEvent;

/**
 *
 * @author sylvia
 */
public class EventRuleOccPeriod extends EventRuleImplementation{
    
    private int OccPeriodID;
    private OccEvent passedRuleEvent;
    
    /**
     *
     * @param impl
     */
    public EventRuleOccPeriod(EventRuleImplementation impl){
        super(impl);
        // implementation subclass
        this.attachedTS = impl.getAttachedTS();
        this.attachedBy = impl.getAttachedBy();
        this.lastEvaluatedTS = impl.getLastEvaluatedTS();
        this.passedRuleTS = impl.getPassedRuleTS();
    }

    /**
     * @return the OccPeriodID
     */
    public int getOccPeriodID() {
        return OccPeriodID;
    }

    /**
     * @return the passedRuleEvent
     */
    public OccEvent getPassedRuleEvent() {
        return passedRuleEvent;
    }

    /**
     * @param OccPeriodID the OccPeriodID to set
     */
    public void setOccPeriodID(int OccPeriodID) {
        this.OccPeriodID = OccPeriodID;
    }

    /**
     * @param passedRuleEvent the passedRuleEvent to set
     */
    public void setPassedRuleEvent(OccEvent passedRuleEvent) {
        this.passedRuleEvent = passedRuleEvent;
    }
    
    
    
    
}
