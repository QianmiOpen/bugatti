'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.service.admin.spiritModule', []);

    app.factory('SpiritService', function($http) {
        return {
            getAll: function(callback) {
                $http(PlayRoutes.controllers.admin.SpiritController.all()).success(callback);
            },
            get: function(id, callback) {
                $http(PlayRoutes.controllers.admin.SpiritController.get(id)).success(callback);
            },
            refresh: function(id, callback) {
                $http(PlayRoutes.controllers.admin.SpiritController.refresh(id)).success(callback);
            },
            save: function(spirit, callback) {
                $http.post(PlayRoutes.controllers.admin.SpiritController.add().url, spirit).success(callback)
            },
            update: function(id, spirit, callback) {
                $http.put(PlayRoutes.controllers.admin.SpiritController.update(id).url, spirit).success(callback)
            },
            remove: function(id, callback) {
                $http(PlayRoutes.controllers.admin.SpiritController.delete(id)).success(callback);
            }
        }
    });

});