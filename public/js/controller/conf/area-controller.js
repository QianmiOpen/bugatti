'use strict';

define(['angular'], function(angular){
    var app = angular.module('bugattiApp.controller.conf.areaModule', []);

    app.controller('AreaCtrl', ['$scope', '$modal', 'AreaService', function($scope, $modal, AreaService){
        AreaService.getAll(function(data){
           $scope.areas = data;
        });

        $scope.delete = function(id, index) {
            var modalInstance = $modal.open({
                templateUrl: "partials/modal.html",
                controller: function ($scope, $modalInstance) {
                    $scope.ok = function() {
                        AreaService.delete(id, function(state) {
                            $modalInstance.close(state);
                        });
                    };
                    $scope.cancel = function() {
                        $modalInstance.dismiss("cancel")
                    };
                }
            });
            modalInstance.result.then(function(state) {
                if (state.r === 'ok') {
                    $scope.areas.splice(index, 1);
                }
            });
        };

        $scope.refresh = function(id, index) {
            var modalInstance = $modal.open({
                templateUrl: "partials/modal.html",
                controller: function ($scope, $modalInstance) {
                    $scope.ok = function() {
                        AreaService.refresh(id, function(state) {
                            $modalInstance.close(state);
                        });
                    };
                    $scope.cancel = function() {
                        $modalInstance.dismiss("cancel")
                    };
                }
            });
            modalInstance.result.then(function(state) {
                if (state.r === 'ok') {
                    $scope.areas[index] = state.msg;
                }
            });
        }
    }]);

    app.controller("AreaCreateCtrl", ["$scope", "$stateParams", "$state", "AreaService", function($scope, $stateParams, $state, AreaService) {
        $scope.area = {}
        $scope.saveOrUpdate = function(area) {
            AreaService.save(angular.toJson(area), function(data) {
               if (data.r === 'ok') {
                   $state.go("^");
               } else if (data.r === 'exist') {
                   $scope.form.name.$invalid = true;
                   $scope.form.name.$error.exists = true;
               }
            });
        }
    }]);

    app.controller("AreaUpdateCtrl", ["$scope", "$stateParams", "$state", "AreaService", function($scope, $stateParams, $state, AreaService) {
        $scope.saveOrUpdate = function(area) {
            area.id = $stateParams.id;
            AreaService.update(angular.toJson(area), function(data) {
                if (data.r === 'ok') {
                    $state.go('^');
                } else if (data.r === 'exist') {
                    $scope.form.name.$invalid = true;
                    $scope.form.name.$error.exists = true;
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

