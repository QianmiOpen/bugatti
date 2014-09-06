'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.service.conf.envModule', []);

    app.factory('EnvService', function($http) {
        return {
            get: function(id, callback) {
                $http(PlayRoutes.controllers.conf.EnvController.show(id)).success(callback);
            },
            getAll: function(callback) {
                $http(PlayRoutes.controllers.conf.EnvController.all()).success(callback);
            },
            getAuth: function(callback) {
                $http(PlayRoutes.controllers.conf.EnvController.showAuth()).success(callback);
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
            },
            allScriptVersion: function(callback) {
                $http(PlayRoutes.controllers.conf.EnvController.allScriptVersion()).success(callback);
            },
            // ------------------------------------------------
            // 环境成员
            // ------------------------------------------------
            member: function(envId, jobNo, callback) {
                $http(PlayRoutes.controllers.conf.EnvController.member(envId, jobNo)).success(callback);
            },
            members: function(envId, callback) {
                $http(PlayRoutes.controllers.conf.EnvController.members(envId)).success(callback);
            },
            saveMember: function(envId, jobNo, callback) {
                $http.post(PlayRoutes.controllers.conf.EnvController.saveMember(envId, jobNo).url).success(callback);
            },
            deleteMember: function(envId, memberId, callback) {
                $http(PlayRoutes.controllers.conf.EnvController.deleteMember(envId, memberId)).success(callback);
            }
        }
    });

});