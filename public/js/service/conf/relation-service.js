'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.service.conf.relationModule', []);

    app.factory('RelationService', function($http) {
        return {
            get: function(id, callback) {
                $http(PlayRoutes.controllers.conf.RelationController.show(id)).success(callback);
            },
            getPage: function(ip, envId, projectId, page, pageSize, callback) {
                $http(PlayRoutes.controllers.conf.RelationController.index(ip, envId, projectId, null, null, page, pageSize)).success(callback);
            },
            getPageSort: function(ip, envId, projectId, sort, direction, page, pageSize, callback) {
                $http(PlayRoutes.controllers.conf.RelationController.index(ip, envId, projectId, sort, direction, page, pageSize)).success(callback);
            },
            ips: function(envId, callback) {
                $http(PlayRoutes.controllers.conf.RelationController.ips(envId)).success(callback);
            },
            count: function(ip, envId, projectId, callback) {
                $http(PlayRoutes.controllers.conf.RelationController.count(ip, envId, projectId)).success(callback);
            },
            unbind: function(id, callback) {
                $http(PlayRoutes.controllers.conf.RelationController.unbind(id)).success(callback);
            },
            bind: function(relation, callback) {
                $http.post(PlayRoutes.controllers.conf.RelationController.bind().url, relation).success(callback)
            },
            update: function(id, relation, callback) {
                $http.put(PlayRoutes.controllers.conf.RelationController.update(id).url, relation).success(callback)
            }
        }
    });

});