package com.websem.main.controllers;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.*;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.sparql.modify.request.UpdateDataInsert;
import org.apache.jena.sparql.modify.request.UpdateVisitor;
import org.apache.jena.sparql.util.NodeIsomorphismMap;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
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
import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.engine.PlanFactory;
import com.github.thesmartenergy.sparql.generate.jena.engine.RootPlan;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery;
import com.jayway.jsonpath.JsonPath;
import com.websem.main.models.BikeStation;
import com.websem.main.models.City;
import com.websem.main.models.HistoriqueStation;
import com.websem.main.models.JsonReader;
import com.websem.main.models.LocalisationCity;



@Controller
public class PagesController {
	public static final int ECART_UPDATE = 600;
	private static final int TAILLE_MAX_HISTORIQUE = 4;
	
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

		//TODO update dynamic with city name
		UpdateCity(getCityWithName(name, cities));

		return "pages/index";
	}

	public void UpdateCity(City city) throws JSONException, IOException {//TODO modifier BDD pour avoir lien vers les donnees dynamique
		RDFConnection conn;
		JSONObject json;
		long begin, end, total;

		List<List<String>> listeTerme = new ArrayList<List<String>>();
		HashMap<String, String> champsJson;

		ArrayList<Integer> informationsUpdate = new ArrayList<Integer>();

		String adresseDynamique = city.getDynamicLink();
		String name = city.getName();
	
		total = System.currentTimeMillis();
		
		// Recupere l'update le plus jeune / vieux et le nombre total d'update
		informationsUpdate = getLastsUpdate(name);
		
		// Recupere JSON sur site Dynamique
		begin = System.currentTimeMillis();
		json = JsonReader.readJsonFromUrl(adresseDynamique);
		end = System.currentTimeMillis();
		System.out.println("Get json Time : " + ((double) (end - begin) / 1000.0));

		// Creation Arbre pour analyser le Json
		ObjectMapper mapper = new ObjectMapper();
		JsonNode listStations = mapper.readTree(json.toString());

		// Recherche des termes du Json pour pouvoir faire le update 
		champsJson = researchsTermesBike(listeTerme, listStations);

		// informationsUpdate.add(listStations.findValue(champsJson.get("lastUpdate")).asInt());
		informationsUpdate.add((int) (System.currentTimeMillis()/1000));
		
		// Update des donnees , connection a la base
		conn = RDFConnectionFactory.connect("http://localhost:3030/Cities/update");
		
		// Suppression de l'update le plus ancien . limite de 10 pour les velo
		updateDelete(informationsUpdate,conn,name);
		
		// Insertion du nouvelle historique pour les velo
		begin = System.currentTimeMillis();
		updateInsert(informationsUpdate,conn,name,listStations ,champsJson);
		end = System.currentTimeMillis();
		System.out.println("Update Insert Time : " + ((double) (end - begin) / 1000.0));
		System.out.println("Total Time to Update : " + ((double) (end - total) / 1000.0));
			
		conn.close();
		
		//TODO ?? Selon les donnees (france , amerique) les noms differe , parser avec jsonObject
		//et Jackson trop specifique https://www.mkyong.com/java/jackson-convert-json-array-string-to-list/
	}

	private static ArrayList<Integer> getLastsUpdate(String name){
		RDFConnection conn;
		QueryExecution qExec;
		QuerySolution qs ;
		ResultSet rs;
		ArrayList<Integer> lastsUpdate = new ArrayList<Integer>();
		int i ,youngLastUpdate = 0,olderLastUpdate=0,nombreUpdate;

		conn = RDFConnectionFactory.connect("http://localhost:3030/Cities/query");

		// Recuperation adresse des donnees dynamique
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
				+ "}"
				+ "ORDER BY ?lastupdate") ;
		rs = qExec.execSelect();



		//Recuperation du plus vieux et plus jeune lastUpdate
		i=0;
		while(rs.hasNext()) {
			i++;

			qs = rs.next();

			int lu = qs.getLiteral("lastupdate").getInt();
			
			if (i == 1) {
				olderLastUpdate= lu;
			} else {
				youngLastUpdate = lu;
			}
		}

		nombreUpdate = i;

		qExec.close();
		conn.close();
		
		lastsUpdate.add(youngLastUpdate);
		lastsUpdate.add(olderLastUpdate);
		lastsUpdate.add(nombreUpdate);

		return lastsUpdate;
	
	}
	
	private void updateDelete(ArrayList<Integer> informationsUpdate,RDFConnection conn,String name) {
		int youngLastUpdate = informationsUpdate.get(0);
		int olderLastUpdate = informationsUpdate.get(1);
		int nombreUpdate = informationsUpdate.get(2);
		int lastUpdate = informationsUpdate.get(3);
		int updateDelete =0;

		if(nombreUpdate >TAILLE_MAX_HISTORIQUE) {
			if(lastUpdate - youngLastUpdate > ECART_UPDATE) {//Si superieur a 10 min on delete du plus vieux
				updateDelete = olderLastUpdate;
			}else {//Sinon delete du plus jeune
				updateDelete = youngLastUpdate;
			}

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
	}
	
	private void updateInsert(ArrayList<Integer> informationsUpdate, RDFConnection conn, String name, JsonNode listStations, HashMap<String, String> champsJson) {
		UpdateRequest up = new UpdateRequest();
		int lastUpdate = informationsUpdate.get(3);
		String path = champsJson.get("stationParentNode");
		
	 	listStations = listStations.findValue(path.substring(path.lastIndexOf('.') + 1));

		for (JsonNode jsonNode: listStations) {
			String idStation = jsonNode.findValue(champsJson.get("stationId")).asText();
			String bikeAvailable = jsonNode.findValue(champsJson.get("bikesAvailable")).asText();
			String slotAvailable = jsonNode.findValue(champsJson.get("docksAvailable")).asText();

			up.add("PREFIX ns0: <http://semanticweb.org/ontologies/City#> " +
					"PREFIX ns1: <http://www.w3.org/2003/01/geo/wgs84_pos> " +
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
		
		conn.update(up);
	}

	private static HashMap<String,String> researchsTermesBike(List<List<String>> listTermes,JsonNode treeJson) {
		//Ordre 
		HashMap<String, String> champsJson = new HashMap<String,String>();; //Champs , Champs trouver dans le Json
		InitialisationCheckValueBikeStation(listTermes);

		for (List<String> list : listTermes) {
			researchTermes(list, champsJson, treeJson);
		}

		return champsJson;
	}

	private static void researchTermes(List<String> list,HashMap<String,String> champsJson,JsonNode treeJson) {
		int i = 0;
		Boolean b = false;
		String champs = list.get(0), path;

		if(champs.equals("stationParentNode")) {
			for (String terme : list) {
	
				if(treeJson.findValue(terme) != null) {
					JsonNode node = treeJson.findValue(terme);
					String g = "";
	
					/*System.out.println("Find terme data  " + terme);
						System.out.println("Find terme data findpath " + treeJson.findPath(terme).asText());
						System.out.println("Find terme data path " + treeJson.path(terme).asText());
						System.out.println("Find terme data a " + treeJson.findValues(terme).toString());
						System.out.println("Find terme data  b" + treeJson.findValues(terme));
						System.out.println("Find terme data c" + treeJson.findPath(terme));
						System.out.println("Find terme data d" + treeJson.findParents(terme));*/
	
					//String value = JSONDataReader.getStringValue(jPath, jsonData);
					//champs = ;
	
					path = getPathListStation(treeJson, terme);
					System.out.println("Path : " + path);
					champsJson.put(champs, path);
					b = true;
					break;
				}
	
				i++;
			}
		} else {

			for (String terme : list) {
	
				if (treeJson.findValue(terme) != null) {
					System.out.println("Find terme " + terme);
					champsJson.put(champs, terme);
					b = true;
					break;
				}
				i++;
			}
	
			if (b == false) {
				champsJson.put(champs, champs);
			}
		}

	}
	
	private static String getPathListStation(JsonNode tree, String term) {
		Iterator<Entry<String, JsonNode>> ite = tree.fields();
		Entry<String, JsonNode> next;
		String test;
		
		while (ite.hasNext()) {
			next = ite.next();
			
			if (next.getKey().equals(term)) {
				System.out.println("Node : " + next.getKey() + "  " + term);
				return next.getKey();
			}

			test = getPathListStation(next.getValue(), term);
			if (test != null)
				return next.getKey() + "." + test;
		}
		
		return null;
	}

	@GetMapping("/newCity")
	public String newCity(ModelMap model) {
		// Envoyer vers la page newCity pour que l'utilisateur ajoute sa ville
		return "pages/newCity";
	}


	@RequestMapping(value="/newCity", method = RequestMethod.POST)
	public String newCity(@RequestParam(required = true) String nameCity, String staticLink, String dynamicLink, String wikidataCity, ModelMap model) throws JSONException, IOException {
    	URI staticL = null, dynamicL = null;
    	
    	try {
    		staticL = new URI(staticLink);
    		dynamicL = new URI(dynamicLink);
    	}catch (URISyntaxException e) {
    		System.out.println("URI incorrecte : " + staticLink + "  " + dynamicLink);
		}
    	
    	// on ne traite la demande que si on a une uri
    	if (staticL != null && dynamicL != null) {
			//List pour determiner quels sont les indices des differentes liste ci-dessous qui sont viable pour inserer une nouvelle ville
			List<Integer> indicesList  = new ArrayList<Integer>();
			List<List<String>> listTermes = new ArrayList<List<String>>();//ORDRE stationParentNode,stationId,stationName,stationLat,stationLon,stationCapacity);
			HashMap<String, String> champsJson;

			//Arbre Json du dynamique , sert a trouver les termes viable pour inserer une nouvelle ville
			JSONObject json = JsonReader.readJsonFromUrl(staticLink);
			//Creation Arbre pour analyser le Json
			ObjectMapper mapper = new ObjectMapper();
			JsonNode listStations = mapper.readTree(json.toString());


			//ORDRE [0]stationParentNode,[1]stationId,[2]stationName [3]StationLat [4]StationLon [5]StationCapacity [6]bikesAvailable [7]docksAvailable


			//Determiner les parametres
			champsJson = researchsTermesBike(listTermes,listStations);
			System.out.println("Id Lon : " + champsJson.get("stationLon"));
			/*System.out.println(champsJson.get("stationParentNode"));
			System.out.println(champsJson.get("stationId"));
			System.out.println(champsJson.get("stationName"));
			System.out.println(champsJson.get("stationLat"));
			System.out.println(champsJson.get("stationLon"));
			System.out.println(champsJson.get("stationCapacity"));*/

			// Parser le Json pour le transformer en RDF
			SPARQLGenerateQuery query = (SPARQLGenerateQuery) QueryFactory.create("PREFIX ite: <http://w3id.org/sparql-generate/iter/>"
					+" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
					+" PREFIX crm: <http://www.cidoc-crm.org/cidoc-crm/>"
					+" PREFIX exVoc: <http://www.ex-city.com/>"
					+" PREFIX ex: <http://example.org/>"
					+" PREFIX ns0: <http://semanticweb.org/ontologies/City#> "
					+" PREFIX ns1: <http://www.w3.org/2003/01/geo/wgs84_pos> "
					+" PREFIX iter: <http://w3id.org/sparql-generate/iter/>"
					+" PREFIX fun: <http://w3id.org/sparql-generate/fn/>"

	                    +" GENERATE { "
	                    +" <"+wikidataCity+"> a ns0:City;"
	                    +"  ns0:CityPublicTransport _:ns;"
	                    +"                ns0:CityName \""+nameCity+"\";"
	                    +"                ns0:LienDonneesDynamique <"+dynamicLink+">."
	                    +"  _:ns a ns0:CityBikeStation ;"
	                    +"   ns0:StationId ?stationId;"
	                    +"   ns0:Stationname ?name;"
	                    +"   ns0:StationLocalisation [ "
	                    +" ns1:lat ?lat;"
	                    +"  ns1:long ?lon;"
	                    +"   ] ;"
	                    +"  ns0:StationTotalcapacity ?capacity;"
	                    +"  ns0:StationHistorique []."
	                    +" }"
	                    +" SOURCE <"+staticLink+"> AS ?chemin"
	                    +" ITERATOR ite:JSONPath(?chemin, \"" + champsJson.get("stationParentNode") + ".*\") AS ?source"
	                    +" WHERE{"
	                    +" BIND(STR((fun:JSONPath(?source,\"." + champsJson.get("stationId") + "\"))) AS ?stationId)"
	                    +" BIND(STR((fun:JSONPath(?source,\"$."+ champsJson.get("stationName") + "\"))) AS ?name)"
	                    +" BIND(STR((fun:JSONPath(?source,\"."+ champsJson.get("stationLat") + "\"))) AS ?lat) "
	                    +"  BIND(STR((fun:JSONPath(?source,\"."+ champsJson.get("stationLon") + "\"))) AS ?lon)"
	                    +"  BIND(STR((fun:JSONPath(?source,\"."+ champsJson.get("stationCapacity") + "\"))) AS ?capacity) "
	                    +" }"
	                    , SPARQLGenerate.SYNTAX);

			RootPlan plan = PlanFactory.create(query);

			Model m = plan.exec();

			// Put le RDF obtenu dans fuseki
			RDFConnection conn = RDFConnectionFactory.connect("http://localhost:3030/Cities/");	 
			conn.load(m);
			conn.close();

			// TODO https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#updateLanguage
		}

		return "redirect:" + "/";
	}

	private static void InitialisationCheckValueBikeStation(List<List<String>> listTermes) { 
		//ORDRE [0]stationParentNode,[1]stationId,[2]stationName [3]StationLat [4]StationLon [5]StationCapacity [6]bikesAvailable [7]docksAvailable
		//Listes de termes a checker pour pouvoir construire le turtle
		List<String> stationParentNode = new ArrayList<String>(Arrays.asList("stationParentNode", "stations", "stationBeanList","properties")) ;
		List<String> stationId = new ArrayList<String>(Arrays.asList("stationId", "station_id", "id","number")) ;
		List<String> stationName = new ArrayList<String>(Arrays.asList("stationName","name","s")) ;
		List<String> stationLat = new ArrayList<String>(Arrays.asList("stationLat","lat","latitude","la")) ;
		List<String> stationLon = new ArrayList<String>(Arrays.asList("stationLon","lon","longitude","lg","long","lo")) ;
		List<String> stationCapacity = new ArrayList<String>(Arrays.asList("stationCapacity","capacity")) ;
		List<String> bikesAvailable = new ArrayList<String>(Arrays.asList("bikesAvailable","num_bikes_available","bikes_available","numBikesAvailable","ba","availableBikes")) ;
		List<String> docksAvailable = new ArrayList<String>(Arrays.asList("docksAvailable","num_docks_available","docks_available","numDocksAvailable","da","availableDocks")) ;
		List<String> lastUpdate = new ArrayList<String>(Arrays.asList("lastUpdate","last_updated","lastUpdate","lastUpdatedOther","lu"));
		//Liste de list de termes

		listTermes.add(stationParentNode);
		listTermes.add(stationId);
		listTermes.add(stationName);
		listTermes.add(stationLat);
		listTermes.add(stationLon);
		listTermes.add(stationCapacity);
		listTermes.add(bikesAvailable);
		listTermes.add(docksAvailable);
		listTermes.add(lastUpdate);
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
