function wemo($scope,$http){
	
	$scope.devices = [{name:"sd"}];
	
	$http({method: 'GET', url: '/devices'}).
	  success(function(data, status, headers, config) {
		  $scope.devices = data.devices;
	  }).
	  error(function(data, status, headers, config) {
		  alert(status);
	  });
	
	$scope.toggle = function(serial,on){
		$http({method: 'GET', url: '/devices/'+serial+"/" + (on?"on":"off")}).
		  success(function(data, status, headers, config) {
			 
		  }).
		  error(function(data, status, headers, config) {
			  alert(status);
		  });
	}
}