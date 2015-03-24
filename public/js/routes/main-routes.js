/*global define */

'use strict';

define(['angular',
    './admin/user-routes',
    './admin/overview-routes',
    './admin/area-routes',
    './admin/env-routes',
    './admin/spirit-routes',
    './admin/project-routes',
    './admin/template-routes',
    './admin/relation-routes',
    './admin/conf-routes',
    './admin/system-routes',
    './admin/script-routes',
    './home/task-routes',
    './profile/profile-routes'
], function(angular) {

// Demonstrate how to register services
// In this case it is a simple value service.
    var app = angular.module('bugattiApp.routes', [
        'bugattiApp.route.admin.overviewModule',
        'bugattiApp.route.admin.userModule',
        'bugattiApp.route.admin.areaModule',
        'bugattiApp.route.admin.envModule',
        'bugattiApp.route.admin.spiritModule',
        'bugattiApp.route.admin.projectModule',
        'bugattiApp.route.admin.templateModule',
        'bugattiApp.route.admin.relationModule',
        'bugattiApp.route.admin.confModule',
        'bugattiApp.route.admin.systemModule',
        'bugattiApp.route.admin.scriptModule',
        'bugattiApp.route.home.taskModule',
        'bugattiApp.route.profile.profileModule'
    ]);

    app.config(['$stateProvider', '$urlRouterProvider', '$httpProvider', function($stateProvider, $urlRouterProvider, $httpProvider) {
        $urlRouterProvider.otherwise("/");

        $stateProvider.state('home', {
            url: "/",
            templateUrl: "partials/home/task.html",
            controller: "TaskCtrl",
            data: { access: 'user' }
        });

        $stateProvider.state('admin', {
            url: "/admin",
            templateUrl: "partials/admin.html",
            controller: function($scope, $state, Auth) {
                $scope.loginUser = Auth.user;
            },
            data: { access: 'admin' }
        });

        $stateProvider.state('admin.logs', {
            url: "/logs",
            templateUrl: "partials/admin/logs-index.html",
            controller: "LogsCtrl",
            data: { access: 'admin' }
        });

        $stateProvider.state('profile', {
            url: "/profile",
            templateUrl: "partials/profile.html",
            controller: function($scope, Auth) {
                $scope.loginUser = Auth.user;
            },
            data: { access: 'user' }
        });

        $stateProvider.state('u', {
            url: "/u",
            templateUrl: "partials/u.html",
            controller: "UCtrl",
            data: { access: 'user' }
        });

        $stateProvider.state('login', {
            url: "/login",
            templateUrl: "partials/login.html",
            controller: "LoginCtrl",
            data: { access: 'anon' }
        });

        $stateProvider.state('api', {
            url: "/api",
            templateUrl: "partials/api.html",
            controller: function($scope) {
                $scope.app.breadcrumb='API';
            },
            data: { access: 'anon' }
        });

        $stateProvider.state('help', {
            url: "/help",
            templateUrl: "partials/help.html",
            controller: function($scope) {
                $scope.app.breadcrumb='文档';
            },
            data: { access: 'anon' }
        });

        $stateProvider.state('license', {
            url: "/license",
            templateUrl: "partials/license.html",
            controller: function($scope) {
                $scope.app.breadcrumb='LICENSE';
            },
            data: { access: 'anon' }
        });

    }]);

});