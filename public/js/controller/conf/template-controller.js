'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.controller.conf.templateModule', []);

    app.controller('TemplateCtrl', ['$scope', '$modal', 'TemplateService', function($scope, $modal, TemplateService) {
        $scope.currentPage = 1;
        $scope.pageSize = 10;
    }]);

    app.controller('TemplateCreateCtrl', ['$scope', '$stateParams', '$state', 'TemplateService', function($scope, $stateParams, $state, TemplateService) {
        $scope.template = { attrs: [] };

        $scope.add = function() {
            $scope.template.attrs.push({
                key: "",
                value: ""
            });
        };
        $scope.remove = function(index) {
            $scope.template.attrs.splice(index, 1);
        }

        // insert
        $scope.saveOrUpdate = function(template, attrs) {
            console.log(angular.toJson(template))
            console.log(angular.toJson(attrs))
        };

    }]);

});