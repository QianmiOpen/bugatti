'use strict';

define(['angular'], function(angular){
    var app = angular.module('bugattiApp.service.logs.logsModule', []);

    app.factory('LogsService', function($http) {
        return {
            search: function(logs, callback) {
                $http.post(PlayRoutes.controllers.logs.LogsController.search().url, logs).success(callback);
            },
            count: function(logs, callback) {
                $http.post(PlayRoutes.controllers.logs.LogsController.count().url, logs).success(callback);
            }
        };
    });

});