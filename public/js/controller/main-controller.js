/*global define */

'use strict';

define(['angular',
    './conf/user-controller',
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
        'bugattiApp.controller.conf.envModule',
        'bugattiApp.controller.conf.projectModule',
        'bugattiApp.controller.conf.templateModule',
        'bugattiApp.controller.conf.confModule',
        'bugattiApp.controller.conf.relationModule',
        'bugattiApp.controller.task.taskModule'
    ]);

    // Auth
    app.controller('NavCtrl', ['$rootScope', '$scope', '$location', function($rootScope, $scope, $location) {

    }]);

});