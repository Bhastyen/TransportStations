// On initialise la latitude et la longitude de Paris (centre de la carte)
var time = 400;
var lat = 45.43992;
var lon = 4.3896303;
var macarte = null;


function initMap(cities, latc, lonc, zoom) {
    
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
	inc = 12
	latc = lat
	lonc = lon
	
	// initialiser la latitude et la longitude
	console.log(lastLoc);
	
	if (lastLoc != null){
		latc = lastLoc.lat
		lonc = lastLoc.lg
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
}


function changeCity(city){   // type : City
	inc = 0
	locs = []
	name_stations = []
	
	// valeur par defaut : localisation Paris
	locs.push(lat)
	locs.push(lon)
	name_stations.push(city.name);
	
	if (city.localisation != null && 
			(city.localisation.lat != 0 || city.localisation.lg != 0)){
		
		locs[0] = city.localisation.lat
		locs[1] = city.localisation.lg
	}

	// positionnement de la carte sur la ville
    macarte.setView([locs[0], locs[1]], 4);
	id = setInterval(zoomMap, time);
	
	// positionnement des markers de stations
	for (var i = 0; i < city.bikesStations.length; i++){
		var st = city.bikesStations[i];
		if (st.localisation.lat != 0 || st.localisation.lg != 0){
			locs.push(st.localisation.lat);
			locs.push(st.localisation.lg);
			name_stations.push(st.name);
		}
	}
	
	// fonction d'animation du zoom
	function zoomMap(){
		
		if (inc >= 8){
	        clearInterval(id);
		    macarte.setView([locs[0], locs[1]], 4 + (inc + 1));
		    
		    for (var i = 0; i < locs.length; i+=2){
		    	var marker = L.marker([locs[i], locs[i+1]]).addTo(macarte);
		    	marker.bindPopup(name_stations[i/2]);
		    }
		}else{
		    inc += 2;
		    macarte.setView([locs[0], locs[1]], 4 + (inc + 1));
		}
	}
}

//window.onload = function(){
	// Fonction d'initialisation qui s'exécute lorsque le DOM est chargé
//};
