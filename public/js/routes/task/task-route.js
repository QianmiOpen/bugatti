'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.task.taskModule', []);

    app.config(['$stateProvider', function($stateProvider) {
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
        })

    }]);

});