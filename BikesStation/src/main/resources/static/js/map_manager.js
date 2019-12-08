// On initialise les variables comme la position par defaut (Paris), la carte, 
// le temps d'animation et les types de transport choisi
var time = 400;
var lat = 45.43992;
var lon = 4.3896303;
var types = ['Bike'];
var search = '';
var macarte = null;


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
    
    // Leaflet ne récupère pas les cartes (tiles) sur un serveur par défaut. Nous devons lui préciser où nous souhaitons les récupérer. Ici, openstreetmap.fr
    L.tileLayer('https://{s}.tile.openstreetmap.fr/osmfr/{z}/{x}/{y}.png', {
	    // Il est toujours bien de laisser le lien vers la source des données
	    attribution: 'données © <a href="//osm.org/copyright">OpenStreetMap</a>/ODbL - rendu <a href="//openstreetmap.fr">OSM France</a>',
	        minZoom: 1,
	        maxZoom: 20
	    }).addTo(macarte);
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
	
	for (var i = 0; i < city.bikesStations.length; i++){
		var st = city.bikesStations[i];
		if (st.localisation.lat != 0 || st.localisation.lg != 0){
	    	var marker = L.marker([st.localisation.lat, st.localisation.lg]).addTo(macarte);
	    	marker.bindPopup(st.name);
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
			if ((st.localisation.lat != 0 || st.localisation.lg != 0) && name.startsWith(search)){
		    	var marker = L.marker([st.localisation.lat, st.localisation.lg]).addTo(macarte);
		    	marker.bindPopup(st.name);
			}
		}
	    
	    // Leaflet ne récupère pas les cartes (tiles) sur un serveur par défaut. Nous devons lui préciser où nous souhaitons les récupérer. Ici, openstreetmap.fr
	    L.tileLayer('https://{s}.tile.openstreetmap.fr/osmfr/{z}/{x}/{y}.png', {
		    // Il est toujours bien de laisser le lien vers la source des données
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
			if ((st.localisation.lat != 0 || st.localisation.lg != 0) && name.startsWith(search)){
		    	var marker = L.marker([st.localisation.lat, st.localisation.lg]).addTo(macarte);
		    	marker.bindPopup(st.name);
			}
		}
	    
	    // Leaflet ne récupère pas les cartes (tiles) sur un serveur par défaut. Nous devons lui préciser où nous souhaitons les récupérer. Ici, openstreetmap.fr
	    L.tileLayer('https://{s}.tile.openstreetmap.fr/osmfr/{z}/{x}/{y}.png', {
		    // Il est toujours bien de laisser le lien vers la source des données
		    attribution: 'données © <a href="//osm.org/copyright">OpenStreetMap</a>/ODbL - rendu <a href="//openstreetmap.fr">OSM France</a>',
		        minZoom: 1,
		        maxZoom: 20
		    }).addTo(macarte);
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

//window.onload = function(){
	// Fonction d'initialisation qui s'exécute lorsque le DOM est chargé
//};
