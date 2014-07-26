'use strict';

define(['angular'], function(angular){
    var app = angular.module('bugattiApp.controller.conf.systemModule', []);

    app.controller('SystemCtrl', ['$scope', '$modal', 'SystemService', function($scope, $modal, SystemService){
        $scope.buildTag = function() {
            var modalInstance = $modal.open({
                templateUrl: "partials/modal.html",
                controller: function ($scope, $modalInstance) {
                    $scope.ok = function() {
                        SystemService.buildTag(function(state) {
                            $modalInstance.close(state);
                        });
                    };
                    $scope.cancel = function() {
                        $modalInstance.dismiss("cancel")
                    };
                }
            });
            modalInstance.result.then(function(state) {
            });
        };

        $scope.refresh = function() {
            var modalInstance = $modal.open({
                templateUrl: "partials/modal.html",
                controller: function ($scope, $modalInstance) {
                    $scope.ok = function() {
                        SystemService.refresh(function(state) {
                            $modalInstance.close(state);
                        });
                    };
                    $scope.cancel = function() {
                        $modalInstance.dismiss("cancel")
                    };
                }
            });
            modalInstance.result.then(function(state) {
            });
        }
    }]);
});

