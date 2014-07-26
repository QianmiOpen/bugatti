'use strict';

define(['angular'], function(angular){
    var app = angular.module('bugattiApp.service.conf.systemModule', []);

    app.factory('SystemService', function($http) {
        return {
            refresh: function(callback) {
                $http.put(PlayRoutes.controllers.conf.SystemController.refresh().url).success(callback);
            },
            buildTag: function(callback) {
                $http.post(PlayRoutes.controllers.conf.SystemController.buildTag().url).success(callback);
            }
        }
    });
});