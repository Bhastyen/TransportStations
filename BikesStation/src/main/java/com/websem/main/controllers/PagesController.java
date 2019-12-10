package com.websem.main.controllers;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.*;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.sparql.modify.request.UpdateDataInsert;
import org.apache.jena.sparql.modify.request.UpdateVisitor;
import org.apache.jena.sparql.util.NodeIsomorphismMap;
import org.apache.jena.system.Txn;
import org.apache.jena.update.Update;
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
    public static final int ECART_UPDATE = 600;
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
		City cityChoose = cityStation(name);
		
    	modelMap.put("Cities", listCity());
    	modelMap.put("CityChoose", cityChoose);

    	// donne la derniere localisation visitee
    	if (lastCity != null && !lastCity.isEmpty()) {
    		modelMap.put("lastLocCity", getCityWithName(lastCity, cities).getLocalisation());
    	}
    	
        // update dynamic with city name
    	UpdateCity(cityChoose);

        return "pages/index";
    }

	public void UpdateCity(City city) throws JSONException, IOException {//TODO modifier BDD pour avoir lien vers les donnees dynamique
		RDFConnection conn;
		QueryExecution qExec;
		ResultSet rs;
		UpdateRequest up = new UpdateRequest();
		long begin, end; double total = 0;

		List<String> bikesAvailable = new ArrayList<String>(Arrays.asList("num_bikes_available")) ;
		List<String> docksAvailable = new ArrayList<String>(Arrays.asList("num_docks_available")) ;
		List<String> stationParentNode =new ArrayList<String>(Arrays.asList("stations")) ;
		List<Integer> indiceArrayList = new ArrayList<>(); //List utiliser apres la recherche pour determiner quels termes sont dans le json pour la recherche en update
		
		int i = 0;
		int lastUpdate;
		String adresseDynamique = city.getDynamicLink();
		String name = city.getName();
		int youngLastUpdate = 0,olderLastUpdate = 0;
		int nombreUpdate ,updateDelete;

		begin = System.currentTimeMillis();
		//TODO
		// Recupere JSON sur site Dynamique
		conn = RDFConnectionFactory.connect("http://localhost:3030/Cities/query");
		end = System.currentTimeMillis();
		total += ((double) (end - begin) / 1000.0);
		
		System.out.println("Connexion Time : " + ((double) (end - begin) / 1000.0));

		// Recuperation adresse des donnees dynamique
		begin = System.currentTimeMillis();
		qExec = conn.query("PREFIX ns0: <http://semanticweb.org/ontologies/City#> "+
		"SELECT DISTINCT ?lastupdate {"+
				"?s ns0:CityName ?name ;" + 
		"           ns0:CityPublicTransport ?n." + 
		"        ?n a ns0:CityBikeStation; " + 
		"             ns0:StationId ?id ;" + 
		"             ns0:StationHistorique ?hs." +
		 "       ?hs ns0:StationState[" +       				
		       				"  ns0:Date ?lastupdate]" +
		"        FILTER (str(?name) = \""+name+"\" )"
				+ "} "
				+ "ORDER BY ?lastupdate");
		rs = qExec.execSelect();

		end = System.currentTimeMillis();
		total += ((double) (end - begin) / 1000.0);
		System.out.println("Query Select Time : " + ((double) (end - begin) / 1000.0));
		
	
		// Recuperation du plus vieux et plus jeune lastUpdate
		i = 0;
		while(rs.hasNext()) {
			QuerySolution qs = rs.next();
			
			i++;
			int lu = qs.getLiteral("lastupdate").getInt();
			
			if (i == 1) {
				olderLastUpdate= lu;
			} else {
				youngLastUpdate = lu;
			}
		}
		
		nombreUpdate = i;
		
		//System.out.println("nombreup " + nombreUpdate);

		qExec.close();
		conn.close();

		begin = System.currentTimeMillis();
		JSONObject json = JsonReader.readJsonFromUrl(adresseDynamique);

		end = System.currentTimeMillis();
		total += ((double) (end - begin) / 1000.0);
		System.out.println("Get json Time : " + ((double) (end - begin) / 1000.0));

		// Verification de la date du dernier update pour voir quand cela a ete changer

		// Creation Arbre pour analyser le Json
		ObjectMapper mapper = new ObjectMapper();
		JsonNode listStations = mapper.readTree(json.toString());

		lastUpdate = listStations.findValue("last_updated").intValue();

		//Update des donnees , connection a la base
		conn = RDFConnectionFactory.connect("http://localhost:3030/Cities/update");

		begin = System.currentTimeMillis();
		//Suppression de l'update le plus ancien . limite de 10
		if(nombreUpdate >= 5) {
			//System.out.println("youngLastUpdate"+ youngLastUpdate + "olderLastUpdate" + olderLastUpdate);
			//System.out.println(lastUpdate+ " - "+ youngLastUpdate + " ="+ (lastUpdate - youngLastUpdate));
			if(lastUpdate - youngLastUpdate > ECART_UPDATE) {//Si superieur a 10 min on delete du plus vieux
				updateDelete = olderLastUpdate;
			}else {//Sinon delete du plus jeune
				updateDelete = youngLastUpdate;
			}
			
			//System.out.println("DELETE " + updateDelete);
			conn.update( "PREFIX ns0: <http://semanticweb.org/ontologies/City#> "
				+"PREFIX ns1: <http://www.w3.org/2003/01/geo/wgs84_pos>"

				+"	DELETE { "
				+"	  		          ?hs ns0:StationState ?s."
				+"	  				         ?s ns0:BikeAvailable ?ba;"
				+"	                            ns0:SlotAvailable ?sa;"
				+"	                            ns0:Date ?ls"
										
				+"	}WHERE{"
				+"	 			?adresse ns0:CityName ?name;"
				+"	                     ns0:CityPublicTransport ?n."
				+"	            ?n a ns0:CityBikeStation ."
				+"	  			?n ns0:StationHistorique ?hs."
				+"	            ?hs ns0:StationState ?s."
				+"	  				         ?s ns0:BikeAvailable ?ba;"
				+"	                            ns0:SlotAvailable ?sa;"
				+"	                            ns0:Date ?ls"	
				+"             FILTER(str(?name) = \""+name+"\")"
				+"	           FILTER(?ls = "+updateDelete+")}"
					);
		}
		end = System.currentTimeMillis();
		total += ((double) (end - begin) / 1000.0);
		System.out.println("Delete old or young update Time : " + ((double) (end - begin) / 1000.0));


		begin = System.currentTimeMillis();
		//Recherche des termes du Json pour pouvoir faire le update
		researchTermes(bikesAvailable,indiceArrayList,listStations);//indice trouver stocker dans indiceArrayList 0
		researchTermes(docksAvailable,indiceArrayList,listStations);//indice trouver stocker dans indiceArrayList 1
		researchTermes(stationParentNode,indiceArrayList,listStations);//indice trouver stocker dans indiceArrayList 2
		end = System.currentTimeMillis();
		total += ((double) (end - begin) / 1000.0);
		System.out.println("Get Reserch Term Time : " + ((double) (end - begin) / 1000.0));

		
		listStations = listStations.findValue(stationParentNode.get(indiceArrayList.get(2)));
		for (JsonNode jsonNode: listStations) {
			String idStation = jsonNode.findValue("station_id").asText();
			String bikeAvailable = jsonNode.findValue(bikesAvailable.get(indiceArrayList.get(0))).asText();
			String slotAvailable = jsonNode.findValue(docksAvailable.get(indiceArrayList.get(1))).asText();

			up.add("PREFIX ns0: <http://semanticweb.org/ontologies/City#> " +
					"PREFIX ns1: <http://www.w3.org/2003/01/geo/wgs84_pos> " +
					"\r\n" +
					"INSERT { " +
					"  		  " +
					"    			  ?hs ns0:StationState[" +
					"  				  ns0:BikeAvailable " + bikeAvailable + "; "+
					"                  ns0:SlotAvailable " + slotAvailable + ";"+
					"                  ns0:Date "+ lastUpdate +
					"            " +
					"					]." +
					"}WHERE{" +
					" 			 _:n ns0:CityName ?name ;" +
					"                ns0:CityPublicTransport ?n." +
					"            ?n a ns0:CityBikeStation; " +
					"                 ns0:StationId ?id ;" +
					"                 ns0:StationHistorique ?hs.          " +
					"FILTER (str(?name) = \"" + name + "\" )" +
					"FILTER (str(?id) = \"" + idStation + "\" )"
					+ "}");
		}

		begin = System.currentTimeMillis();
		conn.update(up);
		end = System.currentTimeMillis();
		total += ((double) (end - begin) / 1000.0);
		System.out.println(name + " Insertion Time : " + ((double) (end - begin) / 1000.0));
		System.out.println("Update Total Time : " + total);

		conn.close();
		//TODO ?? Selon les donnees (france , amerique) les noms differe , parser avec jsonObject
		//et Jackson trop specifiaue https://www.mkyong.com/java/jackson-convert-json-array-string-to-list/
		//Chercher par  termes qui peuvent correspondre a nos besoins (?)
		//Creer dictionnaire exemple : name = stationName
		//Trouver moyen de stocker en liste les Json
		//Chercher par le nom defini et faire la suite avec





		//Boucle sur le JSON prie , iterator chaque noeud ; delete / insert ???
		//TODO
		//Pour chaque station
		//Lastupdate du JSON SELECT -> Not exist (raw == 0 ) SELECT Historistation
		//DELETE pour StationId BikeAvaiable / SlotAvaiable
		//INSERT pour StationId BikeAvaiable / SlotAvaiable
		//Select pour idStation/HistoriqueStation all : StateStation -> Trier par ordre croissant LIMIT 1 -> HISTODEL
		//DELETE HISTODEL

		// conn.update(request);

	}

	private static void researchTermes(List<String> list,List<Integer>indices,JsonNode treeJson) {
		int i=0;
		for (String terme : list) {

			if(treeJson.findValue(terme)!=null) {
				indices.add(i);
				break;
			}
			i++;
		}

	}

	 @GetMapping("/newCity")
	    public String newCity(ModelMap model) {
	        // Envoyer vers la page newCity pour que l'utilisateur ajoute sa ville
	        return "pages/newCity";
	    }
	    
	    
	    @RequestMapping(value="/newCity", method = RequestMethod.POST)
	    public String newCity(@RequestParam String staticLink, @RequestParam String dynamicLink, ModelMap model) {
	    	URI staticL = null, dynamicL = null;
	    	
	    	try {
	    		staticL = new URI(staticLink);
	    		dynamicL = new URI(dynamicLink);
	    	}catch (URISyntaxException e) {
	    		System.out.println("URI incorrecte : " + staticLink + "  " + dynamicLink);
			}
	    	
	    	// on ne traite la demande que si on a une uri
	    	if (staticL != null && dynamicL != null) {
		        // Recuperer le Json
		        String jsonPath = "https://saint-etienne-gbfs.klervi.net/gbfs/en/station_information.json";
	
		        // Parser le Json pour le transformer en RDF
	
		        // Put le RDF obtenu dans fuseki
 		        // TODO https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#updateLanguage
		        model.put("message", "You are in new page !!");
	    	}
	    	
	        // Appeler la page test pour afficher les donner entree
	        return "redirect:" + "/";
	    }

    private static List<City> listCity(){
        List<City> listCity = new ArrayList<City>();
        City city;
        
        RDFConnection conn = RDFConnectionFactory.connect("http://localhost:3030/Cities/query");
        QueryExecution qExec = conn.query("PREFIX ns0: <http://semanticweb.org/ontologies/City#> "
        								+ "SELECT DISTINCT ?link ?iri ?name {"
        									+ " ?iri ns0:CityName ?name; "
        										 + " ns0:LienDonneesDynamique ?link"
        								+ "}");
        ResultSet rs = qExec.execSelect();
        
        // Recuperation des noms des villes pour afficher la liste
        while(rs.hasNext()) {
            QuerySolution qs = rs.next();
            Literal name = qs.getLiteral("name");
            
            city = new City(name.toString(), new LocalisationCity(0,  0));
            city.setIRI(qs.getResource("iri").getURI());
            city.setDynamicLink(qs.getResource("link").getURI());
            listCity.add(city);
        }
        
        qExec.close();
        conn.close();

	    // recupere la position de chaque ville
        for (int i = 0; i < listCity.size(); i++) {
        	city = listCity.get(i);
        	city.setLocalisation(getLocalisation(city.getName()));
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
        		+ "SELECT ?City ?name ?s ?link ?stationId ?lat ?lon ?capacity "
        		+ "WHERE { ?s a ns0:City ; "
        			+ " ns0:CityName ?nameCity ; "
	        		+ " ns0:LienDonneesDynamique ?link ; "
	        		+ " ns0:CityPublicTransport _:ns. "
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
            BikeStation bikeStation = new BikeStation(qs.getLiteral("stationId").getString(), 
            		                                  qs.getLiteral("capacity").getString(),
            		                                  qs.getLiteral("name").getString(),
            		                                  localisationCity,
            		                                  historiqueStationList);
            city.addBikeStation(bikeStation);
    	    city.setIRI(qs.getResource("s").getURI());
    	    city.setDynamicLink(qs.getResource("link").getURI());
    	    
            /*System.out.println(qs.getLiteral("name"));
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
        		+ "FILTER (str(?n) = \"" + name + "\" )} ");
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
