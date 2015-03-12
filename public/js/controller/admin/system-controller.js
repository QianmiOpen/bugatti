'use strict';

define(['angular'], function(angular){
    var app = angular.module('bugattiApp.controller.admin.systemModule', []);

    app.controller('SystemCtrl', ['$scope', function($scope) {

        $scope.ldapAuthentication = false;

        $scope.applyChange = function(ldap) {

        }

    }]);
});

