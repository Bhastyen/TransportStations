package com.websem.main.models;

public class LocalisationCity {

	private float lat;
	private float lg;
	//private String adresse;
	//private String numRue;
	
	
	public LocalisationCity(float lat, float lg) {
		super();
		this.lat = lat;
		this.lg = lg;

	}

	public float getLat() {
		return lat;
	}

	public float getLg() {
		return lg;
	}


}
