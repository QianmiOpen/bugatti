'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.controller.admin.templateModule', []);

    app.controller('TemplateCtrl', ['$scope', '$modal', 'growl', 'TemplateService', function($scope, $modal, growl, TemplateService) {
        $scope.app.breadcrumb='模板管理';
        $scope.currentPage = 1;
        $scope.pageSize = 1000;

        // list
        TemplateService.all(function(data) {
            $scope.templates = data;
            $scope.totalItems = data.length;
        });

        // remove
        $scope.delete = function(id, index) {
            var modalInstance = $modal.open({
                templateUrl: 'partials/modal.html',
                controller: function ($scope, $modalInstance) {
                    $scope.ok = function () {
                        TemplateService.remove(id, function(data) {
                            $modalInstance.close(data);
                        });
                    };
                    $scope.cancel = function () {
                        $modalInstance.dismiss('cancel');
                    };
                }
            });
            modalInstance.result.then(function(data) {
                if (data.r === 'exist') {
                    growl.addWarnMessage('还有项目在使用该模板，请删除后再操作。。。');
                } else {
                    $scope.templates.splice(index, 1);
                    $scope.totalItems = $scope.totalItems -1;
                }
            });
        };


    }]);


    app.controller('TemplateCreateCtrl', ['$scope', '$stateParams', '$state', 'TemplateService', function($scope, $stateParams, $state, TemplateService) {
        $scope.template = { items: [] };

        $scope.add = function() {
            $scope.template.items.push({
                itemName: "",
                itemDesc: "",
                order: 0
            });
        };
        $scope.remove = function(index) {
            $scope.template.items.splice(index, 1);
        }

        // insert
        $scope.saveOrUpdate = function(template) {
            TemplateService.save(angular.toJson(template), function(data) {
                if (data.r === 'exist') {
                    $scope.form.name.$invalid = true;
                    $scope.form.name.$error.exists = true;
                } else {
                    $state.go('^');
                }
            })
        };

    }]);


    app.controller('TemplateShowCtrl', ['$scope', '$stateParams', 'TemplateService',
        function($scope, $stateParams, TemplateService) {

            TemplateService.get($stateParams.id, function(data) {
                $scope.template = data;
            });

            TemplateService.items($stateParams.id, function(data) {
                $scope.items = data;
            });

        }]);


    app.controller('TemplateUpdateCtrl', ['$scope', '$stateParams', '$state', 'TemplateService', function($scope, $stateParams, $state, TemplateService) {
        $scope.template = { items: [] };

        $scope.add = function() {
            $scope.template.items.push({
                itemName: "",
                itemDesc: "",
                order: 0
            });
        };
        $scope.remove = function(index) {
            $scope.template.items.splice(index, 1);
        }

        $scope.saveOrUpdate = function(template) {
            TemplateService.update($stateParams.id, angular.toJson(template), function(data) {
                if (data.r === 'exist') {
                    $scope.form.name.$invalid = true;
                    $scope.form.name.$error.exists = true;
                } else {
                    $state.go('^');
                }
            });
        };

        TemplateService.get($stateParams.id, function(data) {

            // update form reset
            $scope.master = data;
            $scope.reset = function() {
                $scope.template = angular.copy($scope.master);
            };
            $scope.isUnchanged = function(template) {
                return angular.equals(template, $scope.master);
            };
            $scope.reset();

            // load items
            TemplateService.items($stateParams.id, function(data) {
                $scope.template.items = data;
                $scope.master.items = data;
            })
        });
    }]);

});