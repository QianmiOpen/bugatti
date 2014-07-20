/*global define */

'use strict';

define(['angular',
    './conf/user-routes',
    './conf/area-routes',
    './conf/env-routes',
    './conf/project-routes',
    './conf/template-routes',
    './conf/relation-routes',
    './conf/conf-routes',
    './task/task-route'
], function(angular) {

    /* Services */

// Demonstrate how to register services
// In this case it is a simple value service.
    var app = angular.module('bugattiApp.routes', [
        'bugattiApp.route.conf.userModule',
        'bugattiApp.route.conf.areaModule',
        'bugattiApp.route.conf.envModule',
        'bugattiApp.route.conf.projectModule',
        'bugattiApp.route.conf.templateModule',
        'bugattiApp.route.conf.relationModule',
        'bugattiApp.route.conf.confModule',
        'bugattiApp.route.task.taskModule'
    ]);

    app.config(['$stateProvider', '$urlRouterProvider', '$httpProvider', function($stateProvider, $urlRouterProvider, $httpProvider) {
        $urlRouterProvider.otherwise("/");

        $stateProvider.state('home', {
            url: "/",
            templateUrl:'partials/index.html',
            data: { access: 'anon' }
        });

        $stateProvider.state('conf', {
            url: "/conf",
            templateUrl: "partials/conf.html",
            controller: function($scope, Auth) {
                $scope.user = Auth.user;
            },
            data: { access: 'anon' }
        });

        $stateProvider.state('task', {
            url: "/task",
            templateUrl: "partials/task/task.html",
            controller: "TaskCtrl",
            data: { access: 'task' }
        });

        $stateProvider.state('api', {
            url: "/api",
            templateUrl: "partials/api.html",
            data: { access: 'anon' }
        });

        $stateProvider.state('help', {
            url: "/help",
            templateUrl: "partials/help.html",
            data: { access: 'anon' }
        });

        $stateProvider.state('logs', {
            url: "/logs",
            templateUrl: "partials/logs/logs-index.html",
            controller: "LogsCtrl",
            data: { access: 'anon' }
        });

    }]);

});