'use strict';

define(['angular'], function(angular){
    var app = angular.module('bugattiApp.controller.conf.areaModule', []);

    app.controller('AreaCtrl', ['$scope', '$modal', '$window', 'AreaService', function($scope, $modal, $window, AreaService){
        AreaService.getAll(function(data){
           $scope.areas = data;
        });

        $scope.delete = function(id, index) {
            var modalInstance = $modal.open({
                templateUrl: "partials/modal.html",
                controller: function ($scope, $modalInstance) {
                    $scope.ok = function() {
                        AreaService.delete(id, function(data) {
                            $modalInstance.close(data);
                        });
                    };
                    $scope.cancel = function() {
                        $modalInstance.dismiss("cancel")
                    };
                }
            });
            modalInstance.result.then(function(data) {
                $scope.areas.splice(index, 1);
            });
        };

        $scope.refresh = function(id, index) {
            var modalInstance = $modal.open({
                templateUrl: "partials/modal.html",
                controller: function ($scope, $modalInstance) {
                    $scope.ok = function() {
                        AreaService.refresh(id, function(data) {
                            $modalInstance.close(data);
                        });
                    };
                    $scope.cancel = function() {
                        $modalInstance.dismiss("cancel")
                    };
                }
            });
            modalInstance.result.then(function(data) {
                $scope.areas[index] = data;
            });
        }
    }]);

    app.controller("AreaCreateCtrl", ["$scope", "$stateParams", "$state", "AreaService", function($scope, $stateParams, $state, AreaService) {
        $scope.area = {}
        $scope.saveOrUpdate = function(area) {
            AreaService.save(angular.toJson(area), function(data) {
                if (data.r === 'exist' && data.u === '1') {
                    $scope.form.name.$invalid = true;
                    $scope.form.name.$error.exists = true;
                } else if (data.r === 'exist' && data.u === '2') {
                    $scope.form.syndicName.$invalid = true;
                    $scope.form.syndicName.$error.exists = true;
                } else {
                    $state.go('^');
                }
            });
        }
    }]);

    app.controller("AreaUpdateCtrl", ["$scope", "$stateParams", "$state", "AreaService", function($scope, $stateParams, $state, AreaService) {
        $scope.saveOrUpdate = function(area) {
            area.id = $stateParams.id;

            // reset error tips
            $scope.form.name.$error.exists = false;
            $scope.form.syndicName.$error.exists = false;

            AreaService.update(angular.toJson(area), function(data) {
                if (data.r === 'exist' && data.u === '1') {
                    $scope.form.name.$invalid = true;
                    $scope.form.name.$error.exists = true;
                } else if (data.r === 'exist' && data.u === '2') {
                    $scope.form.syndicName.$invalid = true;
                    $scope.form.syndicName.$error.exists = true;
                } else {
                    $state.go('^');
                }
            });
        };

        AreaService.get($stateParams.id, function(data) {
            $scope.master = data;
            $scope.reset = function() {
                $scope.area = angular.copy($scope.master);
            }
            $scope.isUnchanged = function(area) {
                return angular.equals(area, $scope.master);
            }
            $scope.reset();
        })
    }]);
});

