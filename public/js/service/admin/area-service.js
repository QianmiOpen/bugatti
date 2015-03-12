'use strict';

define(['angular'], function(angular){
    var app = angular.module('bugattiApp.service.admin.areaModule', []);

    app.factory('AreaService', function($http) {
        return {
            get: function(id, callback) {
                $http(PlayRoutes.controllers.admin.AreaController.get(id)).success(callback);
            },
            getAll: function(callback) {
                $http(PlayRoutes.controllers.admin.AreaController.all()).success(callback);
            },
            list: function(envId, callback) {
                $http(PlayRoutes.controllers.admin.AreaController.list(envId)).success(callback);
            },
            save: function(area, callback) {
                $http.post(PlayRoutes.controllers.admin.AreaController.save().url, area).success(callback);
            },
            update: function(area, callback) {
                $http.put(PlayRoutes.controllers.admin.AreaController.update().url, area).success(callback);
            },
            delete: function(id, callback) {
                $http(PlayRoutes.controllers.admin.AreaController.delete(id)).success(callback);
            },
            refresh: function(id, callback) {
                $http(PlayRoutes.controllers.admin.AreaController.refresh(id)).success(callback);
            }
        }
    });
});