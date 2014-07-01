'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.service.task.taskModule', []);

    app.factory('TaskService',function($http,$q){
        return  {
            getVersions: function(pid, eid, callback){
                $http(PlayRoutes.controllers.task.TaskController.getVersions(pid, eid)).success(callback)
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