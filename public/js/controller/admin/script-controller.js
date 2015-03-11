'use strict';

define(['angular'], function(angular){
    var app = angular.module('bugattiApp.controller.admin.scriptModule', []);

    app.controller('ScriptCtrl', ['$scope', '$modal', 'SystemService', function($scope, $modal, SystemService){
        $scope.refresh = function() {
            var modalInstance = $modal.open({
                templateUrl: "partials/modal.html",
                controller: function ($scope, $modalInstance) {
                    $scope.ok = function() {
                        SystemService.refresh(function(data) {
                            $modalInstance.close(data);
                        });
                    };
                    $scope.cancel = function() {
                        $modalInstance.dismiss("cancel")
                    };
                }
            });
            modalInstance.result.then(function(data) {
            });
        }
    }]);
});

