'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.service.task.taskModule', []);

    app.factory('TaskService',function($http,$q){
        return  {
            getLastTaskStatus: function(envId, projects, callback){
                $http.post(PlayRoutes.controllers.task.TaskController.findLastTaskStatus().url, {'envId': envId, 'projects': projects}).success(callback)
            },
            findLastStatus: function(envId, projectId, clusters, callback){
                $http(PlayRoutes.controllers.task.TaskController.findLastStatus(envId, projectId, clusters)).success(callback)
            },
            createNewTaskQueue: function(taskQueue, callback){
                $http.post(PlayRoutes.controllers.task.TaskController.createNewTaskQueue().url, {'taskQueue': taskQueue}).success(callback)
            },
            removeTaskQueue: function(qid, callback){
                $http(PlayRoutes.controllers.task.TaskController.removeTaskQueue(qid)).success(callback)
            },
            getTemplates: function(scriptVersion, callback){
                $http(PlayRoutes.controllers.task.TaskController.getTemplates(scriptVersion)).success(callback)
            },
            readHeader: function(taskId, byteSize, callback){
                $http(PlayRoutes.controllers.task.TaskController.taskLogFirst(taskId, byteSize)).success(callback)
            },
            forceTerminate: function(envId, projectId, clusterName, callback){
                $http(PlayRoutes.controllers.task.TaskController.forceTerminate(envId, projectId, clusterName)).success(callback)
            },
            findClusters: function(envId, projectId, callback){
                $http(PlayRoutes.controllers.task.TaskController.findClusterByEnv_Project(envId, projectId)).success(callback)
            },
            findHisTasks: function(envId, projectId, callback){
                $http(PlayRoutes.controllers.task.TaskController.findHisTasks(envId, projectId)).success(callback)
            },
            getCatalinaWS: function(envId, callback){
                $http(PlayRoutes.controllers.task.TaskController.findCatalinaWSUrl(envId)).success(callback)
            }
        }
    });
});