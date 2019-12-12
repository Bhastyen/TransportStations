

function changeStyleCity(element){
	
	if (element.style.backgroundColor != '#fff39f')
		element.style.backgroundColor = '#fff39f';
	else element.style.backgroundColor = '#00000000';
	
}

function showNewCityError(message){
	
	setTimeout( function(){
			alert(message);
		}
	, 1000);
}
