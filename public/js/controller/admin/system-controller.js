'use strict';

define(['angular'], function(angular){
    var app = angular.module('bugattiApp.controller.admin.systemModule', []);

    app.controller('SystemCtrl', ['$scope', 'growl', 'SystemService',
        function($scope, growl, SystemService) {
        $scope.ldapAuthentication = false;

        SystemService.load(function(data) {
            $scope.ldapAuthentication = data.ldapAuthentication;
            $scope.ldap = data.ldap;
        });

        $scope.applyChange = function(ldap) {
            var settings = {ldapAuthentication: $scope.ldapAuthentication, ldap: ldap}
            SystemService.update(angular.toJson(settings), function(data) {
                if (data.r === 'ok') {
                    growl.addSuccessMessage("修改成功");
                }
            });
        }

    }]);
});

