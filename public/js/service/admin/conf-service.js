'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.service.admin.confModule', []);

    app.factory('ConfService', function($http) {
        return {
            get: function(id, callback) {
                $http(PlayRoutes.controllers.admin.ConfController.show(id)).success(callback);
            },
            completer: function(envId, projectId, versionId, callback) {
                $http(PlayRoutes.controllers.admin.ConfController.completer(envId, projectId, versionId)).success(callback);
            },
            getAll: function(envId, pid, vid, callback) {
                $http(PlayRoutes.controllers.admin.ConfController.all(envId, pid, vid)).success(callback);
            },
            save: function(conf, callback) {
                $http.post(PlayRoutes.controllers.admin.ConfController.save().url, conf).success(callback)
            },
            update: function(id, conf, callback) {
                $http.put(PlayRoutes.controllers.admin.ConfController.update(id).url, conf).success(callback)
            },
            remove: function(id, callback) {
                $http(PlayRoutes.controllers.admin.ConfController.delete(id)).success(callback);
            },
            copy: function(copy, callback) {
                $http.post(PlayRoutes.controllers.admin.ConfController.copy().url, copy).success(callback)
            }
        }
    });

    // logs
    app.factory('LogService', function($http) {
        return {
            get: function(id, callback) {
                $http(PlayRoutes.controllers.admin.ConfController.log(id)).success(callback);
            },
            getPage: function(confId, page, pageSize, callback) {
                $http(PlayRoutes.controllers.admin.ConfController.logs(confId, page, pageSize)).success(callback);
            },
            count: function(confId, callback) {
                $http(PlayRoutes.controllers.admin.ConfController.logsCount(confId)).success(callback);
            }
        }
    });

});