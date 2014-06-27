'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.controller.conf.envModule', []);

    app.controller('EnvCtrl', ['$scope', '$modal', 'EnvService', function($scope, $modal, EnvService) {
        $scope.currentPage = 1;
        $scope.pageSize = 10;

        // count
        EnvService.count(function(data) {
            $scope.totalItems = data;
        });

        // list
        EnvService.getPage(0, $scope.pageSize, function(data) {
            $scope.envs = data;
        });

        // page
        $scope.setPage = function (pageNo) {
            EnvService.getPage(pageNo - 1, $scope.pageSize, function(data) {
                $scope.envs = data;
            });
        };

        // remove
        $scope.delete = function(id, index) {
            var modalInstance = $modal.open({
                templateUrl: 'partials/modal.html',
                controller: function ($scope, $modalInstance) {
                    $scope.ok = function () {
                        EnvService.remove(id, function(state) {
                            $modalInstance.close(state);
                        });
                    };
                    $scope.cancel = function () {
                        $modalInstance.dismiss('cancel');
                    };
                }
            });
            modalInstance.result.then(function(state) {
                if (state !== '0') {
                    $scope.envs.splice(index, 1);
                    EnvService.count(function(num) {
                        $scope.totalItems = num;
                    });
                }
            });
        };

    }]);

    app.controller('EnvShowCtrl', ['$scope', '$stateParams', 'EnvService',
        function($scope, $stateParams, EnvService) {
            EnvService.get($stateParams.id, function(data) {
                $scope.env = data;
            });
        }]);

    app.controller('EnvCreateCtrl', ['$scope', '$stateParams', '$state', 'EnvService', function($scope, $stateParams, $state, EnvService) {
        $scope.env = {level: "unsafe"}
        $scope.saveOrUpdate = function(env) {
            EnvService.save(angular.toJson(env), function(data) {
                if (data.r === 1) {
                    $state.go('^');
                } else if (data.r == 'exist') {
                    $scope.form.name.$invalid = true;
                    $scope.form.name.$error.exists = true;
                }
            });
        };
    }]);

    app.controller('EnvUpdateCtrl', ['$scope', '$stateParams', '$state', 'EnvService', function($scope, $stateParams, $state, EnvService) {
        $scope.saveOrUpdate = function(env) {
            EnvService.update($stateParams.id, angular.toJson(env), function(data) {
                if (data.r === 1) {
                    $state.go('^');
                } else if (data.r == 'exist') {
                    $scope.form.name.$invalid = true;
                    $scope.form.name.$error.exists = true;
                }
            });
        };

        EnvService.get($stateParams.id, function(data) {
            // update form reset
            $scope.master = data;
            $scope.reset = function() {
                $scope.env = angular.copy($scope.master);
            };
            $scope.isUnchanged = function(env) {
                return angular.equals(env, $scope.master);
            };
            $scope.reset();
        });
    }]);
});