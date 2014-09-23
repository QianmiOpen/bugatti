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
            getExceptSelf: function(id, callback) {
                $http(PlayRoutes.controllers.conf.ProjectController.allExceptSelf(id)).success(callback);
            },
            getAuth: function(callback) {
                $http(PlayRoutes.controllers.conf.ProjectController.showAuth()).success(callback);
            },
            getPage: function(projectName, my, page, pageSize, callback) {
                $http(PlayRoutes.controllers.conf.ProjectController.index(projectName, my, page, pageSize)).success(callback);
            },
            count: function(projectName, my, callback) {
                $http(PlayRoutes.controllers.conf.ProjectController.count(projectName, my)).success(callback);
            },
            save: function(project, callback) {
                $http.post(PlayRoutes.controllers.conf.ProjectController.save().url, project).success(callback)
            },
            update: function(projectId, envId, project, callback) {
                $http.put(PlayRoutes.controllers.conf.ProjectController.update(projectId, envId).url, project).success(callback)
            },
            remove: function(id, callback) {
                $http(PlayRoutes.controllers.conf.ProjectController.delete(id)).success(callback);
            },
            // ------------------------------------------------
            atts: function(projectId, callback) {
                $http(PlayRoutes.controllers.conf.ProjectController.atts(projectId)).success(callback);
            },
            vars: function(projectId, envId, callback) {
                $http(PlayRoutes.controllers.conf.ProjectController.vars(projectId, envId)).success(callback);
            },
            // ------------------------------------------------
            // 项目成员
            // ------------------------------------------------
            member: function(projectId, jobNo, callback) {
                $http(PlayRoutes.controllers.conf.ProjectController.member(projectId, jobNo)).success(callback);
            },
            members: function(projectId, callback) {
                $http(PlayRoutes.controllers.conf.ProjectController.members(projectId)).success(callback);
            },
            saveMember: function(projectId, jobNo, callback) {
                $http.post(PlayRoutes.controllers.conf.ProjectController.saveMember(projectId, jobNo).url).success(callback);
            },
            updateMember: function(memberId, op, callback) {
                $http.put(PlayRoutes.controllers.conf.ProjectController.updateMember(memberId, op).url).success(callback);
            },
            addCluster: function(eid, pid, callback){
                $http.put(PlayRoutes.controllers.conf.ProjectController.addCluster(eid, pid).url).success(callback)
            },
            removeCluster: function(cid, callback){
                $http.delete(PlayRoutes.controllers.conf.ProjectController.removeCluster(cid).url).success(callback)
            }
        }
    });

    // version
    app.factory('VersionService', function($http) {
        return {
            get: function(id, callback) {
                $http(PlayRoutes.controllers.conf.VersionController.show(id)).success(callback);
            },
            top: function(projectId, callback) {
                $http(PlayRoutes.controllers.conf.VersionController.all(projectId)).success(callback);
            },
            getNexusVersions: function(projectId, callback){
                $http(PlayRoutes.controllers.conf.VersionController.nexusVersions(projectId)).success(callback)
            },
            getPage: function(projectId, page, pageSize, callback) {
                $http(PlayRoutes.controllers.conf.VersionController.index(projectId, page, pageSize)).success(callback);
            },
            count: function(projectId, callback) {
                $http(PlayRoutes.controllers.conf.VersionController.count(projectId)).success(callback);
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

    // Dependency
    app.factory('DependencyService', function($http){
       return {
           get: function(id, callback) {
               $http(PlayRoutes.controllers.conf.DependencyController.show(id)).success(callback);
           },
           removeDependency: function(pid, cid, callback){
               $http(PlayRoutes.controllers.conf.DependencyController.removeDependency(pid, cid)).success(callback);
           },
           addDependency: function(p, c, callback){
               $http.post(PlayRoutes.controllers.conf.DependencyController.addDependency().url, {'parent': p, 'child': c}).success(callback);
            }
       }
    });

});