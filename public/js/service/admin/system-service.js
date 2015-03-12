'use strict';

define(['angular'], function(angular){
    var app = angular.module('bugattiApp.service.admin.systemModule', []);


    app.factory('SystemService', function($http) {
        return {
            load: function(callback) {
                $http(PlayRoutes.controllers.admin.SystemController.index()).success(callback);
            },
            update: function(settings, callback) {
                $http.post(PlayRoutes.controllers.admin.SystemController.update().url, settings).success(callback)
            }
        }
    });

});