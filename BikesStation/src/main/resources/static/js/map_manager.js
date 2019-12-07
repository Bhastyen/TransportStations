// On initialise la latitude et la longitude de Paris (centre de la carte)
var lat = 48.852969;
var lon = 2.349903;
var macarte = null;


function initMap() {
    
    // Créer l'objet "macarte" et l'insèrer dans l'élément HTML qui a l'ID "map"
    macarte = L.map('map').setView([lat, lon], 4);
    
    // Nous ajoutons un marqueur
    //var marker = L.marker([lat, lon]).addTo(macarte);
    
    // Leaflet ne récupère pas les cartes (tiles) sur un serveur par défaut. Nous devons lui préciser où nous souhaitons les récupérer. Ici, openstreetmap.fr
    L.tileLayer('https://{s}.tile.openstreetmap.fr/osmfr/{z}/{x}/{y}.png', {
        // Il est toujours bien de laisser le lien vers la source des données
        attribution: 'données © <a href="//osm.org/copyright">OpenStreetMap</a>/ODbL - rendu <a href="//openstreetmap.fr">OSM France</a>',
            minZoom: 1,
            maxZoom: 20
        }).addTo(macarte);
}


function changeCity(city){   // type : City
	inc = 0
	locs = []
	name_stations = []
	
	// valeur par defaut : localisation Paris
	locs.push(lat)
	locs.push(lon)
	name_stations.push(city.name);
	
	if (city.localisation != null){
		locs[0] = city.localisation.lat
		locs[1] = city.localisation.lg
	}

	// positionnement de la carte sur la ville
    macarte.setView([locs[0], locs[1]], 4);
	id = setInterval(zoomMap, 200);
	
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

initMap();

//window.onload = function(){
	// Fonction d'initialisation qui s'exécute lorsque le DOM est chargé
//};
