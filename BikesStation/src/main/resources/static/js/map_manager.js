// On initialise les variables comme la position par defaut (Paris), la carte, 
// le temps d'animation et les types de transport choisi
var time = 400;
var lat = 45.43992;
var lon = 4.3896303;
var control_begin_pointer = false;
var control_end_pointer = false;
var pointers = [];
var types = ['Bike'];
var search = '';
var macarte = null;
var cityToShow = null;


function initMap(cities, latc, lonc, zoom) {
	
	if (macarte != null){
		macarte.off();
		macarte.remove();
	}
	
    // Créer l'objet "macarte" et l'insèrer dans l'élément HTML qui a l'ID "map"
    macarte = L.map('map').setView([latc, lonc], zoom);
    
    // Nous ajoutons un marqueur sur chaque ville disponible
    if (cities != null){
	    for(var i = 0; i < cities.length; i++) {
	    	var city = cities[i];
	    	var marker = L.marker([city.localisation.lat, city.localisation.lg]).addTo(macarte);
	    	marker.bindPopup(city.name);
	    }
    }
    
    L.tileLayer('https://{s}.tile.openstreetmap.fr/osmfr/{z}/{x}/{y}.png', {
	    attribution: 'données © <a href="//osm.org/copyright">OpenStreetMap</a>/ODbL - rendu <a href="//openstreetmap.fr">OSM France</a>',
	        minZoom: 1,
	        maxZoom: 20
	    }).addTo(macarte);
    
    // add marker when a user click on the map
    macarte.on('click', function(e) {
    	
    	if (control_begin_pointer) {
	        var popLocation = e.latlng;
	    	addPointeur(popLocation, true);
	    	refreshStationFilter(cityToShow);
    	}
    	
    	if (control_end_pointer) {
	        var popLocation = e.latlng;
	    	addPointeur(popLocation, false);
	    	refreshStationFilter(cityToShow);
    	}
    });
}

function addPointer(newLocation, begin) {
	var locStation = nearestStation(newLocation); // compute the nearest station
	
	if (begin && pointers.length < 2) {
		pointers.push(popLocation);
	    pointers.push(locStation);
	}

	if (begin && pointers.length >= 4) {
		pointers[0] = popLocation;
		pointers[1] = locStation;
	}
	
	if (!begin && pointers.length < 4) {	
		pointers.push(popLocation);
	    pointers.push(locStation);
	}

	if (!begin && pointers.length >= 4) {
		pointers[2] = popLocation;
		pointers[3] = locStation;
	}
	
    // "close" the button to add a pointer
	control_begin_pointer = false;
	control_end_pointer = false;
}

function nearestStation(newLocation) {
	var nearest = null;
	var st;
	var dist1, dist2;
	
	for (var i = 0; i < city.bikesStations.length; i++){
		st = city.bikesStations[i];
		
		if (nearest != null){
			dist1 = Math.sqrt((st.localisation.lat - newLocation[0]) * (st.localisation.lat - newLocation[0]) + (st.localisation.lg - newLocation[1]) * (st.localisation.lg - newLocation[1]));  
			dist2 = Math.sqrt((nearest.lat - newLocation[0]) * (nearest.lat - newLocation[0]) + (nearest.lg - newLocation[1]) * (nearest.lg - newLocation[1]));  
			
			console.log("Dist1 " + dist1);
			console.log("Dist2 " + dist2);
			
			if (dist1 < dist2) {
				nearest = st.localisation;
			}
		} else {
			nearest = st.localisation;
		}
	}
	
	return nearest;
}

function updateCityToShow(city) {
	cityToShow = city;
}

function reinitMap(lastLoc){
	inc = 12;
	latc = lat;
	lonc = lon;
	
	// initialiser la latitude et la longitude
	if (lastLoc != null){
		latc = lastLoc.lat;
		lonc = lastLoc.lg;
	}
	

	// positionnement de la carte sur la ville
    //macarte.setView([lat, lon], 12);
	id = setInterval(dezoomMap, time);
	
	// fonction d'animation du zoom
	function dezoomMap(){
		
		if (inc <= 4){
	        clearInterval(id);
		    macarte.setView([latc, lonc], inc);
		    
		    // enlever les marqueurs
		    
		}else{
		    inc -= 2;
		    macarte.setView([latc, lonc], inc);
		}
	}

	// execute la premiere etape de l'animation tout de suite
	dezoomMap();
}

function goToCity(city){   // type : City
	inc = 0;
	locs = [];
	
	// valeur par defaut : localisation Paris
	locs.push(lat);
	locs.push(lon);
	
	if (city.localisation != null && 
			(city.localisation.lat != 0 || city.localisation.lg != 0)){
		
		locs[0] = city.localisation.lat;
		locs[1] = city.localisation.lg;
	}

	// positionnement de la carte sur la ville
    macarte.setView([locs[0], locs[1]], 4);
	id = setInterval(zoomMap, time);
	
	// fonction d'animation du zoom
	function zoomMap(){
		
		if (inc >= 8){
	        clearInterval(id);
		    macarte.setView([locs[0], locs[1]], 4 + (inc + 1));
		    
		    // creer les popup et les marqueurs pour les stations
		    createPopup(city);
		}else{
		    inc += 2;
		    macarte.setView([locs[0], locs[1]], 4 + (inc + 1));
		}
	}
	
	// execute la premiere etape de l'animation tout de suite
	zoomMap();
}

function changeCity(locCityDep, cityArr){   // type : localisationCity, City
	inc = 0; len = 0; zoom = false;
	pos = [0, 0];
	dir = [0, 0];

	// calcul direction du deplacement
	len = Math.sqrt((cityArr.localisation.lat - locCityDep.lat) * (cityArr.localisation.lat - locCityDep.lat) + 
			(cityArr.localisation.lg - locCityDep.lg) * (cityArr.localisation.lg - locCityDep.lg));
	
	dir[0] = (cityArr.localisation.lat - locCityDep.lat) / len;
	dir[1] = (cityArr.localisation.lg - locCityDep.lg) / len;
	
	// calul de la position de depart
	pos[0] = locCityDep.lat;
	pos[1] = locCityDep.lg;
	
	// creation de l'animation
	id = setInterval(moveMap, 400);
	
	// fonction d'animation du deplacement
	function moveMap(){
		
		if (inc >= 2 && zoom){
	        clearInterval(id);
		    macarte.setView([cityArr.localisation.lat, cityArr.localisation.lg], 7 + inc * 3);
		    
		    // creer les popup et les marqueurs pour les stations
		    createPopup(cityArr);
		}else if (inc >= 2 && !zoom){
			inc = 0;
			zoom = !zoom;
		}else if (!zoom){
		    inc += 1;
		    macarte.setView(pos, 12 - inc * 3);
		}else if (zoom){
		    inc += 1;
			
			// calul de la nouvelle position
			//pos[0] = pos[0] + dir[0];
			//pos[1] = pos[1] + dir[1];
			
		    macarte.setView([cityArr.localisation.lat, cityArr.localisation.lg], 7 + inc * 3);
		}
	}
}

function createPopup(city){
	var str = "";
	
	for (var i = 0; i < city.bikesStations.length; i++){
		str = "";
		var st = city.bikesStations[i];
		var hist = st.listHistoriqueStation[st.listHistoriqueStation.length - 1]; // get the last update of this station
		
		if (st.localisation.lat != 0 || st.localisation.lg != 0){
	    	var marker = L.marker([st.localisation.lat, st.localisation.lg]).addTo(macarte);
	    	str += "<p>" + st.name + "  " + st.capacity + "<p>";
	    	str += "<p style='text-align:center;'>" + hist.bikeAvailable + "  " + hist.slotAvailable + "<p>";
	    	marker.bindPopup(str);
		}
	}
}

function stationFilter(city, field){
	
	if(city != null){
		
		// change la valeur de la recherche
		search = field.value.toLowerCase();
		
		// reinitialise la carte
		if (macarte != null){
			macarte.off();
			macarte.remove();
		}
	
	    // Creer l'objet "macarte" et l'inserer dans l'element HTML qui a l'ID "map"
	    macarte = L.map('map').setView([city.localisation.lat, city.localisation.lg], 12);
		
	    // ajout des marqueurs pertinant
		for (var i = 0; i < city.bikesStations.length; i++){
			var st = city.bikesStations[i];
			var name = st.name.toLowerCase();
			if ((st.localisation.lat != 0 || st.localisation.lg != 0) && name.includes(search)){
		    	var marker = L.marker([st.localisation.lat, st.localisation.lg]).addTo(macarte);
		    	marker.bindPopup(st.name);
			}
		}
	    
	    L.tileLayer('https://{s}.tile.openstreetmap.fr/osmfr/{z}/{x}/{y}.png', {
		    attribution: 'données © <a href="//osm.org/copyright">OpenStreetMap</a>/ODbL - rendu <a href="//openstreetmap.fr">OSM France</a>',
		        minZoom: 1,
		        maxZoom: 20
		    }).addTo(macarte);
	}
}

function refreshStationFilter(city){
	
	if(city != null){
		// reinitialise la carte
		if (macarte != null){
			macarte.off();
			macarte.remove();
		}
	
	    // Creer l'objet "macarte" et l'inserer dans l'element HTML qui a l'ID "map"
	    macarte = L.map('map').setView([city.localisation.lat, city.localisation.lg], 12);
		
	    // ajout des marqueurs pertinant
		for (var i = 0; i < city.bikesStations.length; i++){
			var st = city.bikesStations[i];
			var name = st.name.toLowerCase();
			if ((st.localisation.lat != 0 || st.localisation.lg != 0) && name.includes(search)){
		    	var marker = L.marker([st.localisation.lat, st.localisation.lg]).addTo(macarte);
		    	marker.bindPopup(st.name);
			}
		}
	    
	    L.tileLayer('https://{s}.tile.openstreetmap.fr/osmfr/{z}/{x}/{y}.png', {
		    attribution: 'données © <a href="//osm.org/copyright">OpenStreetMap</a>/ODbL - rendu <a href="//openstreetmap.fr">OSM France</a>',
		        minZoom: 1,
		        maxZoom: 20
		    }).addTo(macarte);
		
		// add the trip
		if (pointers.length >= 4) {
			/*L.Routing.control({
			    waypoints: [
			        L.latLng(pointers[0][0], pointers[0][1]),
			        L.latLng(pointers[1][0], pointers[1][1]),
			        L.latLng(pointers[2][0], pointers[2][1]),
			        L.latLng(pointers[3][0], pointers[3][1])
			    ],
			    routeWhileDragging: false
			}).addTo(map);*/
		}
	}
}

function addRemoveTypeTransport(element, name){
	
	// ajoute ou supprime le type de transport
	if (types.includes(name))
		for( var i = 0; i < types.length; i++){
		   if ( types[i] === name) {
			   types.splice(i, 1);
		   }
		}
	else types.push(name);
	
	// change l'image de l'element
	
}

function addTrip(begin){   // begin : bool
	
	control_begin_pointer = begin;
	control_end_pointer = !begin;
		
}

//window.onload = function(){
	// Fonction d'initialisation qui s'exécute lorsque le DOM est chargé
//};
