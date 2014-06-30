'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.controller.conf.ptypeModule', []);

    app.controller('PTypeCtrl', ['$scope', '$modal', 'PTypeService', function($scope, $modal, PTypeService) {
        $scope.currentPage = 1;
        $scope.pageSize = 10;
    }]);

    app.controller('PTypeCreateCtrl', ['$scope', '$stateParams', '$state', 'PTypeService', function($scope, $stateParams, $state, PTypeService) {
        $scope.items = [];

        $scope.add = function () {
            $scope.items.push({
                key: "",
                value: ""
            });
        };
        $scope.remove = function(index) {
            $scope.items.splice(index, 1);
        }

    }]);

});