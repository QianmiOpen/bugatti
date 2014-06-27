'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.service.conf.projectModule', []);

    app.factory('ProjectService', function($http) {
        return {
            get: function(id, callback) {
                $http(PlayRoutes.controllers.conf.ProjectController.show(id)).success(callback);
            },
            save: function(project, callback) {
                $http.post(PlayRoutes.controllers.conf.ProjectController.save().url, project).success(callback)
            },
            types: function(callback) {
                $http(PlayRoutes.controllers.conf.ProjectController.types()).success(callback);
            }
        }
    });

});