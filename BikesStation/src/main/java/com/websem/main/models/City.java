package com.websem.main.models;

import java.util.ArrayList;
import java.util.List;


public class City {
	private String IRI;
	private String name;
	private String dynamicLink;
	private LocalisationCity localisation;
	private List<BikeStation> bikesStations;

	
	
	public City(String name) {
		this.name = name;
		bikesStations = new ArrayList<BikeStation>();
	}
	
	public City(String name, LocalisationCity localisation) {
		this(name);
		this.setLocalisation(localisation);
	}

	public City(String name, List<BikeStation> bikesStations) {
		this(name);
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

	public LocalisationCity getLocalisation() {
		return localisation;
	}

	public void setLocalisation(LocalisationCity localisation) {
		this.localisation = localisation;
	}

	public String getIRI() {
		return IRI;
	}

	public void setIRI(String iRI) {
		IRI = iRI;
	}

	public String getDynamicLink() {
		return dynamicLink;
	}

	public void setDynamicLink(String dynamicLink) {
		this.dynamicLink = dynamicLink;
	}
}
