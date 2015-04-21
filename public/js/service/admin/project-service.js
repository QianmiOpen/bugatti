'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.service.admin.projectModule', []);

    app.factory('ProjectService', function($http) {
        return {
            get: function(id, callback) {
                $http(PlayRoutes.controllers.admin.ProjectController.show(id)).success(callback);
            },
            getAll: function(callback) {
                $http(PlayRoutes.controllers.admin.ProjectController.all()).success(callback);
            },
            my: function(jobNo, callback) {
                $http(PlayRoutes.controllers.admin.ProjectController.my(jobNo)).success(callback);
            },
            getExceptSelf: function(id, callback) {
                $http(PlayRoutes.controllers.admin.ProjectController.allExceptSelf(id)).success(callback);
            },
            getAuth: function(envId, callback) {
                $http(PlayRoutes.controllers.admin.ProjectController.showAuth(envId)).success(callback);
            },
            getPage: function(projectName, templateId, page, pageSize, callback) {
                $http(PlayRoutes.controllers.admin.ProjectController.index(projectName, templateId, page, pageSize)).success(callback);
            },
            count: function(projectName, templateId, callback) {
                $http(PlayRoutes.controllers.admin.ProjectController.count(projectName, templateId)).success(callback);
            },
            save: function(project, callback) {
                $http.post(PlayRoutes.controllers.admin.ProjectController.save().url, project).success(callback)
            },
            update: function(projectId, envId, project, callback) {
                $http.put(PlayRoutes.controllers.admin.ProjectController.update(projectId, envId).url, project).success(callback)
            },
            remove: function(id, callback) {
                $http(PlayRoutes.controllers.admin.ProjectController.delete(id)).success(callback);
            },
            // ------------------------------------------------
            atts: function(projectId, callback) {
                $http(PlayRoutes.controllers.admin.ProjectController.atts(projectId)).success(callback);
            },
            vars: function(projectId, envId, callback) {
                $http(PlayRoutes.controllers.admin.ProjectController.vars(projectId, envId)).success(callback);
            },
            // ------------------------------------------------
            // 项目成员
            // ------------------------------------------------
            member: function(projectId, jobNo, callback) {
                $http(PlayRoutes.controllers.admin.ProjectController.member(projectId, jobNo)).success(callback);
            },
            members: function(projectId, callback) {
                $http(PlayRoutes.controllers.admin.ProjectController.members(projectId)).success(callback);
            },
            saveMember: function(projectId, jobNo, callback) {
                $http.post(PlayRoutes.controllers.admin.ProjectController.saveMember(projectId, jobNo).url).success(callback);
            },
            updateMember: function(memberId, op, callback) {
                $http.put(PlayRoutes.controllers.admin.ProjectController.updateMember(memberId, op).url).success(callback);
            },
            addCluster: function(rel, callback){
                $http.post(PlayRoutes.controllers.admin.ProjectController.addCluster().url, rel).success(callback)
            },
            removeCluster: function(cid, callback){
                $http.delete(PlayRoutes.controllers.admin.ProjectController.removeCluster(cid).url).success(callback)
            }
        }
    });

    // version
    app.factory('VersionService', function($http) {
        return {
            get: function(id, callback) {
                $http(PlayRoutes.controllers.admin.VersionController.show(id)).success(callback);
            },
            getVersions: function(pid, eid, callback) {
                $http(PlayRoutes.controllers.admin.VersionController.getVersions(pid, eid)).success(callback)
            },
            top: function(projectId, callback) {
                $http(PlayRoutes.controllers.admin.VersionController.all(projectId)).success(callback);
            },
            getNexusVersions: function(projectId, callback){
                $http(PlayRoutes.controllers.admin.VersionController.nexusVersions(projectId)).success(callback)
            },
            getPage: function(vs, projectId, page, pageSize, callback) {
                $http(PlayRoutes.controllers.admin.VersionController.index(vs, projectId, page, pageSize)).success(callback);
            },
            count: function(vs, projectId, callback) {
                $http(PlayRoutes.controllers.admin.VersionController.count(vs, projectId)).success(callback);
            },
            save: function(version, callback) {
                $http.post(PlayRoutes.controllers.admin.VersionController.save().url, version).success(callback)
            },
            update: function(id, version, callback) {
                $http.put(PlayRoutes.controllers.admin.VersionController.update(id).url, version).success(callback)
            },
            remove: function(id, callback) {
                $http(PlayRoutes.controllers.admin.VersionController.delete(id)).success(callback);
            }
        }
    });

    // Dependency
    app.factory('DependencyService', function($http){
       return {
           get: function(id, callback) {
               $http(PlayRoutes.controllers.admin.DependencyController.show(id)).success(callback);
           },
           removeDependency: function(pid, cid, callback){
               $http(PlayRoutes.controllers.admin.DependencyController.removeDependency(pid, cid)).success(callback);
           },
           addDependency: function(p, c, callback){
               $http.post(PlayRoutes.controllers.admin.DependencyController.addDependency().url, {'parent': p, 'child': c}).success(callback);
           },
           changeTemplateProject: function(parentId, oldId, newId, callback){
               $http.post(PlayRoutes.controllers.admin.DependencyController.updateTemplateProject().url, {'parentId': parentId, 'oldId': oldId, 'newId': newId}).success(callback);
           }
       }
    });

});