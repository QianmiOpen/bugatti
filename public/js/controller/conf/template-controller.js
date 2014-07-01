'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.controller.conf.templateModule', []);

    app.controller('TemplateCtrl', ['$scope', '$modal', 'TemplateService', function($scope, $modal, TemplateService) {
        $scope.currentPage = 1;
        $scope.pageSize = 10;
    }]);

    app.controller('TemplateCreateCtrl', ['$scope', '$stateParams', '$state', 'TemplateService', function($scope, $stateParams, $state, TemplateService) {
        $scope.template = { items: [] };

        $scope.add = function() {
            $scope.template.items.push({
                name: "",
                desc: "",
                order: 0
            });
        };
        $scope.remove = function(index) {
            $scope.template.items.splice(index, 1);
        }

        // insert
        $scope.saveOrUpdate = function(template, items) {
            console.log(angular.toJson(template))
            console.log(angular.toJson(items))
        };

    }]);

});