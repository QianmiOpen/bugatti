'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.service.task.taskModule', []);

    app.factory('TaskService',function($http,$q){
        return  {
            all: function(page, pageSize, callback){
                $http(PlayRoutes.controllers.task.TaskController.index(page, pageSize)).success(callback);
            },
            findByCondition: function(envId, projectId, page, pageSize, callback){
                $http(PlayRoutes.controllers.task.TaskController.findByCondition(envId, projectId, page, pageSize)).success(callback);
            },
            count: function(envId, projectId, callback) {
                $http(PlayRoutes.controllers.task.TaskController.count(envId, projectId)).success(callback);
            },
            save: function(task, taskMachines, taskProperties, callback) {
                $http.post(PlayRoutes.controllers.task.TaskController.save().url, {'task': task, 'taskMachine': taskMachines, 'taskAttribute': taskProperties}).success(callback)
            },
            find: function(taskId,callback) {
                $http(PlayRoutes.controllers.task.TaskController.find(taskId)).success(callback);
            },
            getOperate: function(){
                var deferred = $q.defer()
                $http(PlayRoutes.controllers.task.TaskController.getOperates()).then(function(data){deferred.resolve(data)});
                return deferred.promise;
            },
            getVersion: function(task, callback){
                $http.post(PlayRoutes.controllers.task.TaskController.getVersions().url, {'task': task}).success(callback)
            },getMachine: function(task, callback){
                $http.post(PlayRoutes.controllers.task.TaskController.getMachines().url, {'task': task}).success(callback)
            },
            getAttribute: function(task, callback){
                $http.post(PlayRoutes.controllers.task.TaskController.getAttributes().url, {'task': task}).success(callback)
            },
            getMinion: function(callback){
                $http(PlayRoutes.controllers.task.TaskController.getMinions(1)).success(callback)
            },
            getLastTaskStatus: function(envId, projects, callback){
                $http.post(PlayRoutes.controllers.task.TaskController.findLastTaskStatus().url, {'envId': envId, 'projects': projects}).success(callback)
            },
            createNewTaskQueue: function(taskQueue, callback){
                $http.post(PlayRoutes.controllers.task.TaskController.createNewTaskQueue().url, {'taskQueue': taskQueue}).success(callback)
            }
        }
    });


});