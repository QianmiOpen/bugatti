'use strict';

define(['angular'], function(angular){
    var app = angular.module('bugattiApp.controller.admin.overviewModule', []);

    app.controller('OverviewCtrl', ['$scope', 'ProjectService', 'EnvService', 'UserService',
        function($scope, ProjectService, EnvService, UserService) {
            $scope.app.breadcrumb='综合概况';
        ProjectService.count('', undefined, function(data) {
            $scope.projectCount = data;
        });

        EnvService.count(function(data) {
            $scope.envCount = data;
        });

        UserService.count('', function(data) {
            $scope.userCount = data;
        });
    }]);

});
