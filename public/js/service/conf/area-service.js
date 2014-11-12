'use strict';

define(['angular'], function(angular){
    var app = angular.module('bugattiApp.service.conf.areaModule', []);

    app.factory('AreaService', function($http) {
        return {
            get: function(id, callback) {
                $http(PlayRoutes.controllers.conf.AreaController.get(id)).success(callback);
            },
            getAll: function(callback) {
                $http(PlayRoutes.controllers.conf.AreaController.all()).success(callback);
            },
            list: function(envId, callback) {
                $http(PlayRoutes.controllers.conf.AreaController.list(envId)).success(callback);
            },
            save: function(area, callback) {
                $http.post(PlayRoutes.controllers.conf.AreaController.save().url, area).success(callback);
            },
            update: function(area, callback) {
                $http.put(PlayRoutes.controllers.conf.AreaController.update().url, area).success(callback);
            },
            delete: function(id, callback) {
                $http(PlayRoutes.controllers.conf.AreaController.delete(id)).success(callback);
            },
            refresh: function(id, callback) {
                $http(PlayRoutes.controllers.conf.AreaController.refresh(id)).success(callback);
            }
        }
    });
});