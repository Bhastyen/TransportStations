package com.websem.main.models;

import java.util.List;



public class BikeStation {
	private String idStation;
	private String capacity;
	private LocalisationCity localisation;
	private String name;
	private HistoriqueStation stateStation;
	private List<HistoriqueStation> listHistoriqueStation;
	
	
	public BikeStation(String idStation, String capacity, String name, LocalisationCity localisation,
			List<HistoriqueStation> listHistoriqueStation) {
		super();
		this.idStation = idStation;
		this.capacity = capacity;
		this.localisation = localisation;
		this.name = name;
		this.listHistoriqueStation = listHistoriqueStation;
	}
	
	public String getIdStation() {
		return idStation;
	}
	
	public String getCapacity() {
		return capacity;
	}
	
	public LocalisationCity getLocalisation() {
		return localisation;
	}
	
	public String getName() {
		return name;
	}
	
	public List<HistoriqueStation> getListHistoriqueStation() {
		return listHistoriqueStation;
	}
	
}
