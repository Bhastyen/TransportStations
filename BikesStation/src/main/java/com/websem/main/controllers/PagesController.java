package com.websem.main.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.jena.query.Dataset ;
import org.apache.jena.query.DatasetFactory ;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.websem.main.models.BikeStation;
import com.websem.main.models.City;
import com.websem.main.models.HistoriqueStation;
import com.websem.main.models.JsonReader;
import com.websem.main.models.LocalisationCity;



@Controller
public class PagesController {
	
	@GetMapping("/")
	public String home(@RequestParam(required = false) String city, ModelMap modelMap) {
		List<City> cities = listCity();
		modelMap.put("Cities", cities);

    	// donne la derniere localisation visitee
    	if (city != null && !city.isEmpty()) {
    		modelMap.put("lastLocCity", getCityWithName(city, cities).getLocalisation());
    	}
    	
    	return "pages/index";
	}
	

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public String promptStationCity(@RequestParam("city") String name, @RequestParam(required=false) String lastCity, ModelMap modelMap) throws JSONException, IOException{
		List<City> cities = listCity();
    	modelMap.put("Cities", listCity());
    	modelMap.put("CityChoose", cityStation(name));

    	// donne la derniere localisation visitee
    	if (lastCity != null && !lastCity.isEmpty()) {
    		modelMap.put("lastLocCity", getCityWithName(lastCity, cities).getLocalisation());
    	}
    	
        //TODO update dynamic with city name
    	UpdateCity(name);

        return "pages/index";
    }

    public void UpdateCity(String name) throws JSONException, IOException {//TODO modifier BDD pour avoir lien vers les donnees dynamique
    	 RDFConnection conn = RDFConnectionFactory.connect("http://localhost:3030/Cities/update");
    	 
    	 //TODO
    	 //Recupere JSON sur site Dynamique
    	 //System.out.println("PageController-UpdateCity");
    	 JSONObject json = JsonReader.readJsonFromUrl("https://saint-etienne-gbfs.klervi.net/gbfs/en/station_status.json");
    	 //System.out.println(json.toString());
    	 
    	 //Creation Arbre pour analyser le Json
    	 ObjectMapper mapper = new ObjectMapper();
    	 JsonNode listStations = mapper.readTree(json.toString());
    	// listStations.
    	 
    	 //TODO ?? Selon les donnees (france , amerique) les noms differe , parser avec jsonObject
    	  //et Jackson trop specifiaue https://www.mkyong.com/java/jackson-convert-json-array-string-to-list/
    	 //Chercher par  termes qui peuvent correspondre a nos besoins (?)
    	 //Creer dictionnaire exemple : name = stationName
    	 //Trouver moyen de stocker en liste les Json
    	 //Chercher par le nom defini et faire la suite avec
    	 
    	 
    	
    	 /*try {
             JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(result)));
         } catch (FileNotFoundException e) {
             e.printStackTrace();
         }
         JsonParser jsonParser = new JsonParser();
         JsonArray userArray = jsonParser.parse(result).getAsJsonArray();
         for (JsonElement aUser : userArray) {
             Log.i( "Json2", aUser.toString());
             for (Map.Entry<String, JsonElement> valueEntry : aUser.getAsJsonObject().entrySet()) {
                 Log.i( "Json3", valueEntry.getKey().toString() + " " + valueEntry.getValue().toString());
             }
  
  
             Log.i( "Json", "------");
         }*/


    	 //Boucle sur le JSON prie , iterator chaque noeud ; delete / insert ???
    	 //TODO
    	 //Pour chaque station
    	 //Lastupdate du JSON SELECT -> Not exist (raw == 0 ) SELECT Historistation 
    	 //DELETE pour StationId BikeAvaiable / SlotAvaiable
    	 //INSERT pour StationId BikeAvaiable / SlotAvaiable
    	 //Select pour idStation/HistoriqueStation all : StateStation -> Trier par ordre croissant LIMIT 1 -> HISTODEL
    	 //DELETE HISTODEL
    	 
    	// conn.update(request);
    	 conn.close();
    }
	
    @GetMapping("/newCity")
    public String newCity(ModelMap model) {
        // Envoyer vers la page newCity pour que l'utilisateur ajoute sa ville
        return "pages/newCity";
    }
    
    
    @RequestMapping(value="/newCity", method = RequestMethod.POST)
    public String newCity(@RequestParam(required = true) String staticLink, ModelMap model) {
        // Recuperer le Json
        String jsonPath = "https://saint-etienne-gbfs.klervi.net/gbfs/en/station_information.json";

        // Parser le Json pour le transformer en RDF

        // Put le RDF obtenu dans fuseki
       // TODO https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#updateLanguage
        model.put("message", "You are in new page !!");

        // Appeler la page test pour afficher les donner entree
        return "redirect:" + "/";
    }
	

    private static List<City> listCity(){
        List<City> listCity = new ArrayList<City>();
        
        RDFConnection conn = RDFConnectionFactory.connect("http://localhost:3030/Cities/query");
        QueryExecution qExec = conn.query("PREFIX ns0: <http://semanticweb.org/ontologies/City#> PREFIX ns1: <http://geo.> SELECT DISTINCT ?n { ?s ns0:CityName ?n; }") ;
        ResultSet rs = qExec.execSelect();
        
        // Recuperation des noms des villes pour afficher la liste
        while(rs.hasNext()) {
            QuerySolution qs = rs.next();

            Literal objet = qs.getLiteral("n");
            listCity.add(new City(objet.toString(), new LocalisationCity(0,  0)));
        }
        
        qExec.close();
        conn.close();

	    // recupere la position de chaque ville
        for (int i = 0; i < listCity.size(); i++) {
        	listCity.get(i).setLocalisation(getLocalisation(listCity.get(i).getName()));
        }
        
        return  listCity;
    }
    

    private static City cityStation(String nameCity){
        City city = new City(nameCity, new LocalisationCity(0,  0));
        float avgLat = 0; float avgLon = 0;
        int nbStation = 0;
        //System.out.println("name City " + nameCity);
        
        RDFConnection conn = RDFConnectionFactory.connect("http://localhost:3030/Cities/query");
        QueryExecution qExec = conn.query("PREFIX ns0: <http://semanticweb.org/ontologies/City#> PREFIX ns1: <http://www.w3.org/2003/01/geo/wgs84_pos> "
        		+ "SELECT ?City ?name ?stationId ?lat ?lon ?capacity "
        		+ "WHERE { ?s a ns0:City; "
	        		+ "ns0:CityPublicTransport _:ns; "
	        		+ "ns0:CityName ?nameCity ."
	        		+ " _:ns a ns0:CityBikeStation; "
	        		+ "ns0:StationId ?stationId; "
	        		+ "ns0:Stationname ?name; "
	        		+ "ns0:StationLocalisation [ns1:lat ?lat; ns1:long ?lon;] ;"
	        		+ "ns0:StationTotalcapacity ?capacity. "
        		+ "FILTER (str(?nameCity) = \"" + nameCity + "\" ) }");
        ResultSet rs = qExec.execSelect();

        //Recuperation des noms des villes pour afficher la liste
        while(rs.hasNext()) {
            QuerySolution qs = rs.next();
            LocalisationCity localisationCity = new LocalisationCity(qs.getLiteral("lat").getFloat(),qs.getLiteral("lon").getFloat());
            List<HistoriqueStation> historiqueStationList = new ArrayList<HistoriqueStation>();
            HistoriqueStation historiqueStation = new HistoriqueStation();
            BikeStation bikeStation = new BikeStation(qs.getLiteral("stationId").getString(),qs.getLiteral("capacity").getString(),qs.getLiteral("name").getString(),localisationCity,historiqueStationList);
            city.addBikeStation(bikeStation);
            
            /*System.out.println("Nouvelle ligne requete ");

            System.out.println(qs.getLiteral("name"));
            System.out.println(qs.getLiteral("stationId"));
            System.out.println(qs.getLiteral("lat"));
            System.out.println(qs.getLiteral("lon"));*/
        }
        
        qExec.close();
	    conn.close();
	    
	    // recupere la position de la ville
    	city.setLocalisation(getLocalisation(city.getName()));
        
        return city;
    }
    
    
    private static LocalisationCity getLocalisation(String name) {
        float avgLat = 0; float avgLon = 0;
        int nbStation = 0;
        
        RDFConnection conn = RDFConnectionFactory.connect("http://localhost:3030/Cities/query");
        QueryExecution qExec = conn.query("PREFIX ns0: <http://semanticweb.org/ontologies/City#> PREFIX ns1: <http://www.w3.org/2003/01/geo/wgs84_pos> "
        		+ "SELECT ?n ?lon ?lat { ?v ns0:CityName ?n; ns0:CityPublicTransport _:ns. "
        		+ "_:ns a ns0:CityBikeStation; ns0:StationLocalisation [ns1:lat ?lat; ns1:long ?lon;]."
        		+ "FILTER (str(?n) = \"" + name + "\" ) } ");
        ResultSet rs = qExec.execSelect();
	
	    // Recuperation des localisations des stations
	    while(rs.hasNext()) {
	        QuerySolution qs = rs.next();
	
	        String n = qs.getLiteral("n").getString();
	        float lon = qs.getLiteral("lon").getFloat();
	        float lat = qs.getLiteral("lat").getFloat();
	        
	        // j'ajoute la nouvelle latitude
	        avgLat += lat;
	        
	        // j'ajoute la nouvelle longitude
	        avgLon += lon;
	        
	        // j'augmente le nombre de stations
	        nbStation += 1;
	    }
	    
	    qExec.close();
	    conn.close();
	    
	    // renvoie la localisation
    	return new LocalisationCity(avgLat / nbStation, avgLon / nbStation);
    }
    
    private static City getCityWithName(String name, List<City> cities) {
    	for (City c : cities) {
    		if (c.getName().equals(name)) {
        		return c;
    		}
    	}
    	
    	return null;
    } 
}
