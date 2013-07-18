'use strict';

angular.module('ootw', ['ootwDirectives', 'ootwServices', 'infinite-scroll']).
    config(['$routeProvider', function ($routeProvider) {
      $routeProvider.
          when('/home', {templateUrl: 'partials/home'}).
          when('/tome', {templateUrl: 'partials/tome'}).
          when('/recruit', {templateUrl: 'partials/recruit', controller: RecruitCtrl}).
          when('/passwd', {templateUrl: 'partials/passwd', controller: ChangePasswordCtrl}).
          when('/gateway/add', {templateUrl: 'partials/gateway/add', controller: AddGatewayCtrl}).
          when('/gateway/:gatewayId/edit', {templateUrl: 'partials/gateway/edit', controller: EditGatewayCtrl}).
          otherwise({redirectTo: '/home'});
    }]);