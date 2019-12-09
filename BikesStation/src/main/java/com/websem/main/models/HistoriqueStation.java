package com.websem.main.models;


public class HistoriqueStation {
	private String date; //TODO mettre en format date ?
	private int bikeAvailable;
	private int slotAvailable;

	public HistoriqueStation(){

	}
	
	public HistoriqueStation(String date, int bikeAvailable, int slotAvailable) {
		super();
		this.date = date;
		this.bikeAvailable = bikeAvailable;
		this.slotAvailable = slotAvailable;
	}
	
	public String getDate() {
		return date;
	}
	
	public int getBikeAvailable() {
		return bikeAvailable;
	}
	
	public int getSlotAvailable() {
		return slotAvailable;
	}

}
