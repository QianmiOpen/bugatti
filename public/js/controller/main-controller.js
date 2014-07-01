/*global define */

'use strict';

define(['angular',
    './conf/user-controller',
    './conf/env-controller',
    './conf/project-controller',
    './conf/ptype-controller',
    './task/task-controller'
], function(angular) {

    /* Controllers */
    var app = angular.module('bugattiApp.controllers', [
        'bugattiApp.controller.conf.userModule',
        'bugattiApp.controller.conf.envModule',
        'bugattiApp.controller.conf.projectModule',
        'bugattiApp.controller.conf.ptypeModule',
        'bugattiApp.controller.task.taskModule'
    ]);

    // Auth
    app.controller('NavCtrl', ['$rootScope', '$scope', '$location', function($rootScope, $scope, $location) {

    }]);

});