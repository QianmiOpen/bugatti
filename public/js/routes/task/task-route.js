'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.task.taskModule', []);

    app.config(['$stateProvider', function($stateProvider) {

        $stateProvider.state('task2.list',{
            url: "/list/:eid?txt&top"
        });

        $stateProvider.state('task2.list.info',{
            url: "/info/:pid",
            views:{
                "task-info@task2":{
                    templateUrl: "partials/task/task2-info.html",
                    controller: "Task2InfoCtrl"
                }
            }
        });

        //task create
        $stateProvider.state('task.create',{
            url: "/create",
            views:{
                "@":{
                    templateUrl: "partials/task/task-new.html",
                    controller: "TaskCreateCtrl"
                }
            }

        });

        $stateProvider.state('task.shortcut',{
            url: "/shortcut?operateId&preTaskId",
            views:{
                "@":{
                    templateUrl:"partials/task/task-new.html",
                    controller: "TaskCreateCtrl"
                }
            }
        });

        $stateProvider.state('task.log',{
            url: "/log/:taskId",
            views:{
                "@":{
                    templateUrl:"partials/task/task-log.html",
                    controller:"TaskLogCtrl"
                }
            }
        });

        $stateProvider.state('task.info',{
            url: "/task/info/:taskId",
            views:{
                "@":{
                    templateUrl: "partials/task/task-info.html",
                    controller: "TaskInfoCtrl"
                }
            }
        });

        $stateProvider.state('task.queue',{
            url: "/queue?envId&projectId",
            views:{
                "@":{
                    templateUrl: "partials/task/task-queue.html",
                    controller: "TaskQueueCtrl"
                }
            }
        });

    }]);

});