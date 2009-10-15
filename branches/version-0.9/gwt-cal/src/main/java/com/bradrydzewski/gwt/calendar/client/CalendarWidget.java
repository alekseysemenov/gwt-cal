/*
 * This file is part of gwt-cal
 * Copyright (C) 2009  Scottsdale Software LLC
 * 
 * gwt-cal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/
 */

package com.bradrydzewski.gwt.calendar.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.bradrydzewski.gwt.calendar.client.event.DeleteEvent;
import com.bradrydzewski.gwt.calendar.client.event.DeleteHandler;
import com.bradrydzewski.gwt.calendar.client.event.HasDeleteHandlers;
import com.bradrydzewski.gwt.calendar.client.util.AppointmentUtil;
import com.google.gwt.event.logical.shared.HasOpenHandlers;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;

/**
 * TODO: Need Calendar "View" - CHECK
 * TODO: Need CalendarSettings
 * TODO: Need LayoutStrategy - CHECK
 * TODO: Need DragDropStrategy
 * TODO: Need ResizeStrategy ??? or is this same as DragDrop
 * TODO: Add AppointmentBuilder ??? downside is that if the Appointment object is updated, need to refersh widget
 * @author Brad Rydzewski
 *
 */
public class CalendarWidget extends InteractiveWidget implements
	HasSelectionHandlers<Appointment>, HasDeleteHandlers<Appointment>,
	HasOpenHandlers<Appointment> {

	/**
	 * Represents the currently selected appointment. Is set to null when no
	 * appointment is selected.
	 */
	protected Appointment selectedAppointment = null;
	
	/**
	 * List of appointments.
	 */
	private ArrayList<Appointment> appointments 
					= new ArrayList<Appointment>();
	
	/**
	 * Set to <code>true</code> if the calendar layout is suspended
	 * and cannot be triggered.
	 */
	private boolean layoutSuspended = false;

	/**
	 * Set to <code>true</code> if the calendar is pending the layout
	 * of its appointments.
	 */
	private boolean layoutPending = false;

	/**
	 * Set to <code>true</code> if the list of appointments needs to
	 * be sorted.
	 */
	private boolean sortPending = true;

	/**
	 * The date currently displayed by the calendar.
	 * Set to Today by default.
	 */
    private Date date;

    /**
     * Calendar settings, set to default.
     */
    private CalendarSettings settings = CalendarSettings.DEFAULT_SETTINGS;

    protected CalendarView view = null;
    

    public CalendarWidget() {
    	super();
    	date = new Date();
    	AppointmentUtil.resetTime(date);
    	//setStyleName("gwt-cal");
    }

    public Date getDate() {
        return (Date)date.clone();
    }

    public void setDate(Date date, int days) {
        
        AppointmentUtil.resetTime(date);
        this.date = date;
        view.setDays(days);
        refresh();
    }

    public void setDate(Date date) {
        setDate(date, getDays());
    }

    public int getDays() {
        return view==null?3:view.getDays();
    }

    public void setDays(int days) {
        view.setDays(days);
        refresh();
    }
    
    public List<Appointment> getAppointments() {
    	return appointments;
    }

    /**
     * Removes an appointment from the calendar.
     * @param appointment the item to be removed.
     */
    public void removeAppointment(Appointment appointment) {
        removeAppointment(appointment, false);
    }

    /**
     * Removes an appointment from the calendar.
     * @param appointment the item to be removed.
     * @param fireEvents <code>true</code> to allow deletion events to be fired
     */
    public void removeAppointment(Appointment appointment, boolean fireEvents) {

        boolean commitChange = true;

        if (fireEvents) {
            commitChange = DeleteEvent.fire(this, getSelectedAppointment());
        }
        if (commitChange) {
            appointments.remove(appointment);
            //multiDayAppointments.remove(appointment);
            selectedAppointment = null;
            sortPending = true;
            refresh();
        }
    }

    /**
     * Adds an appointment to the calendar.
     * @param appointment item to be added
     */
    public void addAppointment(Appointment appointment) {
        appointments.add(appointment);
        
        /* this is what i need to do, but calcuation doens't work yet */
        /* so for now developer will need to manually set flag */
//        if(AppointmentUtil.isMultiDay(appointment)) {
//            appointment.setMultiDay(true);
//            multiDayAppointments.add(appointment);
//        }
//        if(appointment.isMultiDay())
//            multiDayAppointments.add(appointment);

        sortPending = true;

        refresh();
    }

    /**
     * Adds each appointment in the list to the calendar.
     * @param appointments items to be added.
     */
    public void addAppointments(ArrayList<Appointment> appointments) {
        for (Appointment appointment : appointments) {
            addAppointment(appointment);
        }
    }

    /**
     * Clears all appointment items.
     */
    public void clearAppointments() {
        appointments.clear();
        //multiDayAppointments.clear();
        refresh();
    }

    /**
     * Sets the currently selected item.
     * @param appointment the item to be selected,
     * 			or <code>null</code> to de-select all items.
     */
    public void setSelectedAppointment(Appointment appointment) {
    	view.setSelectedAppointment(appointment);
    	//selectedAppointment = appointment;
    }

    /**
     * Gets the currently selected item.
     * @return the selected item.
     */
    public Appointment getSelectedAppointment() {
    	return selectedAppointment;
    }

    /**
     * Performs all layout calculations for the list of
     * appointments and resizes the Calendar View appropriately.
     */
    protected void refresh() {
        if (layoutSuspended) {
            layoutPending = true;
            return;
        }
        
        if (sortPending) {
            Collections.sort(appointments);
            //Collections.sort(multiDayAppointments);
            sortPending = false;
        }
        
        //PERFORM APPOINTMENT LAYOUT NOW
        //view.setDateAndDays(date, days);
        doSizing();
        doLayout();
        

    }
    
    public void doLayout() {
    	view.doLayout();
    }
    public void doSizing() {
    	view.doSizing();
    }
    
    public void onLoad() {
        DeferredCommand.addCommand(new Command() {

            //@Override
            public void execute() {
                //if (GWT.isScript()) {
            	doSizing();
                //}
            }
        });
    	
    }

    /**
     * Suspends the calendar from performing a layout. This can be
     * useful when adding a large number of appointments at a time,
     * since a layout is performed each time an appointment is added.
     */
    public void suspendLayout() {
        layoutSuspended = true;
    }

    /**
     * Allows the calendar to perform a layout, sizing the component
     * and placing all appointments. If a layout is pending it will
     * get executed when this method is called.
     */
    public void resumeLayout() {
        layoutSuspended = false;
        if (layoutPending) {
            refresh();
        }
    }


	public CalendarSettings getSettings() {
		return this.settings;
	}

	public void setSettings(CalendarSettings settings) {
		this.settings = settings;
	}


	public boolean selectPreviousAppointment() {
	      if (getSelectedAppointment() == null) {
	            return false;
	        }
	        int index = getAppointments().indexOf(getSelectedAppointment());
	        if (index <= 0) {
	            return false;
	        }
	        Appointment appt = getAppointments().get(index - 1);

	        if(appt==null) {
	        	return false;
	        }
	        
	        setSelectedAppointment(appt);
	        return true;
	}
	
    public boolean selectNextAppointment() {

        if (getSelectedAppointment() == null) {
            return false;
        }

        int index = getAppointments().indexOf(
        		getSelectedAppointment());

        if (index >= getAppointments().size()) {
            return false;
        }
        
        Appointment appt = getAppointments().get(index + 1);
        
        if(appt==null) {
        	return false;
        }
        
        setSelectedAppointment(appt);
        
        return true;
    }

	@Override
	public void onDeleteKeyPressed() {
		view.onDeleteKeyPressed();		
	}

	@Override
	public void onDoubleClick(Element element) {
		view.onDoubleClick(element);
	}

	@Override
	public void onDownArrowKeyPressed() {
		view.onDownArrowKeyPressed();
	}

	@Override
	public void onLeftArrowKeyPressed() {
		view.onLeftArrowKeyPressed();
	}

	@Override
	public void onMouseDown(Element element) {
		view.onMouseDown(element);
	}

	@Override
	public void onRightArrowKeyPressed() {
		view.onRightArrowKeyPressed();
	}

	@Override
	public void onUpArrowKeyPressed() {
		view.onUpArrowKeyPressed();
	}

	public void fireOpenEvent(Appointment appointment) {
		OpenEvent.fire(this, appointment);
	}

	public void fireDeleteEvent(Appointment appointment) {
		DeleteEvent.fire(this, appointment);
	}

	public void fireSelectionEvent(Appointment appointment) {
		SelectionEvent.fire(this, appointment);
	}

	public HandlerRegistration addSelectionHandler(
			SelectionHandler<Appointment> handler) {

		return addHandler(handler, SelectionEvent.getType());
	}

	public HandlerRegistration addDeleteHandler(
			DeleteHandler<Appointment> handler) {
		
		return addHandler(handler, DeleteEvent.getType());
	}

	public HandlerRegistration addOpenHandler(
			OpenHandler<Appointment> handler) {
		
		return addHandler(handler, OpenEvent.getType());
	}
}