'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.controller.conf.relationModule', []);

    app.controller('RelationCtrl', ['$scope', '$modal', 'RelationService', 'ProjectService', 'EnvService',
        function($scope, $modal, RelationService, ProjectService, EnvService) {
            $scope.currentPage = 1;
            $scope.pageSize = 10;

            // init
            EnvService.getAll(function(data) {
                $scope.envs = data;
            });

            ProjectService.getAll(function(data) {
                $scope.projects = data;
            });

            $scope.env = $scope.project = {};

            // count
            RelationService.count($scope.env.id, $scope.project.id, function(data) {
                $scope.totalItems = data;
            });

            RelationService.getPage($scope.env.id, $scope.project.id, 0, $scope.pageSize, function(data) {
                $scope.relations = data;
            });

            $scope.setPage = function (pageNo) {
                $scope.currentPage = pageNo;
                $scope.ids = {};
                $scope.master = false;

                RelationService.getPage($scope.env.id, $scope.project.id, pageNo - 1, $scope.pageSize, function(data) {
                    $scope.relations = data;
                });
            };

            $scope.select = function() {
                if ($scope.env == null) {$scope.env ={}}
                if ($scope.project == null) {$scope.project ={}}
                RelationService.count($scope.env.id, $scope.project.id, function(data) {
                    $scope.totalItems = data;
                });
                $scope.setPage($scope.currentPage);
            };

            // unbind
            $scope.unbind = function(id, index) {
                var modalInstance = $modal.open({
                    templateUrl: 'partials/modal.html',
                    controller: function ($scope, $modalInstance) {
                        $scope.ok = function () {
                            RelationService.unbind(id, function(state) {
                                $modalInstance.close(state);
                            });
                        };
                        $scope.cancel = function () {
                            $modalInstance.dismiss('cancel');
                        };
                    }
                });
                modalInstance.result.then(function(state) {
                    if (state !== 0) {
                        $scope.setPage($scope.currentPage);
                        RelationService.count($scope.env.id, $scope.project.id, function(num) {
                            $scope.totalItems = num;
                        });
                    }
                });
            };

        }]);


    app.controller('RelationCreateCtrl', ['$scope', '$state', '$modal', 'RelationService', 'ProjectService', 'EnvService',
        function($scope, $state, $modal, RelationService, ProjectService, EnvService) {
            // init
            EnvService.getAll(function(data) {
                $scope.envs = data;
            });

            ProjectService.getAll(function(data) {
                $scope.projects = data;
            });

            $scope.selectEnv = function() {
                var env = $scope.env || {};
                if (env.id != undefined) {
                    $scope.load = true;
                    RelationService.ips(env.id, function(data) {
                        $scope.ips = data;
                        $scope.load = false;
                    })
                }
            };


            // insert
            $scope.save = function() {
                var relation = { ids: []};

                angular.forEach($scope.ck_ips, function(value, key) {
                    if (typeof value === 'boolean' && value === true) {
                        relation.ids.push(key);
                    }
                });
                $scope.ips.$error = false;
                if (relation.ids.length < 1) {
                    $scope.ips.$error = true;
                    return;
                }

                relation.envId = $scope.env.id;
                relation.projectId = $scope.project.id;

                RelationService.bind(angular.toJson(relation), function(data) {
                    if (data !== 0) $state.go("^");
                });
            };
    }]);

});