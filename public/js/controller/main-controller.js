/*global define */

'use strict';

define(['angular',
    './conf/user-controller',
    './conf/area-controller',
    './conf/env-controller',
    './conf/project-controller',
    './conf/template-controller',
    './conf/conf-controller',
    './conf/relation-controller',
    './conf/system-controller',
    './task/task-controller',
    './logs/logs-controller'
], function(angular) {

    /* Controllers */
    var app = angular.module('bugattiApp.controllers', [
        'bugattiApp.controller.conf.userModule',
        'bugattiApp.controller.conf.areaModule',
        'bugattiApp.controller.conf.envModule',
        'bugattiApp.controller.conf.projectModule',
        'bugattiApp.controller.conf.templateModule',
        'bugattiApp.controller.conf.confModule',
        'bugattiApp.controller.conf.relationModule',
        'bugattiApp.controller.conf.systemModule',
        'bugattiApp.controller.task.taskModule',
        'bugattiApp.controller.logs.logsModule'
    ]);

    // Auth
    app.controller('NavCtrl', ['$rootScope', '$scope', '$location', 'Auth', function($rootScope, $scope, $location, Auth) {
        $scope.user = Auth.user;

        $scope.login = function() {
            window.open('/login','千米LDAP登陆','location=yes,left=200,top=100,width=1020,height=568,resizable=no');
        }

        $scope.logout = function() {
            Auth.logout(function() {
                $location.path('/');
            }, function() {
                $rootScope.error = "Failed to logout";
            });
        };
    }]);

});