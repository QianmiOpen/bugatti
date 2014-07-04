'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.controller.conf.confModule', []);

    app.controller('ConfCtrl', ['$scope', '$state', '$stateParams', '$modal',
        'ConfService', 'EnvService', 'ProjectService', 'VersionService',
        function($scope, $state, $stateParams, $modal, ConfService, EnvService, ProjectService, VersionService) {
            // load project
            ProjectService.get($stateParams.id, function(data) {
                $scope.project = data;
            });
            // load version
            VersionService.get($stateParams.vid, function(data) {
                $scope.version = data;
            });
            // load env all
            EnvService.getAll(function(data) {
                if (data == null || data.length == 0) {
                    return;
                }
                $scope.envs = data;
                // default select first
                $scope.envChange(data[0])
            });

            // 环境选择
            $scope.envChange = function(e) {
                $scope.env = e;
                // 根据环境和版本动态加载配置文件
//                ConfService.getAll($scope.env.id, $stateParams.vid, function(data) {
//                    $scope.confs = data;
//                });
                $state.go('conf.project.version.conf.list', {eid: e.id})
            };
    }]);

    app.controller('ConfListCtrl', ['$scope', '$state', '$stateParams', '$modal',
        'ConfService',
        function($scope, $state, $stateParams, $modal, ConfService) {
            ConfService.getAll($stateParams.eid, $stateParams.vid, function(data) {
                $scope.confs = data;
            });
        }]);

    app.controller('ConfCreateCtrl', ['$scope', '$state', '$stateParams', '$modal',
        'ConfService', 'EnvService', 'ProjectService', 'VersionService',
        function($scope, $state, $stateParams, $modal, ConfService, EnvService, ProjectService, VersionService) {
            $scope.conf = {pid: $stateParams.id, vid: $stateParams.vid};
            $scope.cancel = function() {
//                if($scope.conf.content == undefined) {
//                    $state.go('^')
//                }
            };

            $scope.save = function() {
                $scope.conf.eid = $scope.env.id;
                ConfService.save(angular.toJson($scope.conf), function(data) {
                    $state.go('conf.project.version.conf.list', {eid: $scope.env.id})
                });
            }
    }]);


    app.controller('ConfShowCtrl', ['$scope', '$state', '$stateParams', '$modal',
        'ConfService',
        function($scope, $state, $stateParams, $modal, ConfService) {
            ConfService.get($stateParams.cid, function(data) {
                $scope.conf = data.conf;
                $scope.conf.content = data.content.content;
            });

        }]);
});