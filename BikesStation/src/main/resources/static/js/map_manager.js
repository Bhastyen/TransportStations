// On initialise les variables comme la position par defaut (Paris), la carte, 
// le temps d'animation et les types de transport choisi
var time = 400;
var lat = 45.43992;
var lon = 4.3896303;
var control_begin_pointer = false;
var control_end_pointer = false;
var pointers = [];
var infos = [];
var types = ['Bike'];
var search = '';
var macarte = null;
var cityToShow = null;
var controlRouting = null;
var zoomArr = 13, zoomBeg = 3;


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
	    	//refreshStationFilter(cityToShow);
    	}
    	
    	if (control_end_pointer) {
	        var popLocation = e.latlng;
	    	addPointer(popLocation, false);
	    	//refreshStationFilter(cityToShow);
    	}
    });
	
	// add the trip
	controlRouting = L.Routing.control({
	    waypoints: [],
	    routeWhileDragging: false,
	    useZoomParameter: false,
	    autoRoute: false,
	    createMarker: myCreateMarker,
		router: L.Routing.graphHopper('62fcb6e5-14d3-4ac6-b64c-65eb9bcbb803')
	}).addTo(macarte);
	
	controlRouting.getPlan().draggableWaypoints = false;
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
	    pointers.push(locStation.lat);
	    pointers.push(locStation.lg);
		pointers.push(newLocation.lat);
		pointers.push(newLocation.lng);
	}

	if (!begin && pointers.length >= 8) {
		pointers[4] = locStation.lat;
		pointers[5] = locStation.lg;
		pointers[6] = newLocation.lat;
		pointers[7] = newLocation.lng;
	}
	
    // "close" the button to add a pointer
	document.getElementById('map').style.cursor = "pointer";
	control_begin_pointer = false;
	control_end_pointer = false;
	
	// modify waypoints for control routing
	if (controlRouting != null){
		controlRouting.setWaypoints( [
			        L.latLng(pointers[0 % pointers.length], pointers[1 % pointers.length]),
			        L.latLng(pointers[2 % pointers.length], pointers[3 % pointers.length]),
			        L.latLng(pointers[4 % pointers.length], pointers[5 % pointers.length]),
			        L.latLng(pointers[6 % pointers.length], pointers[7 % pointers.length])
			    ]);
		controlRouting.route();
	}
}

function myCreateMarker (i, start, n){
    var marker_icon = null;
    
    if (i == 0) {
        marker_icon = new L.icon({
        	iconUrl: '../../../css/images/pointer-green.png',

            iconSize:     [23, 45], 
            shadowSize:   [50, 64],
            iconAnchor:   [11, 44],
            shadowAnchor: [4, 62], 
            popupAnchor:  [-3, -32] });
    } else if (i == n - 1) {
        marker_icon = new L.icon({
        	iconUrl: '../../../css/images/pointer-red.png',

            iconSize:     [23, 45], 
            shadowSize:   [50, 64],
            iconAnchor:   [11, 44],
            shadowAnchor: [4, 62], 
            popupAnchor:  [-3, -32] });
    } else {
        marker_icon = new L.icon({
        	iconUrl: '../../../css/images/pointer-purple.png',

            iconSize:     [23, 45], 
            shadowSize:   [50, 64],
            iconAnchor:   [11, 44],
            shadowAnchor: [4, 62], 
            popupAnchor:  [-3, -32] });
    }
    
    var marker = L.marker (start.latLng, {
        draggable: false,
        bounceOnAdd: false,
        icon: marker_icon
    });
    
    // create popup
    if (i == 1) {
    	createInfo(infos[0], marker);
    }else if (i == 2){
    	createInfo(infos[1], marker);
    }
    
    return marker;
}

function nearestStation(newLocation, begin) {
	var nearest = null;
	var info = null;
	var st;
	var dist1, dist2;
	
	for (var i = 0; i < cityToShow.bikesStations.length; i++){
		st = cityToShow.bikesStations[i];
		
		if (nearest != null){
			dist1 = Math.sqrt((st.localisation.lat - newLocation.lat) * (st.localisation.lat - newLocation.lat) + (st.localisation.lg - newLocation.lng) * (st.localisation.lg - newLocation.lng));  
			dist2 = Math.sqrt((nearest.lat - newLocation.lat) * (nearest.lat - newLocation.lat) + (nearest.lg - newLocation.lng) * (nearest.lg - newLocation.lng));  
			
			if (dist1 < dist2) {
				nearest = st.localisation;
				info = st;
			}
		} else {
			nearest = st.localisation;
			info = st;
		}
	}
	
	// give infos about new station
	if (begin) {
		if (infos.length < 1) {
			infos.push(info);
		}else{
			infos[0] = info;
		}
	}else{
		if (infos.length < 2) {
			infos.push(info);
		}else{
			infos[1] = info;
		}
	}
	
	return nearest;
}

function updateCityToShow(city) {
	cityToShow = city;
}

function reinitMap(lastLoc){
	inc = zoomArr;
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
		
		if (inc <= zoomBeg){
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
    macarte.setView([locs[0], locs[1]], zoomBeg);
	id = setInterval(zoomMap, time);
	
	// fonction d'animation du zoom
	function zoomMap(){
		
		if (zoomBeg + (inc + 1) >= zoomArr){
	        clearInterval(id);
		    macarte.setView([locs[0], locs[1]], zoomBeg + (inc + 1));
		    
		    // creer les popup et les marqueurs pour les stations
		    createPopup(city);
		}else{
		    inc += 3;
		    macarte.setView([locs[0], locs[1]], zoomBeg + (inc + 1));
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
		
		if (inc >= 5 && zoom){
	        clearInterval(id);
		    macarte.setView([cityArr.localisation.lat, cityArr.localisation.lg], zoomBeg + inc * 2);
		    
		    // creer les popup et les marqueurs pour les stations
		    createPopup(cityArr);
		}else if (inc >= 5 && !zoom){
			inc = 0;
			zoom = !zoom;
		}else if (!zoom){
		    inc += 1;
		    macarte.setView(pos, zoomArr - inc * 2);
		}else if (zoom){
		    inc += 1;
		    macarte.setView([cityArr.localisation.lat, cityArr.localisation.lg], zoomBeg + inc * 2);
		}
	}
}

function createPopup(city){  // https://www.datavis.fr/index.php?page=leaflet-cluster
	var str = "";
	
    // specify popup options
	var markersCluster = new L.markerClusterGroup({
	    iconCreateFunction: function(cluster) {
	        var digits = (cluster.getChildCount() + '').length;
	        return L.divIcon({ 
	            html: cluster.getChildCount(), 
	            className: 'cluster digits-'+digits,
	            iconSize: null 
	        });
	    }
	});
	
	for (var i = 0; i < city.bikesStations.length; i++){
		str = "";
		var st = city.bikesStations[i];
		
		if ((st.localisation.lat != 0 || st.localisation.lg != 0) && st.name.toLowerCase().includes(search)){
	    	var marker = L.marker([st.localisation.lat, st.localisation.lg]);      //.addTo(macarte);
	    	createInfo(st, marker);
	    	markersCluster.addLayer(marker);
		}
	}
	
	//markersCluster.addTo(macarte);
	macarte.addLayer(markersCluster);
}

function createInfo(st, marker) {
	var str = "";
	var hist = st.listHistoriqueStation[st.listHistoriqueStation.length - 1];
	
	str += "<h3 style='text-align:center;'>" + st.name + "</h3>";
	str += "<table class='popup_table'>" +
			"<thead>"
            +"    <tr>"
            +"       <th class='popup_table'>Bikes Available</th>"
            +"       <th class='popup_table'>Slots Available</th>"
            +"         <th class='popup_table'>Total Capacity</th>"
            +"    </tr>"
            +"</thead>"
            +"<tbody>"
            +"<tr>"
            +"    <td class='popup_table'>"+hist.bikeAvailable +"</td>"
            +"    <td class='popup_table'>"+hist.slotAvailable+"</td>"
            +"    <td class='popup_table'>"+st.capacity+"</td>"
            +"</tr>"
            +"</tbody>"
            +"</table>";

	marker.bindPopup(str);
	
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
	    macarte = L.map('map').setView([city.localisation.lat, city.localisation.lg], zoomArr);
	    
	    L.tileLayer('https://{s}.tile.openstreetmap.fr/osmfr/{z}/{x}/{y}.png', {
		    attribution: 'données © <a href="//osm.org/copyright">OpenStreetMap</a>/ODbL - rendu <a href="//openstreetmap.fr">OSM France</a>',
		        minZoom: 1,
		        maxZoom: 20
		    }).addTo(macarte);
		
	    // ajout des marqueurs pertinent
	    createPopup(city);
	    
	    // add marker when a user click on the map
	    macarte.on('click', function(e) {
	    	
	    	if (control_begin_pointer) {
		        var popLocation = e.latlng;
		    	addPointer(popLocation, true);
		    	//refreshStationFilter(cityToShow);
	    	}
	    	
	    	if (control_end_pointer) {
		        var popLocation = e.latlng;
		    	addPointer(popLocation, false);
		    	//refreshStationFilter(cityToShow);
	    	}
	    });
		
		// add the trip
		controlRouting = L.Routing.control({
		    waypoints: [
		        L.latLng(pointers[0 % pointers.length], pointers[1 % pointers.length]),
		        L.latLng(pointers[2 % pointers.length], pointers[3 % pointers.length]),
		        L.latLng(pointers[4 % pointers.length], pointers[5 % pointers.length]),
		        L.latLng(pointers[6 % pointers.length], pointers[7 % pointers.length])
		    ],
		    routeWhileDragging: false,
		    useZoomParameter: false,
		    autoRoute: false,
		    createMarker: myCreateMarker,
    		router: L.Routing.graphHopper('62fcb6e5-14d3-4ac6-b64c-65eb9bcbb803')
		}).addTo(macarte);
		controlRouting.getPlan().draggableWaypoints = false;
		controlRouting.route();
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
	// control button to add point
	control_begin_pointer = begin;
	control_end_pointer = !begin;
	
	// change cursor for the map
	if (begin)
		document.getElementById('map').style.cursor = "url(../../../css/images/pointer-green-icon.png) 16 48, pointer";
	else document.getElementById('map').style.cursor = "url(../../../css/images/pointer-red-icon.png) 16 48, pointer";
}

function changeCursor() {
	document.getElementById('bd').style.cursor = 'progress';
	document.getElementById('map').style.cursor = 'progress';
}

//window.onload = function(){
	// Fonction d'initialisation qui s'exécute lorsque le DOM est chargé
//};
