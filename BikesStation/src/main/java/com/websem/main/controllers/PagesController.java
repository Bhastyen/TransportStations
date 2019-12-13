package com.websem.main.controllers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
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
import org.json.XML;
import org.apache.jena.query.Dataset ;
import org.apache.jena.query.DatasetFactory ;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import com.fasterxml.jackson.dataformat.xml.*;;

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
	public String promptStationCity(@RequestParam("city") String name, @RequestParam(required=false) String lastCity, ModelMap modelMap){
		boolean ok;
		List<City> cities = listCity();
		City cityChoose = cityStation(name), last;

		// verifie si la ville a ete trouve dans city station
		if (cityChoose == null) {
			modelMap.put("error","Error : Stations not found for the city of : \" "+name+ " \" in database.");
			return "pages/error";
		}

		// update dynamic avec la ville choisi
		ok = UpdateCity(cityChoose);
		if (!ok)  // si echec on envoie une erreur
		{
			modelMap.put("error","Error : Update of data for the city of : \" \"+name+ \" \" failed.");
			return "pages/error";
		}


		// envoie donnees au client
		modelMap.put("Cities", listCity());
		modelMap.put("CityChoose", cityChoose);

		// donne la derniere localisation visitee
		if (lastCity != null && !lastCity.isEmpty()) {
			last = getCityWithName(lastCity, cities);

			// verifie si la ville a ete trouve
			if (last == null)
				return "pages/error";

			modelMap.put("lastLocCity", last.getLocalisation());
		}

		return "pages/index";
	}

	@GetMapping("/newCity")
	public String newCity(ModelMap model) {
		// Envoyer vers la page newCity pour que l'utilisateur ajoute sa ville
		return "pages/newCity";
	}


	@RequestMapping(value="/newCity", method = RequestMethod.POST)
	public String newCity(@RequestParam(required = true) String nameCity, String staticLink, String dynamicLink, String wikidataCity, ModelMap model,String formatFichier){
		URL staticL = null, dynamicL = null;

		try {
			staticL = new URL(staticLink);
			dynamicL = new URL(dynamicLink);
		}catch (MalformedURLException e) {
			System.out.println("URI incorrecte : " + staticLink + "  " + dynamicLink);
		}

		// on ne traite la demande que si on a une uri
		if (staticL == null || dynamicL == null) {
			model.put("error", "Error : one of these urls is not correct.");
			return "pages/newCity";
		}

		// List pour determiner quels sont les indices des differentes liste ci-dessous qui sont viable pour inserer une nouvelle ville
		List<Integer> indicesList  = new ArrayList<Integer>();
		List<List<String>> listTermes = new ArrayList<List<String>>(); // ORDRE stationParentNode,stationId,stationName,stationLat,stationLon,stationCapacity);
		HashMap<String, String> champsJson;
		boolean ok;

		JSONObject json = null;
		ObjectMapper mapper = null;
		JsonNode listStations = null;

		try {
			if(formatFichier.equals("XML")) {
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document doc = db.parse(new URL(staticLink).openStream());
				/*System.out.println("doc "+ doc.toString());
				XmlMapper xmlMapper = new XmlMapper();
				JsonNode jsonNode = xmlMapper.readTree(doc.toString().getBytes());
				System.out.println("jsonNOde "+ jsonNode.toString());
				ObjectMapper jsonMapper = new ObjectMapper();
				String jsonString = jsonMapper.writeValueAsString(jsonNode);*/

			    json = XML.toJSONObject(doc.toString());
			    //System.out.println(jsonString);
			}else if(formatFichier.equals("JSON")){
				// Arbre Json du dynamique , sert a trouver les termes viable pour inserer une nouvelle ville
				json = JsonReader.readJsonFromUrl(staticLink);
				
				
			}
			//Creation Arbre pour analyser le Json
			mapper = new ObjectMapper();
			listStations = mapper.readTree(json.toString());
		} catch (JSONException | IOException  | SAXException | ParserConfigurationException e ) {
			System.err.println("Probleme lors de la lecture du json " + e.getMessage());
		
		} 

		if (json == null || mapper == null || listStations == null) {
			model.put("error", "Error : the file of static link is not a json.");
			return "pages/newCity";
		}

		// ORDRE [0]stationParentNode,[1]stationId,[2]stationName [3]StationLat [4]StationLon [5]StationCapacity [6]bikesAvailable [7]docksAvailable
		// Determiner les parametres
		champsJson = researchsTermesBike(listTermes, listStations, true);  // create the base for this city so staticLink = true
		if (champsJson == null) {
			model.put("error", "Error : the format of json file is not compatible.");
			return "pages/newCity";
		}

		/*System.out.println(champsJson.get("stationParentNode"));
		System.out.println(champsJson.get("stationId"));
		System.out.println(champsJson.get("stationName"));
		System.out.println(champsJson.get("stationLat"));
		System.out.println(champsJson.get("stationLon"));
		System.out.println(champsJson.get("stationCapacity"));*/

		System.out.println("Name " + champsJson.get("stationName"));

		// Parser le Json pour le transformer en RDF
		SPARQLGenerateQuery query = (SPARQLGenerateQuery) QueryFactory.create("PREFIX ite: <http://w3id.org/sparql-generate/iter/> "
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
                    +" SOURCE <" + staticLink + "> AS ?chemin"
                    +" ITERATOR ite:JSONPath(?chemin, \"" + champsJson.get("stationParentNode") + ".*\") AS ?source"
                    +" WHERE{"
                    +"    BIND(STR(fun:JSONPath(?source,\"." + champsJson.get("stationId") + "\")) AS ?stationId)"
                    +"    BIND(STR(fun:JSONPath(?source,\"$." + champsJson.get("stationName") + "\")) AS ?name)"
                    +"    BIND(STR((fun:JSONPath(?source,\"."+ champsJson.get("stationLat") + "\"))) AS ?lat) "
                    +"    BIND(STR((fun:JSONPath(?source,\"."+ champsJson.get("stationLon") + "\"))) AS ?lon)"
                    +"    BIND(STR((fun:JSONPath(?source,\"."+ champsJson.get("stationCapacity") + "\"))) AS ?capacity) "
                    +" }"
                    , SPARQLGenerate.SYNTAX);
		 //BIND(STR(REPLACE(fun:JSONPath(?source,\"$." + champsJson.get("stationName") + "\"),\"[^A-Za-z0-9.]\",\"test\")) AS ?name)

		RootPlan plan = PlanFactory.create(query);

		Model m = plan.exec();

		// met le RDF obtenu dans fuseki
		RDFConnection conn = RDFConnectionFactory.connect("http://localhost:3030/Cities/");

		conn.load(m);

		conn.close();

		// update une premiere fois
		ok = UpdateCity(getCityWithName(nameCity, listCity()));
		if (!ok) {
			model.put("error", "Error : update of the informations for the city of " + nameCity + " failed.");
			return "pages/newCity";
		}

		// TODO https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#updateLanguage
		return "redirect:" + "/";
	}

	public boolean UpdateCity(City city){
		RDFConnection conn;
		JSONObject json = null;
		ObjectMapper mapper = null;
		JsonNode listStations = null;
		String adresseDynamique, name;
		long begin, end, total;
		boolean ok = true;

		List<List<String>> listeTerme = new ArrayList<List<String>>();
		HashMap<String, String> champsJson;

		ArrayList<Integer> informationsUpdate = new ArrayList<Integer>();

		// verifier l'adresse dynamique avant de chercher le fichier
		if (city.getDynamicLink() == null || city.getDynamicLink().isEmpty())
			return false;

		adresseDynamique = city.getDynamicLink();
		name = city.getName();

		total = System.currentTimeMillis();

		// Recupere l'update le plus jeune / vieux et le nombre total d'update
		informationsUpdate = getLastsUpdate(name);

		begin = System.currentTimeMillis();
		try {
			
			URLConnection conn1 = new URL(adresseDynamique).openConnection();
			String type = conn1.getContentType();
			if(type.contains("xml")) {
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document doc = db.parse(new URL(adresseDynamique).openStream());
				
			    json = XML.toJSONObject(doc.toString());
			}else {
				System.out.println("type fichier " + type);
				// Recupere JSON sur site Dynamique
				json = JsonReader.readJsonFromUrl(adresseDynamique);
			}
			// Creation Arbre pour analyser le Json
			mapper = new ObjectMapper();
			listStations = mapper.readTree(json.toString());
		} catch (JSONException | IOException  | SAXException | ParserConfigurationException e) {
			System.err.println("Probleme lors de la lecture du json " + e.getMessage());
		} 

		end = System.currentTimeMillis();
		System.out.println("Get json Time : " + ((double) (end - begin) / 1000.0));

		// verifie si le json a ete lu ou non
		if (json == null || mapper == null || listStations == null)
			return false;

		// Recherche des termes du Json pour pouvoir faire le update
		champsJson = researchsTermesBike(listeTerme, listStations, false);  // update dynamic so staticLink = false

		if (champsJson == null)  // si pas de termes trouves pour parser on annule le update et on previent
			return false;

		// informationsUpdate.add(listStations.findValue(champsJson.get("lastUpdate")).asInt());
		informationsUpdate.add((int) (System.currentTimeMillis()/1000));

		// Update des donnees , connection a la base
		conn = RDFConnectionFactory.connect("http://localhost:3030/Cities/update");

		// Suppression de l'update le plus ancien . limite de 10 pour les velo
		ok = updateDelete(informationsUpdate, conn, name);
		if (!ok)   // si probleme lors de la suppression on avertit
			return false;

		// Insertion du nouvelle historique pour les velos
		begin = System.currentTimeMillis();
		ok = updateInsert(informationsUpdate, conn, name, listStations ,champsJson);
		if (!ok)   // si probleme lors de la mise a jour on avertit
			return false;
		end = System.currentTimeMillis();

		// pour connaitre le temps d'un update : peut etre long suivant la ville
		System.out.println("Update Insert Time : " + ((double) (end - begin) / 1000.0));
		System.out.println("Total Time to Update : " + ((double) (end - total) / 1000.0));

		conn.close();

		return true;

		//TODO ?? Selon les donnees (france , amerique) les noms differe , parser avec jsonObject
		//et Jackson trop specifique https://www.mkyong.com/java/jackson-convert-json-array-string-to-list/
	}

	private static ArrayList<Integer> getLastsUpdate(String name){
		RDFConnection conn;
		QueryExecution qExec;
		QuerySolution qs ;
		ResultSet rs;
		ArrayList<Integer> lastsUpdate = new ArrayList<Integer>();
		int i, youngLastUpdate = 0, olderLastUpdate = 0, nombreUpdate;

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
		i = 0;
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

	private boolean updateDelete(ArrayList<Integer> informationsUpdate,RDFConnection conn,String name) {
		int youngLastUpdate = informationsUpdate.get(0);
		int olderLastUpdate = informationsUpdate.get(1);
		int nombreUpdate = informationsUpdate.get(2);
		int lastUpdate = informationsUpdate.get(3);
		int updateDelete = 0;

		if(nombreUpdate > TAILLE_MAX_HISTORIQUE) {
			if(lastUpdate - youngLastUpdate > ECART_UPDATE) {// Si superieur a 'Ecart Update' seconde on supprime le plus vieux
				updateDelete = olderLastUpdate;
			}else { // Sinon on supprime le plus jeune
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
				+"             FILTER(str(?name) = \"" + name + "\")"
				+"	           FILTER(?ls = " + updateDelete + ")}"
					);
		}

		return true;
	}

	// recupere les valeurs dans le json et les insert dans la base de données
	private boolean updateInsert(ArrayList<Integer> informationsUpdate, RDFConnection conn, String name, JsonNode listStations, HashMap<String, String> champsJson) {
		int lastUpdate = informationsUpdate.get(3);
		UpdateRequest up = new UpdateRequest();
		String path = champsJson.get("stationParentNode");
		String id = champsJson.get("stationId");
		String bike_available = champsJson.get("bikesAvailable");
		String dock_available = champsJson.get("docksAvailable");
		String bikeAvailable, slotAvailable, idStation;

		//System.out.println("Id : " + id + " Bike : " + bike_available + "  " + dock_available);

		listStations = listStations.findValue(path.substring(path.lastIndexOf('.') + 1));

		for (JsonNode jsonNode: listStations) {

			if (jsonNode.findValue(id) != null && !jsonNode.findValue(id).asText().equals("None"))   // si l'id n'est pas trouve dans le fichier on envoit une erreur
				idStation = jsonNode.findValue(id).asText();
			else return false;

			if (jsonNode.findValue(bike_available) != null && !jsonNode.findValue(bike_available).asText().equals("None"))   // mets des valeurs par defaut si besoin
				bikeAvailable = jsonNode.findValue(bike_available).asText();
			else bikeAvailable = "0";

			if (jsonNode.findValue(bike_available) != null && !jsonNode.findValue(dock_available).asText().equals("None"))   // mets des valeurs par defaut si besoin
				slotAvailable = jsonNode.findValue(dock_available).asText();
			else slotAvailable = "0";

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

		return true;
	}

	// donne les termes a utilise pour recuperer les valeurs du json
	private static HashMap<String,String> researchsTermesBike(List<List<String>> listTermes, JsonNode treeJson, boolean staticLink) {
		HashMap<String, String> champsJson = new HashMap<String,String>(); // Champs , Champs trouver dans le Json
		boolean ok;
		InitialisationCheckValueBikeStation(listTermes, staticLink);

		for (List<String> list : listTermes) {
			ok = researchTermes(list, champsJson, treeJson);

			if (!ok)  // si aucun terme n'a ete trouve alors on avertit le programme que nous ne pouvons pas parser le fichier
				return null;
		}
		return champsJson;
	}

	// cherche terme valide dans le json
	private static boolean researchTermes(List<String> list, HashMap<String,String> champsJson, JsonNode treeJson) {
		int i = 0;
		Boolean b = false;
		String champs = list.get(0), path;

		if (champs.equals("stationParentNode")) {
			for (String terme : list) {

				if(treeJson.findValue(terme) != null) {
					path = getPathListStation(treeJson, terme);
					System.err.println("Path : " + path);
					champsJson.put(champs, path);
					b = true;
					break;
				}

				i++;
			}
		} else {

			for (String terme : list) {

				if (treeJson.findValue(terme) != null) {
					System.err.println("Find term " + terme);
					champsJson.put(champs, terme);
					b = true;
					break;
				}

				i++;
			}

			if (b == false) {
				champsJson.put(champs, null);

			}
		}

		return b;
	}

	// cherche le noeud json ou sont stockees les stations et retourne son chemin
	private static String getPathListStation(JsonNode tree, String term) {
		Iterator<Entry<String, JsonNode>> ite = tree.fields();
		Entry<String, JsonNode> next;
		String test;

		while (ite.hasNext()) {
			next = ite.next();

			if (next.getKey().equals(term))
				return next.getKey();

			test = getPathListStation(next.getValue(), term);
			if (test != null)
				return next.getKey() + "." + test;
		}

		return null;
	}

	private static void InitialisationCheckValueBikeStation(List<List<String>> listTermes, boolean staticLink) {
		List<String> stationParentNode = null, stationLat = null, bikesAvailable = null;
		List<String> stationId = null, stationLon = null, docksAvailable = null;
		List<String> stationName = null, stationCapacity = null, lastUpdate = null;
		// ORDRE [0]stationParentNode,[1]stationId,[2]stationName [3]StationLat [4]StationLon [5]StationCapacity [6]bikesAvailable [7]docksAvailable

		// Listes de termes a checker pour pouvoir construire le turtle
		stationParentNode = new ArrayList<String>(Arrays.asList("stationParentNode", "stations", "stationBeanList", "features", "sl", "fields","values"));
		stationId = new ArrayList<String>(Arrays.asList("stationId", "station_id", "id", "number", "properties.number","idstation")) ;
		if (staticLink) {
			stationName = new ArrayList<String>(Arrays.asList("stationName", "name", "s", "properties.name", "na", "nom")) ;
			stationLat = new ArrayList<String>(Arrays.asList("stationLat", "lat", "latitude", "la", "properties.lat", "coordonnees[0]")) ;
			stationLon = new ArrayList<String>(Arrays.asList("stationLon", "lon", "longitude", "lg", "long", "lo", "lng", "properties.lng", "coordonnees[1]")) ;
			stationCapacity = new ArrayList<String>(Arrays.asList("stationCapacity", "capacity", "bike_stands", "properties.bike_stands", "totalDocks", "da", "to", "nombreemplacementsactuels")) ;
		}else {
			bikesAvailable = new ArrayList<String>(Arrays.asList("bikesAvailable","num_bikes_available","bikes_available","numBikesAvailable","ba","availableBikes",
					"available_bikes","properties.available_bikes","properties.bikes_available","properties.num_bikes_available","av","nombrevelosdisponibles"));
			docksAvailable = new ArrayList<String>(Arrays.asList("docksAvailable","num_docks_available","docks_available","numDocksAvailable","da","availableDocks","available_bike_stands",
					"properties.available_bike_stands", "properties.docks_available", "properties.num_docks_available","fr","nombreemplacementsdisponibles")) ;
			lastUpdate = new ArrayList<String>(Arrays.asList("lastUpdate","last_updated","lastUpdate","lastUpdatedOther","lu","properties.last_update"));
		}

		// Liste de list de termes a verifie lors de la lecture du fichier
		listTermes.add(stationParentNode);
		listTermes.add(stationId);
		if (staticLink) {
			listTermes.add(stationName);
			listTermes.add(stationLat);
			listTermes.add(stationLon);
			listTermes.add(stationCapacity);
		} else {
			listTermes.add(bikesAvailable);
			listTermes.add(docksAvailable);
			//listTermes.add(lastUpdate);
		}
	}

	private static List<City> listCity(){  // pas besoin de gestion d'erreur ici, si la ville n'est pas retournee, elle n'apparaitra pas pour l'utilisateur
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
		String testIdStation = "";
		List<HistoriqueStation> historiqueStationList = null;
		HistoriqueStation historiqueStation;
		float avgLat = 0; float avgLon = 0;
		int nbStation = 0;  // sert a savoir combien de station ont ete traite

		RDFConnection conn = RDFConnectionFactory.connect("http://localhost:3030/Cities/query");
		QueryExecution qExec = conn.query("PREFIX ns0: <http://semanticweb.org/ontologies/City#> PREFIX ns1: <http://www.w3.org/2003/01/geo/wgs84_pos> "
				+ "SELECT ?nameCity ?name ?s ?link ?stationId ?lat ?lon ?capacity ?ba ?sa ?date ?bikeAvailable ?slotAvailable "
				+ "WHERE { ?s a ns0:City ; "
				+ " ns0:CityName ?nameCity ; "
				+ " ns0:LienDonneesDynamique ?link ; "
				+ " ns0:CityPublicTransport _:ns. "
				+ " _:ns a ns0:CityBikeStation; "
				+ "ns0:StationId ?stationId; "
				+ "ns0:Stationname ?name; "
				+ "ns0:StationLocalisation [ns1:lat ?lat; ns1:long ?lon;] ; "
				+ "ns0:StationTotalcapacity ?capacity; "
				+ "ns0:StationHistorique _:hs. "
				+ "_:hs ns0:StationState _:s. "
				+"	  		_:s ns0:BikeAvailable ?bikeAvailable; "
				+"	           ns0:SlotAvailable ?slotAvailable; "
				+"	           ns0:Date ?date "
				+ "FILTER (str(?nameCity) = \"" + nameCity + "\" ) }");
		ResultSet rs = qExec.execSelect();

		// Recuperation des stations d'une ville
		while(rs.hasNext()) {
			QuerySolution qs = rs.next();

			if (testIdStation.equals(qs.getLiteral("stationId").getString()) == false) {  // teste si on a change de station
				// mets a jour le nombre de stations recuperees
				nbStation += 1;

				// recupere les donnees d'une station
				LocalisationCity localisationCity = new LocalisationCity(qs.getLiteral("lat").getFloat(), qs.getLiteral("lon").getFloat());
				historiqueStationList = new ArrayList<HistoriqueStation>();
				historiqueStation = new HistoriqueStation();
				BikeStation bikeStation = new BikeStation(qs.getLiteral("stationId").getString(),
						qs.getLiteral("capacity").getString(),
						qs.getLiteral("name").getString(),
						localisationCity,
						historiqueStationList);
				city.addBikeStation(bikeStation);
				city.setIRI(qs.getResource("s").getURI());
				city.setDynamicLink(qs.getResource("link").getURI());

				// recupere le nouvel id station pour savoir quand on a change
				testIdStation = qs.getLiteral("stationId").getString();
			}

			// recupere les donnees d'un etat de stations
			historiqueStation = new HistoriqueStation(qs.getLiteral("date").getLong(), qs.getLiteral("bikeAvailable").getInt(), qs.getLiteral("slotAvailable").getInt());
			historiqueStationList.add(historiqueStation);

			/*System.out.println(qs.getLiteral("name"));
            System.out.println(qs.getLiteral("stationId"));
            System.out.println(qs.getLiteral("lat"));
            System.out.println(qs.getLiteral("lon"));*/
		}

		qExec.close();
		conn.close();

		if (nbStation == 0)  // si aucune station recuperee ont envoie null pour declarer une erreur
			return null;

		// recupere la position de la ville
		city.setLocalisation(getLocalisation(city.getName()));

		return city;
	}

	// calcul la position de la ville 'name' en moyennant la position des stations
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

		if (nbStation == 0)  // previent le programme que la requete a echoue
			return null;

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
