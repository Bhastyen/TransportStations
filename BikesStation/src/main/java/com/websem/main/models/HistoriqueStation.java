package com.websem.main.models;

import java.util.Date;



public class HistoriqueStation {
	private long date;
	private int bikeAvailable;
	private int slotAvailable;

	public HistoriqueStation(){

	}
	
	public HistoriqueStation(long date, int bikeAvailable, int slotAvailable) {
		super();
		this.date = date;
		this.bikeAvailable = bikeAvailable;
		this.slotAvailable = slotAvailable;
	}
	
	public String getDate() {
		Date d = new Date(date);
		return d.toString();
	}
	
	public int getBikeAvailable() {
		return bikeAvailable;
	}
	
	public int getSlotAvailable() {
		return slotAvailable;
	}

}
