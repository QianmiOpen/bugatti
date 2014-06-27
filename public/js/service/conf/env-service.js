'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.service.conf.envModule', []);

    app.factory('EnvService', function($http) {
        return {
            get: function(id, callback) {
                $http(PlayRoutes.controllers.conf.EnvController.show(id)).success(callback);
            },
            getPage: function(page, pageSize, callback) {
                $http(PlayRoutes.controllers.conf.EnvController.index(page, pageSize)).success(callback);
            },
            count: function(callback) {
                $http(PlayRoutes.controllers.conf.EnvController.count()).success(callback);
            },
            save: function(env, callback) {
                $http.post(PlayRoutes.controllers.conf.EnvController.save().url, env).success(callback)
            },
            update: function(id, env, callback) {
                $http.put(PlayRoutes.controllers.conf.EnvController.update(id).url, env).success(callback)
            },
            remove: function(id, callback) {
                $http(PlayRoutes.controllers.conf.EnvController.delete(id)).success(callback);
            }
        }
    });

});