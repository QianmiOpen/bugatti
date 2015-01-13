'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.service.conf.spiritModule', []);

    app.factory('SpiritService', function($http) {
        return {
            getAll: function(callback) {
                $http(PlayRoutes.controllers.conf.SpiritController.all()).success(callback);
            },
            get: function(id, callback) {
                $http(PlayRoutes.controllers.conf.SpiritController.get(id)).success(callback);
            },
            refresh: function(id, callback) {
                $http(PlayRoutes.controllers.conf.SpiritController.refresh(id)).success(callback);
            },
            save: function(spirit, callback) {
                $http.post(PlayRoutes.controllers.conf.SpiritController.add().url, spirit).success(callback)
            },
            update: function(id, spirit, callback) {
                $http.put(PlayRoutes.controllers.conf.SpiritController.update(id).url, spirit).success(callback)
            },
            remove: function(id, callback) {
                $http(PlayRoutes.controllers.conf.SpiritController.delete(id)).success(callback);
            }
        }
    });

});