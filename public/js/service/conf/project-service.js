'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.service.conf.projectModule', []);

    app.factory('ProjectService', function($http) {
        return {
            get: function(id, callback) {
                $http(PlayRoutes.controllers.conf.ProjectController.show(id)).success(callback);
            },
            getAll: function(callback) {
                $http(PlayRoutes.controllers.conf.ProjectController.all()).success(callback);
            },
            getPage: function(my, page, pageSize, callback) {
                $http(PlayRoutes.controllers.conf.ProjectController.index(my, page, pageSize)).success(callback);
            },
            count: function(my, callback) {
                $http(PlayRoutes.controllers.conf.ProjectController.count(my)).success(callback);
            },
            save: function(project, callback) {
                $http.post(PlayRoutes.controllers.conf.ProjectController.save().url, project).success(callback)
            },
            update: function(id, project, callback) {
                $http.put(PlayRoutes.controllers.conf.ProjectController.update(id).url, project).success(callback)
            },
            remove: function(id, callback) {
                $http(PlayRoutes.controllers.conf.ProjectController.delete(id)).success(callback);
            },
            atts: function(pid, callback) {
                $http(PlayRoutes.controllers.conf.ProjectController.atts(pid)).success(callback);
            }
        }
    });

    // version
    app.factory('VersionService', function($http) {
        return {
            get: function(id, callback) {
                $http(PlayRoutes.controllers.conf.VersionController.show(id)).success(callback);
            },
            top: function(pid, callback) {
                $http(PlayRoutes.controllers.conf.VersionController.all(pid)).success(callback);
            },
            getPage: function(pid, page, pageSize, callback) {
                $http(PlayRoutes.controllers.conf.VersionController.index(pid, page, pageSize)).success(callback);
            },
            count: function(pid, callback) {
                $http(PlayRoutes.controllers.conf.VersionController.count(pid)).success(callback);
            },
            save: function(version, callback) {
                $http.post(PlayRoutes.controllers.conf.VersionController.save().url, version).success(callback)
            },
            update: function(id, version, callback) {
                $http.put(PlayRoutes.controllers.conf.VersionController.update(id).url, version).success(callback)
            },
            remove: function(id, callback) {
                $http(PlayRoutes.controllers.conf.VersionController.delete(id)).success(callback);
            }
        }
    });

});