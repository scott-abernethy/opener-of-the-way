'use strict';

ArtifactLogCtrl.$inject = ['$http', '$scope', 'ArtifactLog'];
function ArtifactLogCtrl($http, $scope, ArtifactLog) {
  $scope.log = ArtifactLog.query();
  $scope.artifactSelect = function(artifact) {
    // TODO "Are you sure you want to clone this artifact AGAIN?"
    $http.put('artifact/' + artifact.id + '/touch')
  };
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

AwaitingCtrl.$inject = ['$scope', 'Awaiting'];
function AwaitingCtrl($scope, Awaiting) {
  $scope.awaitings = Awaiting.query();
}

