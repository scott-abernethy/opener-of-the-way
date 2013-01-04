'use strict';

ArtifactLogCtrl.$inject = ['$http', '$scope', 'ArtifactLog', 'ArtifactSocket'];
function ArtifactLogCtrl($http, $scope, ArtifactLog, ArtifactSocket) {
  $scope.log = ArtifactLog.query();
  $scope.artifactSelect = function(artifact) {
    // TODO "Are you sure you want to clone this artifact AGAIN?"
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

  ArtifactSocket.subscribe(update, "ArtifactAwaiting");
  ArtifactSocket.subscribe(update, "ArtifactUnawaiting");
}

GatewayCtrl.$inject = ['$scope', 'Gateway'];
function GatewayCtrl($scope, Gateway) {
  $scope.gateways = Gateway.query();
}

ClonedCtrl.$inject = ['$scope', 'Cloned'];
function ClonedCtrl($scope, Cloned) {
  $scope.cloned = Cloned.query();
//  $('li').popover();
}

AwaitingCtrl.$inject = ['$http', '$scope', 'Awaiting', 'ArtifactSocket'];
function AwaitingCtrl($http, $scope, Awaiting, ArtifactSocket) {
  $scope.awaitings = Awaiting.query();
  $scope.artifactSelect = function(artifact) {
    // TODO "Are you sure you want to clone this artifact AGAIN?"
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

  ArtifactSocket.subscribe(awaiting, "ArtifactAwaiting");
  ArtifactSocket.subscribe(unawaiting, "ArtifactUnawaiting");
}

