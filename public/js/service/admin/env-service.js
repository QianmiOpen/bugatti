'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.service.admin.envModule', []);

    app.factory('EnvService', function($http) {
        return {
            get: function(id, callback) {
                $http(PlayRoutes.controllers.admin.EnvController.show(id)).success(callback);
            },
            getAll: function(callback) {
                $http(PlayRoutes.controllers.admin.EnvController.all()).success(callback);
            },
            my: function(jobNo, callback) {
                $http(PlayRoutes.controllers.admin.EnvController.my(jobNo)).success(callback);
            },
            getPage: function(page, pageSize, callback) {
                $http(PlayRoutes.controllers.admin.EnvController.index(page, pageSize)).success(callback);
            },
            count: function(callback) {
                $http(PlayRoutes.controllers.admin.EnvController.count()).success(callback);
            },
            save: function(env, callback) {
                $http.post(PlayRoutes.controllers.admin.EnvController.save().url, env).success(callback)
            },
            update: function(id, env, callback) {
                $http.put(PlayRoutes.controllers.admin.EnvController.update(id).url, env).success(callback)
            },
            remove: function(id, callback) {
                $http(PlayRoutes.controllers.admin.EnvController.delete(id)).success(callback);
            },
            allScriptVersion: function(callback) {
                $http(PlayRoutes.controllers.admin.EnvController.allScriptVersion()).success(callback);
            },
            // ------------------------------------------------
            // 环境成员
            // ------------------------------------------------
            member: function(envId, jobNo, callback) {
                $http(PlayRoutes.controllers.admin.EnvController.member(envId, jobNo)).success(callback);
            },
            members: function(envId, callback) {
                $http(PlayRoutes.controllers.admin.EnvController.members(envId)).success(callback);
            },
            saveMember: function(envId, jobNo, callback) {
                $http.post(PlayRoutes.controllers.admin.EnvController.saveMember(envId, jobNo).url).success(callback);
            },
            updateMember: function(memberId, op, callback) {
                $http.put(PlayRoutes.controllers.admin.EnvController.updateMember(memberId, op).url).success(callback);
            }
        }
    });

});