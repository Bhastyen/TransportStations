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
	    	addPointer(popLocation, true);
	    	refreshStationFilter(cityToShow);
    	}
    	
    	if (control_end_pointer) {
	        var popLocation = e.latlng;
	    	addPointer(popLocation, false);
	    	refreshStationFilter(cityToShow);
    	}
    });
}

function addPointer(newLocation, begin) {
	var locStation = nearestStation(newLocation); // compute the nearest station
	
	if (begin && pointers.length < 4) {
		pointers.push(newLocation.lat);
		pointers.push(newLocation.lng);
	    pointers.push(locStation.lat);
	    pointers.push(locStation.lg);
	}

	if (begin && pointers.length >= 8) {
		pointers[0] = newLocation.lat;
		pointers[1] = newLocation.lng;
		pointers[2] = locStation.lat;
		pointers[3] = locStation.lg;
	}
	
	if (!begin && pointers.length < 8) {
		pointers.push(newLocation.lat);
		pointers.push(newLocation.lng);
	    pointers.push(locStation.lat);
	    pointers.push(locStation.lg);
	}

	if (!begin && pointers.length >= 8) {
		pointers[4] = newLocation.lat;
		pointers[5] = newLocation.lng;
		pointers[6] = locStation.lat;
		pointers[7] = locStation.lg;
	}
	
    // "close" the button to add a pointer
	control_begin_pointer = false;
	control_end_pointer = false;
}

function nearestStation(newLocation) {
	var nearest = null;
	var st;
	var dist1, dist2;
	
	for (var i = 0; i < cityToShow.bikesStations.length; i++){
		st = cityToShow.bikesStations[i];
		
		if (nearest != null){
			dist1 = Math.sqrt((st.localisation.lat - newLocation.lat) * (st.localisation.lat - newLocation.lat) + (st.localisation.lg - newLocation.lng) * (st.localisation.lg - newLocation.lng));  
			dist2 = Math.sqrt((nearest.lat - newLocation.lat) * (nearest.lat - newLocation.lat) + (nearest.lg - newLocation.lng) * (nearest.lg - newLocation.lng));  
			
			//console.log("Dist1 " + dist1);
			//console.log("Dist2 " + dist2);
			
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
    //macarte.setView([lat, lon], 13);
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
	
	// position de depart
	var pos = [locCityDep.lat, locCityDep.lg];
	
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
		refreshStationFilter(city);
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
	    macarte = L.map('map').setView([city.localisation.lat, city.localisation.lg], 13);
		
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
	    
	    // add marker when a user click on the map
	    macarte.on('click', function(e) {
	    	
	    	if (control_begin_pointer) {
		        var popLocation = e.latlng;
		    	addPointer(popLocation, true);
		    	refreshStationFilter(cityToShow);
	    	}
	    	
	    	if (control_end_pointer) {
		        var popLocation = e.latlng;
		    	addPointer(popLocation, false);
		    	refreshStationFilter(cityToShow);
	    	}
	    });
		
		// add the trip
		if (pointers.length >= 8) {
			L.Routing.control({
			    waypoints: [
			        L.latLng(pointers[0], pointers[1]),
			        L.latLng(pointers[2], pointers[3]),
			        L.latLng(pointers[4], pointers[5]),
			        L.latLng(pointers[6], pointers[7])
			    ],
			    routeWhileDragging: false,
			    useZoomParameter: false,
        		router: L.Routing.graphHopper('62fcb6e5-14d3-4ac6-b64c-65eb9bcbb803')
			}).addTo(macarte);
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
