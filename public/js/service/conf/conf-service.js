'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.service.conf.confModule', []);

    app.factory('ConfService', function($http) {
        return {
            get: function(id, callback) {
                $http(PlayRoutes.controllers.conf.ConfController.show(id)).success(callback);
            },
            completer: function(envId, projectId, versionId, callback) {
                $http(PlayRoutes.controllers.conf.ConfController.completer(envId, projectId, versionId)).success(callback);
            },
            getAll: function(envId, vid, callback) {
                $http(PlayRoutes.controllers.conf.ConfController.all(envId, vid)).success(callback);
            },
            getDefaultAll: function(envId, pid, vid, callback) {
                $http(PlayRoutes.controllers.conf.ConfController.defaultAll(envId, pid, vid)).success(callback);
            },
            save: function(conf, callback) {
                $http.post(PlayRoutes.controllers.conf.ConfController.save().url, conf).success(callback)
            },
            update: function(id, conf, callback) {
                $http.put(PlayRoutes.controllers.conf.ConfController.update(id).url, conf).success(callback)
            },
            remove: function(id, callback) {
                $http(PlayRoutes.controllers.conf.ConfController.delete(id)).success(callback);
            },
            copy: function(copy, callback) {
                $http.post(PlayRoutes.controllers.conf.ConfController.copy().url, copy).success(callback)
            }
        }
    });

    // logs
    app.factory('LogService', function($http) {
        return {
            get: function(id, callback) {
                $http(PlayRoutes.controllers.conf.ConfController.log(id)).success(callback);
            },
            getPage: function(confId, page, pageSize, callback) {
                $http(PlayRoutes.controllers.conf.ConfController.logs(confId, page, pageSize)).success(callback);
            },
            count: function(confId, callback) {
                $http(PlayRoutes.controllers.conf.ConfController.logsCount(confId)).success(callback);
            }
        }
    });

});