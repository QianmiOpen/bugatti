/*global define */

'use strict';

define(['angular',
    './conf/user-controller'
], function(angular) {

    /* Controllers */
    var app = angular.module('bugattiApp.controllers', [
        'bugattiApp.controller.conf.userModule'
    ]);


    // Auth
    app.controller('NavCtrl', ['$rootScope', '$scope', '$location', function($rootScope, $scope, $location) {

    }]);


});