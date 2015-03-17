'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.controller.admin.spiritModule', []);

    app.controller('SpiritCtrl', ['$scope', '$modal', 'SpiritService', function($scope, $modal, SpiritService) {
        $scope.app.breadcrumb='网关管理';
        SpiritService.getAll(function(data){
            $scope.spirits = data;
        });
        // refresh
        $scope.refresh = function(id) {
            var modalInstance = $modal.open({
                templateUrl: 'partials/modal.html',
                controller: function($scope, $modalInstance) {
                    $scope.ok = function(){
                        SpiritService.refresh(id, function(data){
                            $modalInstance.close(data);
                        })
                    };
                    $scope.cancel = function(){
                        $modalInstance.dismiss('cancel')
                    };
                }
            });
        };

        // remove
        $scope.delete = function(id, index) {
            var modalInstance = $modal.open({
                templateUrl: 'partials/modal.html',
                controller: function ($scope, $modalInstance) {
                    $scope.ok = function () {
                        SpiritService.remove(id, function(data) {
                            $modalInstance.close(data);
                        });
                    };
                    $scope.cancel = function () {
                        $modalInstance.dismiss('cancel');
                    };
                }
            });
            modalInstance.result.then(function(data) {
                $scope.spirits.splice(index, 1);
                SpiritService.count(function(num) {
                    $scope.totalItems = num;
                });
            });
        };

    }]);

    app.controller('SpiritCreateCtrl', ['$scope', '$stateParams', '$state', 'SpiritService', function($scope, $stateParams, $state, SpiritService) {
        $scope.spirit = {};
        $scope.saveOrUpdate = function(spirit) {
            spirit.variable = angular.copy($scope.vars);
            SpiritService.save(angular.toJson(spirit), function(data) {
                if (data.r === 'exist') {
                    $scope.form.name.$invalid = true;
                    $scope.form.name.$error.exists = true;
                } else {
                    $state.go('^');
                }
            });
        };
    }]);

    app.controller('SpiritUpdateCtrl', ['$scope', '$stateParams', '$state', 'SpiritService', function($scope, $stateParams, $state, SpiritService) {
        $scope.saveOrUpdate = function(spirit) {
            spirit.variable = angular.copy($scope.vars);
            SpiritService.update($stateParams.id, angular.toJson(spirit), function(data) {
                if (data.r === 'exist') {
                    $scope.form.name.$invalid = true;
                    $scope.form.name.$error.exists = true;
                } else {
                    $state.go('^');
                }
            });
        };

        SpiritService.get($stateParams.id, function(data) {
            // update form reset
            $scope.master = data;
            $scope.reset = function() {
                $scope.spirit = angular.copy($scope.master);
            };
            $scope.isUnchanged = function(spirit) {
                return angular.equals(spirit, $scope.master);
            };
            $scope.reset();

            // init variable
            $scope.vars = angular.copy(data.globalVariable);
        });
    }]);

});