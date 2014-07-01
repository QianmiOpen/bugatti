'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.controller.conf.projectModule', []);

    app.controller('ProjectCtrl', ['$scope', '$modal', 'ProjectService', function($scope, $modal, ProjectService) {
        $scope.currentPage = 1;
        $scope.pageSize = 10;

        // count
        ProjectService.count(function(data) {
            $scope.totalItems = data;
        });

        // list
        ProjectService.getPage(0, $scope.pageSize, function(data) {
            $scope.projects = data;
        });

        // page
        $scope.setPage = function (pageNo) {
            ProjectService.getPage(pageNo - 1, $scope.pageSize, function(data) {
                $scope.projects = data;
            });
        };

    }]);


    app.controller('ProjectShowCtrl', ['$scope', '$stateParams', '$modal', 'ProjectService',
        function($scope, $stateParams, $modal, ProjectService) {
            ProjectService.get($stateParams.id, function(data) {
                $scope.project = data;
            });

    }]);

    app.controller('ProjectCreateCtrl', ['$scope', '$stateParams', '$state', 'ProjectService', 'TemplateService',
        function($scope, $stateParams, $state, ProjectService, TemplateService) {

            $scope.saveOrUpdate = function(project) {

                project.items = [];
                angular.forEach($scope.items, function(item) {
                    project.items.push({name: item.itemName, value: item.value})
                });

                ProjectService.save(angular.toJson(project), function(data) {
                    if (data.r >= 0) {
                        $state.go('^');
                    } else if (data.r == 'exist') {
                        $scope.form.name.$invalid = true;
                        $scope.form.name.$error.exists = true;
                    }
                });
            };

            // load template all
            TemplateService.all(function(data) {
                $scope.templates = data;
            });

            // template change
            $scope.change = function(x) {
                $scope.items = [];
                if(typeof x === 'undefined') {
                    return;
                }
                TemplateService.items(x, function(data) {
                    $scope.items = data;
                });
            }
    }]);

});
