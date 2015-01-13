'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.service.conf.spiritModule', []);

    app.factory('SpiritService', function($http) {
        return {
            getAll: function(callback) {
                $http(PlayRoutes.controllers.conf.SpiritController.all()).success(callback);
            },
            refresh: function(id, callback) {
                $http(PlayRoutes.controllers.conf.SpiritController.refresh(id)).success(callback);
            }
        }
    });

});