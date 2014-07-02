/*global define */

'use strict';

define(['angular'], function(angular) {

    /* Directives */

    var app = angular.module('bugattiApp.directives', []);

    /* 用户权限列表展示 */
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

    // 模板名称显示
    app.directive('templateShow', ['TemplateService', function(TemplateService) {
        return {
            restrict: 'E',
            scope: {
                tid: '@'
            },
            template: '<span>{{template.name}}</span>',
            link: function($scope, element, attrs) {
                TemplateService.get($scope.tid, function(data) {
                    $scope.template = data;
                });
            }
        }
    }]);

    // 项目名称显示
    app.directive('projectShow', ['ProjectService', function(ProjectService) {
        return {
            restrict: 'E',
            scope: {
                pid: '@'
            },
            template: '<span>{{project.name}}</span>',
            link: function($scope, element, attrs) {
                ProjectService.get($scope.pid, function(data) {
                    $scope.project = data;
                });
            }
        }
    }]);



});