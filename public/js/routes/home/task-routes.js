'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.home.taskModule', []);

    app.config(['$stateProvider', function($stateProvider) {

        $stateProvider.state('home.list',{
            url: "list/:eid?txt&top"
        });

        $stateProvider.state('home.list.info',{
            url: "/info/:pid",
            views:{
                "task-info@home":{
                    templateUrl: "partials/home/task-info.html",
                    controller: "TaskInfoCtrl"
                }
            }
        });

    }]);

});