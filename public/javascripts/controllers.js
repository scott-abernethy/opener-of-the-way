'use strict';

ArtifactLogCtrl.$inject = ['$http', '$scope', 'ArtifactLog', 'ArtifactSocket'];
function ArtifactLogCtrl($http, $scope, ArtifactLog, ArtifactSocket) {
  $scope.log = ArtifactLog.query();
  $scope.artifactSelect = function(artifact) {
    $http.put('artifact/' + artifact.id + '/touch')
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
    $scope.$digest();
  }

  ArtifactSocket.subscribe(update, "ArtifactCloning");
  ArtifactSocket.subscribe(update, "ArtifactCloneFailed");
  ArtifactSocket.subscribe(update, "ArtifactCloned");
  ArtifactSocket.subscribe(update, "ArtifactUpdate");
  ArtifactSocket.subscribe(update, "ArtifactAwaiting");
  ArtifactSocket.subscribe(update, "ArtifactUnawaiting");
}

GatewayCtrl.$inject = ['$http', '$scope', 'Gateway', 'Cultist', 'ArtifactSocket'];
function GatewayCtrl($http, $scope, Gateway, Cultist, ArtifactSocket) {
  var scope = $scope;

  var recalc = function() {
    var open = false;
    for (var i = 0; i < scope.gateways.length; i++) {
      if (scope.gateways[i].open) {
        open = true;
        break;
      }
    }
    if (open) {
      scope.noteClass = "label-warning";
      scope.isNote = true;
      scope.noteTitle = "In Use!";
      scope.noteText = "Please do not mount locally or disconnect network.";
    }
    else if (scope.locked) {
      scope.noteClass = "label-success";
      scope.isNote = true;
      scope.noteTitle = "Locked";
      scope.noteText = "The system will not attempt to access locked gateways, so they are safe to mount locally.";
    }
    else {
      scope.noteClass = "";
      scope.isNote = false;
      scope.noteTitle = "-";
      scope.noteText = "-";
    }
  }

  scope.$watch('gateways', recalc);
  scope.$watch('locked', recalc);

  scope.locked = false;
  scope.gateways = [];
  recalc();

  var gateways = Gateway.query(function() {
    scope.gateways = gateways;
  });
  var cultist = Cultist.get(function() {
    scope.locked = cultist.shut
  });

  scope.lock = function() {
    scope.locked = true
    $http.put('gateway/lock', {"enable": true})
  };
  scope.unlock = function() {
    scope.locked = false
    $http.put('gateway/lock', {"enable": false})
  };
  scope.scour = function() {
    $http.put('gateway/scour')
  };

  var update = function(gs) {
    scope.gateways = gs;
    scope.$digest();

    var cultist = Cultist.get(function() {
      scope.locked = cultist.shut
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

BabbleCtrl.$inject = ['$http', '$scope', 'Babble', 'ArtifactSocket'];
function BabbleCtrl($http, $scope, Babble, ArtifactSocket) {
  $scope.babblings = Babble.query();
  $scope.addBabble = function() {
    $http.post('babble', {'text': $scope.babbleText});
    $scope.babbleText = '';
  };

  var append = function(b) {
    $scope.babblings.unshift(b)
    $scope.$digest();
  }

  ArtifactSocket.subscribe(append, "BabbleAdd");
}

