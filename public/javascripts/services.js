'use strict';

angular.module('ootwServices', ['ngResource']).
    factory('ArtifactLog', function($resource){
      return $resource('artifact/log', {}, {
        query: {method:'GET', params:{}, isArray:true}
      });
    }).
    factory('Gateway', function($resource){
      return $resource('gateway', {}, {
        query: {method:'GET', params:{}, isArray:true}
      })
    }).
    factory('Cloned', function($resource){
      return $resource('clone/history', {}, {
        query: {method:'GET', params:{}, isArray:true}
      })
    }).
    factory('Awaiting', function($resource){
      return $resource('clone/awaiting', {}, {
        query: {method:'GET', params:{}, isArray:true}
      })
    });