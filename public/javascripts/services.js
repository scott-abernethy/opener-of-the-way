'use strict';

var publisher = {
  subscribers: {
    any: [] // event type: subscribers
  },
  subscribe: function (fn, type) {
    type = type || 'any';
    if (typeof this.subscribers[type] === "undefined") {
      this.subscribers[type] = [];
    }
    this.subscribers[type].push(fn);
  },
  unsubscribe: function (fn, type) {
    this.visitSubscribers('unsubscribe', fn, type);
  },
  publish: function (publication, type) {
    this.visitSubscribers('publish', publication, type);
  },
  visitSubscribers: function (action, arg, type) {
    var pubtype = type || 'any',
        subscribers = this.subscribers[pubtype] || [],
        i,
        max = subscribers.length;

    for (i = 0; i < max; i += 1) {
      if (action === 'publish') {
        subscribers[i](arg);
      } else {
        if (subscribers[i] === arg) {
          subscribers.splice(i, 1);
        }
      }
    }
  }
};
function makePublisher(o) {
  var i;
  for (i in publisher) {
    if (publisher.hasOwnProperty(i) && typeof publisher[i] === "function") {
      o[i] = publisher[i];
    }
  }
  o.subscribers = {any: []};
};

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
    }).
    factory('ArtifactSocket', function() {
      console.log("created ArtifactSocket");
      var url = "ws://" + location.hostname + ":" + location.port + "/artifact/stream";
      var obj = {
        init : function() {
          var self = this;
          this.connection = new WebSocket(url);

          this.connection.onopen = function() {
            console.log("connected");
          }
          this.connection.onclose = function() {
            console.log("onclose");
          }
          this.connection.onerror = function (error) {
            console.log(error);
          }
          this.connection.onmessage = function(event) {
            console.log("refresh");
            var data = angular.fromJson(event.data);
            self.publish(data.message, data.type);
          }
        },
        sendMessage : function(event) {
          this.connection.send(JSON.stringify(event));
        }
      }
      obj.init();
      makePublisher(obj);
      // could send messages based on the services that need to subscribe
      return obj;
    });