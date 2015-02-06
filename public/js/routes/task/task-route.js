'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.task.taskModule', []);

    app.config(['$stateProvider', function($stateProvider) {

        $stateProvider.state('task.list',{
            url: "/list/:eid?txt&top"
        });

        $stateProvider.state('task.list.info',{
            url: "/info/:pid",
            views:{
                "task-info@task":{
                    templateUrl: "partials/task/task-info.html",
                    controller: "TaskInfoCtrl"
                }
            }
        });

    }]);

});