'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.controller.conf.projectModule', []);

    app.controller('ProjectCtrl', ['$scope', '$modal', 'ProjectService', function($scope, $modal, ProjectService) {
        $scope.currentPage = 1;
        $scope.pageSize = 10;
        
    }]);


    app.controller('ProjectShowCtrl', ['$scope', '$stateParams', '$modal', 'ProjectService',
        function($scope, $stateParams, $modal, ProjectService) {
            ProjectService.get($stateParams.id, function(data) {
                $scope.project = data;
            });

    }]);

    app.controller('ProjectCreateCtrl', ['$scope', '$stateParams', '$state', 'ProjectService',
        function($scope, $stateParams, $state, ProjectService) {

            $scope.saveOrUpdate = function(project) {

                ProjectService.save(angular.toJson(project), function(data) {
                    if (data.r === 1) {
                        $state.go('^');
                    } else if (data.r == 'exist') {
                        $scope.form.name.$invalid = true;
                        $scope.form.name.$error.exists = true;
                    }
                });
            };

            ProjectService.types(function(data) {
                $scope.types = data;
            });
    }]);

});
