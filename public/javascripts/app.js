'use strict';

angular.module('ootw', ['ootwServices']).
    config(['$routeProvider', function ($routeProvider) {
      $routeProvider.
          when('/home', {templateUrl: 'partials/home'}).
          when('/tome', {templateUrl: 'partials/tome'}).
          when('/gateway/add', {templateUrl: 'partials/gateway/add'}).
          otherwise({redirectTo: '/home'});
    }]);