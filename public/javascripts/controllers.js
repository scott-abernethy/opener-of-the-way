'use strict';

NavCtrl.$inject = ['$scope', '$location'];
function NavCtrl($scope, $location) {
  $scope.isPage = function(page) {
    var currentRoute = $location.path().substring(1) || 'home';
    return page === currentRoute;
  };
}

ArtifactLogCtrl.$inject = ['$http', '$scope', 'ArtifactLog', 'ArtifactSearch', 'ArtifactSocket'];
function ArtifactLogCtrl($http, $scope, ArtifactLog, ArtifactSearch, ArtifactSocket) {
  $scope.log = [];
  $scope.busy = false;
  $scope.last = -1;

  $scope.nextPage = function() {
    if ($scope.busy) return;
    $scope.busy = true;

    $http.get('artifact/log?count=20&last=' + $scope.last).
      success(function(data){
        for (var i = 0; i < data.length; i++) {
          $scope.log.push(data[i]);
        }
        if (data.length > 0) {
          var lastDay = data[data.length - 1];
          $scope.last = lastDay.items[lastDay.items.length - 1].id;
        }
        $scope.busy = false;
      }).
      error(function(data){
        alert('Failed to load data!');
      });
  };

  $scope.artifactSelect = function(artifact) {
    if (!artifact.proffered) {
      $http.put('artifact/' + artifact.id + '/touch')
    }
  };

  var update = function(a) {
    for (var i = 0; i < $scope.log.length; i++) {
      var day = $scope.log[i];
      for (var j = 0; j < day.items.length; j++) {
        var item = day.items[j];
        if (item.id == a.id) {
          day.items[j] = a;
        }
      }
    }

    for (var k = 0; k < $scope.results.items.length; k++) {
      var item = $scope.results.items[k];
      if (item.id == a.id) {
        $scope.results.items[k] = a;
      }
    }
    $scope.$digest();
  }

  var append = function(data) {
    var group = data.group;
    var artifact = data.artifact;
    if ($scope.log[0].name == group) {
      $scope.log[0].items.unshift(artifact);
    }
    else {
      $scope.log.unshift({
        name: group,
        items: [artifact]
      })
    }
    $scope.$digest();
  };

  var noResults = {
    show: false,
    items: [],
    icon: "icon-minus",
    desc: "None"
  };

  $scope.search = function(text) {
    if (text) {
      $scope.results = {
        show: true,
        items: [],
        icon: "icon-spinner icon-spin",
        desc: "Searching..."
      };
      var data = ArtifactSearch.query({query:text}, function(result) {
        $scope.results = {
          show: true,
          items: data,
          icon: "icon-minus",
          desc: "None"
        }
      });
    }
    else {
      $scope.results = noResults;
    }
  };
  $scope.clear = function() {
    $scope.searchText = "";
    $scope.results = noResults;
  };

  ArtifactSocket.subscribe(update, "ArtifactCloning");
  ArtifactSocket.subscribe(update, "ArtifactCloneFailed");
  ArtifactSocket.subscribe(update, "ArtifactCloned");
  ArtifactSocket.subscribe(update, "ArtifactUpdate");
  ArtifactSocket.subscribe(update, "ArtifactAwaiting");
  ArtifactSocket.subscribe(update, "ArtifactUnawaiting");
  ArtifactSocket.subscribe(append, "ArtifactCreated");

  $scope.clear();
}

GatewayCtrl.$inject = ['$http', '$scope', '$location', 'Gateway', 'Cultist', 'ArtifactSocket'];
function GatewayCtrl($http, $scope, $location, Gateway, Cultist, ArtifactSocket) {
  var recalc = function() {
    var open = false;
    for (var i = 0; i < $scope.gateways.length; i++) {
      if ($scope.gateways[i].open) {
        open = true;
        break;
      }
    }
    $scope.open = open;
  };
  $scope.$watch('gateways', recalc);
  $scope.$watch('locked', recalc);

  $scope.open = false;
  $scope.locked = false;
  $scope.gateways = [];
  recalc();

  var gateways = Gateway.query(function() {
    $scope.gateways = gateways;
  });
  var cultist = Cultist.get(function() {
    $scope.locked = cultist.shut
  });

  $scope.lock = function() {
    $scope.locked = true
    $http.put('gateway/lock', {"enable": true})
  };
  $scope.unlock = function() {
    $scope.locked = false
    $http.put('gateway/lock', {"enable": false})
  };
  $scope.scour = function() {
    $http.put('gateway/scour')
  };
  $scope.gatewaySelect = function(gateway) {
    var uri = '/gateway/'+gateway.id+'/edit';
    $location.path(uri);
  };

  var update = function(gs) {
    $scope.gateways = gs;
    $scope.$digest();

    var cultist = Cultist.get(function() {
      $scope.locked = cultist.shut
    });
  }

  ArtifactSocket.subscribe(update, "GatewayReload");
}

ClonedCtrl.$inject = ['$http', '$scope', 'Cloned', 'ArtifactSocket'];
function ClonedCtrl($http, $scope, Cloned, ArtifactSocket) {
  $scope.cloned = Cloned.query();
  $scope.artifactSelect = function(artifact) {
    $http.put('artifact/' + artifact.id + '/touch')
  };

  var cloned = function(a) {
    $scope.cloned.unshift(a)
    $scope.$digest();
  }
  var removeCheck = function(a) {
    var xs = $scope.cloned
    var xs2 = []
    for (var i = 0; i < xs.length; i++) {
      if (xs[i].id != a.id) {
        xs2.push(xs[i])
      }
    }
    $scope.cloned = xs2;
    $scope.$digest();
  }

  ArtifactSocket.subscribe(cloned, "ArtifactCloned");
  ArtifactSocket.subscribe(removeCheck, "ArtifactAwaiting");
}

AwaitingCtrl.$inject = ['$http', '$scope', 'Awaiting', 'ArtifactSocket'];
function AwaitingCtrl($http, $scope, Awaiting, ArtifactSocket) {
  $scope.awaitings = Awaiting.query();
  $scope.artifactSelect = function(artifact) {
    $http.put('artifact/' + artifact.id + '/touch')
  };

  var awaiting = function(a) {
    $scope.awaitings.unshift(a)
    $scope.$digest();
  }
  var unawaiting = function(a) {
    var xs = $scope.awaitings
    var xs2 = []
    for (var i = 0; i < xs.length; i++) {
      if (xs[i].id != a.id) {
        xs2.push(xs[i])
      }
    }
    $scope.awaitings = xs2;
    $scope.$digest();
  }
  var update = function(a) {
    for (var i = 0; i < $scope.awaitings.length; i++) {
      var item = $scope.awaitings[i];
      if (item.id == a.id) {
        $scope.awaitings[i] = a;
      }
    }
    $scope.$digest();
  }

  ArtifactSocket.subscribe(awaiting, "ArtifactAwaiting");
  ArtifactSocket.subscribe(unawaiting, "ArtifactUnawaiting");
  ArtifactSocket.subscribe(update, "ArtifactUpdate");
  ArtifactSocket.subscribe(update, "ArtifactCloning");
  ArtifactSocket.subscribe(update, "ArtifactCloneFailed");
  ArtifactSocket.subscribe(unawaiting, "ArtifactCloned");
}

BabbleCtrl.$inject = ['$http', '$scope', 'Babble', 'ArtifactSocket', '$location'];
function BabbleCtrl($http, $scope, Babble, ArtifactSocket, $location) {
  $scope.babblings = Babble.query();
  $scope.addBabble = function() {
    $http.post('babble', {'text': $scope.babbleText});
    $scope.babbleText = '';
  };

  var append = function(b) {
    $scope.babblings.unshift(b);
    $scope.$digest();
  }

  ArtifactSocket.subscribe(append, "BabbleAdd");
}

RecruitCtrl.$inject = ['$http', '$scope', '$location'];
function RecruitCtrl($http, $scope, $location) {
    $scope.save = function(email) {
        $http.post('/cultist', {"email": email}).
            success(function(data){
                $scope.saveError = false;
                $scope.errorMsg = {};
                alert("Cultist '"+email+"' has been recruited. PLEASE TELL THEM that their initial password is '"+data+"'.");
                $location.path('/home');
            }).
            error(function(data){
                $scope.errorMsg = data
                $scope.saveError = true;
            });
    };

    $scope.cancel = function() {
        $location.path('/home');
    };

    $scope.saveError = false;
    $scope.errorMsg = {};
    $scope.email = {};
}

AddGatewayCtrl.$inject = ['$http', '$scope', '$location'];
function AddGatewayCtrl($http, $scope, $location) {
  $scope.modes = [
    {name: "Disabled", source: false, sink: false},
    {name: "Source", source: true, sink: false},
    {name: "Sink", source: false, sink: true},
    {name: "Source + Sink", source: true, sink: true}
  ];
  $scope.cleared = {};
  $scope.saveError = false;
  $scope.passwordRequired = true;

  $scope.save = function(gateway) {
    $http.post('/gateway', gateway).
        success(function(data){
          $location.path('/home');
        }).
        error(function(data){
          $scope.saveError = true;
        });
  };

  $scope.cancel = function() {
    $location.path('/home');
  };

  $scope.gateway = angular.copy($scope.cleared);
}

EditGatewayCtrl.$inject = ['$http', '$scope', '$location', '$routeParams', 'Gateway'];
function EditGatewayCtrl($http, $scope, $location, $routeParams, Gateway) {
  $scope.modes = [
    {name: "Disabled", source: false, sink: false},
    {name: "Source", source: true, sink: false},
    {name: "Sink", source: false, sink: true},
    {name: "Source + Sink", source: true, sink: true}
  ];
  $scope.gateway = Gateway.get({id:$routeParams.gatewayId}, function() {
    var source = $scope.gateway.mode.source
    var sink = $scope.gateway.mode.sink
    if (source && sink) {
      $scope.gateway.mode = $scope.modes[3];
    }
    else if (sink) {
      $scope.gateway.mode = $scope.modes[2];
    }
    else if (source) {
      $scope.gateway.mode = $scope.modes[1];
    }
    else {
      $scope.gateway.mode = $scope.modes[0];
    }
  });
  $scope.cleared = {};
  $scope.saveError = false;
  $scope.passwordRequired = false;

  $scope.save = function(gateway) {
    gateway.$save();
    $location.path('/home');
    /*$http.post('/gateway', gateway).
        success(function(data){
          $location.path('#/home');
        }).
        error(function(data){
          $scope.saveError = true;
        });*/
  };

  $scope.cancel = function() {
    $location.path('/home');
  };
}

