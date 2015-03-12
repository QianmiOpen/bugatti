'use strict';

define(['angular'], function(angular){
    var app = angular.module('bugattiApp.service.admin.logsModule', []);

    app.factory('LogsService', function($http) {
        return {
            search: function(logs, page, pageSize, callback) {
                $http.post(PlayRoutes.controllers.admin.LogsController.search(page, pageSize).url, logs).success(callback);
            },
            count: function(logs, callback) {
                $http.post(PlayRoutes.controllers.admin.LogsController.count().url, logs).success(callback);
            }
        };
    });

});