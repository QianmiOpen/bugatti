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
    './task/task-controller'
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
        'bugattiApp.controller.task.taskModule'
    ]);

    // Auth
    app.controller('NavCtrl', ['$rootScope', '$scope', '$location', 'Auth', function($rootScope, $scope, $location, Auth) {
        $scope.user = Auth.user;

        $scope.login = function() {
            window.open('/login','千米LDAP登陆','location=yes,left=200,top=100,width=710,height=400,resizable=yes');
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