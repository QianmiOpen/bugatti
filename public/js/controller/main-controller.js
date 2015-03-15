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
    './home/task-controller'
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
        'bugattiApp.controller.home.taskModule'
    ]);

    // Auth
    app.controller('NavCtrl', ['$rootScope', '$scope', '$location', 'Auth',
        function($rootScope, $scope, $location, Auth) {
        $scope.user = Auth.user;

        $scope.logout = function() {
            Auth.logout(function() {
                $location.path('/');
            }, function() {
                $rootScope.error = "Failed to logout";
            });
        };
    }]);

    app.controller('LoginCtrl', ['$scope', '$location', 'Auth', function($scope, $location, Auth) {
        $scope.login = function(user) {
            Auth.login(user, function() {
                $location.path('/');
            },function(){});
        };
    }]);

    app.controller('UCtrl', ['$scope', '$location', 'Auth', function($scope, $location, Auth) {
        $scope.user = Auth.user;

    }]);
});