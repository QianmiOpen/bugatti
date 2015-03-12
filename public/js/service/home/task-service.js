'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.service.home.taskModule', []);

    app.factory('TaskService',function($http,$q){
        return  {
            getLastTaskStatus: function(envId, projects, callback){
                $http.post(PlayRoutes.controllers.home.TaskController.findLastTaskStatus().url, {'envId': envId, 'projects': projects}).success(callback)
            },
            findLastStatus: function(envId, projectId, clusters, callback){
                $http(PlayRoutes.controllers.home.TaskController.findLastStatus(envId, projectId, clusters)).success(callback)
            },
            createNewTaskQueue: function(taskQueue, callback){
                $http.post(PlayRoutes.controllers.home.TaskController.createNewTaskQueue().url, {'taskQueue': taskQueue}).success(callback)
            },
            removeTaskQueue: function(qid, callback){
                $http(PlayRoutes.controllers.home.TaskController.removeTaskQueue(qid)).success(callback)
            },
            getTemplates: function(scriptVersion, callback){
                $http(PlayRoutes.controllers.home.TaskController.getTemplates(scriptVersion)).success(callback)
            },
            LogReader: function(envId, proId, taskId, callback){
              $http(PlayRoutes.controllers.home.TaskController.logReader(envId, proId, taskId)).success(callback)
            },
            readHeader: function(envId, proId, taskId, byteSize, callback){
                $http(PlayRoutes.controllers.home.TaskController.logHeaderContent(envId, proId, taskId, byteSize)).success(callback)
            },
            forceTerminate: function(envId, projectId, clusterName, callback){
                $http(PlayRoutes.controllers.home.TaskController.forceTerminate(envId, projectId, clusterName)).success(callback)
            },
            findClusters: function(envId, projectId, callback){
                $http(PlayRoutes.controllers.home.TaskController.findClusterByEnv_Project(envId, projectId)).success(callback)
            },
            findHisTasks: function(envId, projectId, callback){
                $http(PlayRoutes.controllers.home.TaskController.findHisTasks(envId, projectId)).success(callback)
            },
            getCatalinaWS: function(envId, callback){
                $http(PlayRoutes.controllers.home.TaskController.findCatalinaWSUrl(envId)).success(callback)
            }
        }
    });
});