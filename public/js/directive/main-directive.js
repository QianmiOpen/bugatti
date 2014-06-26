/*global define */

'use strict';

define(['angular'], function(angular) {

    /* Directives */

    var app = angular.module('bugattiApp.directives', []);

    /* 用户权限 */
    app.directive('permission', ['UserService', function(UserService) {
        return {
            restrict: 'E',
            scope: {
                jobNo: '@'
            },
            template: '<ul class="horizonal list-group"><li class="list-group-item" ng-repeat="func in functions">{{func}}</li></ul>',
            link: function($scope, element, attrs) {
                UserService.permissions($scope.jobNo, function(data) {
                    $scope.functions = data.functions;
                });
            }
        }
    }]);


});