'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.service.conf.relationModule', []);

    app.factory('RelationService', function($http) {
        return {
            getPage: function(envId, projectId, page, pageSize, callback) {
                $http(PlayRoutes.controllers.conf.RelationController.index(envId, projectId, null, null, page, pageSize)).success(callback);
            },
            getPageSort: function(envId, projectId, sort, direction, page, pageSize, callback) {
                $http(PlayRoutes.controllers.conf.RelationController.index(envId, projectId, sort, direction, page, pageSize)).success(callback);
            },
            ips: function(envId, callback) {
                $http(PlayRoutes.controllers.conf.RelationController.ips(envId)).success(callback);
            },
            count: function(envId, projectId, callback) {
                $http(PlayRoutes.controllers.conf.RelationController.count(envId, projectId)).success(callback);
            },
            unbind: function(id, callback) {
                $http(PlayRoutes.controllers.conf.RelationController.unbind(id)).success(callback);
            },
            bind: function(relation, callback) {
                $http.post(PlayRoutes.controllers.conf.RelationController.bind().url, relation).success(callback)
            }
        }
    });

});