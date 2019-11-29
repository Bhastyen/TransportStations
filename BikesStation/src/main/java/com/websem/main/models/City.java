package com.websem.main.models;

import java.util.ArrayList;
import java.util.List;


public class City {
	
	private String name;
	private List<BikeStation> bikesStations;

	public City(String name) {
		this.name = name;
		bikesStations = new ArrayList<BikeStation>();
	}

	public City(String name, List<BikeStation> bikesStations) {
		super();
		this.name = name;
		this.bikesStations = bikesStations;
	}
	
	public String getName() {
		return name;
	}
	
	public List<BikeStation> getBikesStations() {
		return bikesStations;
	}

	public void addBikeStation(BikeStation newStation) {
		if (bikesStations != null)
			bikesStations.add(newStation);
	}
}
