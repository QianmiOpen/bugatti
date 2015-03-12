'use strict';

define(['angular'], function(angular){
    var app = angular.module('bugattiApp.service.admin.scriptModule', []);

    app.factory('ScriptService', function($http) {
        return {
            refresh: function(callback) {
                $http.put(PlayRoutes.controllers.admin.ScriptController.refresh().url).success(callback);
            }
        }
    });
});