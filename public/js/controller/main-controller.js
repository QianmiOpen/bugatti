/*global define */

'use strict';

define(['angular',
    './admin/overview-controller',
    './admin/user-controller',
    './admin/area-controller',
    './admin/env-controller',
    './admin/spirit-controller',
    './admin/project-controller',
    './admin/template-controller',
    './admin/conf-controller',
    './admin/relation-controller',
    './admin/system-controller',
    './admin/script-controller',
    './admin/logs-controller',
    './home/task-controller',
    './profile/profile-controller'
], function(angular) {

    /* Controllers */
    var app = angular.module('bugattiApp.controllers', [
        'bugattiApp.controller.admin.overviewModule',
        'bugattiApp.controller.admin.userModule',
        'bugattiApp.controller.admin.areaModule',
        'bugattiApp.controller.admin.envModule',
        'bugattiApp.controller.admin.spiritModule',
        'bugattiApp.controller.admin.projectModule',
        'bugattiApp.controller.admin.templateModule',
        'bugattiApp.controller.admin.confModule',
        'bugattiApp.controller.admin.relationModule',
        'bugattiApp.controller.admin.systemModule',
        'bugattiApp.controller.admin.scriptModule',
        'bugattiApp.controller.admin.logsModule',
        'bugattiApp.controller.home.taskModule',
        'bugattiApp.controller.profile.profileModule'
    ]);

    // Auth
    app.controller('NavCtrl', ['$rootScope', '$scope', '$state', 'Auth',
        function($rootScope, $scope, $state, Auth) {
            $scope.loginUser = Auth.user;

            $scope.logout = function() {
                Auth.logout(function() {
                    $state.go('login');
                }, function() {
                    $rootScope.error = "Failed to logout";
                });
            };
    }]);

    app.controller('LoginCtrl', ['$scope', '$location', 'Auth', function($scope, $location, Auth) {
        $scope.app.breadcrumb='登录';

        $scope.login = function(user) {
            Auth.login(user, function() {
                $location.path('/');
            },function(){});
        };
    }]);

    app.controller('UCtrl', ['$scope', '$location', '$filter', 'Auth', 'EnvService', 'ProjectService', 'LogsService', 'UserService',
        function($scope, $location, $filter, Auth, EnvService, ProjectService, LogsService, UserService) {
            $scope.app.breadcrumb='用户中心';

            $scope.loginUser = Auth.user;

            EnvService.my($scope.loginUser.username, function(data) {
                $scope.envs = data;
            });

            ProjectService.my($scope.loginUser.username, function(data) {
                $scope.projects = data;
            });

            UserService.get($scope.loginUser.username, function(data) {
                $scope.user = data;
            });

            // logs
            $scope.start = 0;
            $scope.pageSize = 20;

            var date = new Date();
            $scope.logs = {
                startTime: $filter('date')(date.setYear(date.getFullYear() - 1), 'yyyy-MM-dd') + ' 00:00:00',
                endTime: $filter('date')(new Date(), "yyyy-MM-dd") + ' 23:59:59',
                jobNo: $scope.loginUser.username
            };

            LogsService.search(angular.toJson($scope.logs), $scope.start, $scope.pageSize, function(data) {
                $scope.logData = data;
            });


        }]);
});