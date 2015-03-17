'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.controller.home.taskModule', []);

    function keepSession($scope, $interval, Auth) {
        var intervalPromise = $interval(function () {
            Auth.ping(function() {}, function() {});
        }, 5000);
        $scope.$on('$destroy', function () { $interval.cancel(intervalPromise); });
    }

    app.controller('TaskCtrl', ['$scope', '$state', '$stateParams', "$interval", "Auth", 'EnvService', 'ProjectService', 'AreaService', 'TaskService',
        function($scope, $state, $stateParams, $interval, Auth, EnvService, ProjectService, AreaService, TaskService) {

        keepSession($scope, $interval, Auth);

        // init
        $scope.env = {};
        $scope.envs = [];
        $scope.load = { is: true };
        $scope.focus = { is: true };
        $scope.model = {hps : false};
        $scope._search = {name: ''};

        // keep search value
        if (angular.isDefined($state.params.txt)) {
            $scope._search.name = $state.params.txt;
        }

        // load envs
        EnvService.getAuth(function(data) {
            if (data == null || data.length == 0) {
                return;
            }
            $scope.envs = data;

            if (angular.isUndefined($state.params.eid)) { // 第一次访问任务页面
                $scope.activeEnv($scope.envs[0]);
            } else {                                      // F5刷新保持当前URL
                var e = ck_env_in_array($scope.envs, $state.params.eid);
                $scope.activeEnv(e);
            }

        });

        // util
        function ck_env_in_array(envs, eid) {
            var find = false;
            var r = envs[0];
            angular.forEach(envs, function(e) {
                if (!find && e.id == eid) {
                    r = e;
                    find = true;
                }
            });
            return r;
        }
        function in_projects(projects, pid) {
            var find = false;
            angular.forEach(projects, function(p) {
                if (!find && p.id == pid) {
                    find = true;
                }
            });
            return find;
        }

        $scope.activeEnv = function(e) {
            $scope.env = e;
            // load projects
            $scope.load.is = true;
            $scope.projects = [];
            ProjectService.getAuth(e.id, function(data) {
                $scope.projects = data;
                $scope.load.is = false;
                if (angular.isDefined($state.params.pid) && in_projects($scope.projects, $state.params.pid)) {
                    $state.go('home.list.info', { eid: e.id, pid: $state.params.pid, top: $state.params.top});
                } else {
                    if ($scope.projects.length > 0 && $scope.projects[0].id > 0) {
                        $state.go('home.list.info', { eid: e.id, pid: $scope.projects[0].id, top: $state.params.top});
                    } else {
                        $state.go('home.list', { eid: e.id });
                    }
                }
            });

            $scope.scriptVersion = e.scriptVersion;
            $scope.getTemplates();
        };

        $scope.getTemplates = function(){
            //查询项目模板（操作按钮）
            TaskService.getTemplates($scope.scriptVersion, function(data){
                $scope.templates = data;
            })
        }

        $scope.closeWS = function(){
            if($scope.taskSocket){
                $scope.taskSocket.close();
            }
        }

        $scope.receiveEvent = function(event){
            if(event.data.error){
                console.log("there is errors:" + event.data.error)
            }else{
                $scope.$apply(function(){
                    $scope.tsData = JSON.parse(event.data)
                })
            }
        }

        $scope.wsInvoke = function(){
            var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket;
            var path = PlayRoutes.controllers.home.TaskController.joinProcess($scope.env.id, $state.params.pid).webSocketURL();

            $scope.closeWS();
            $scope.taskSocket = new WS(path);
            $scope.taskSocket.onmessage = $scope.receiveEvent;
        }

    }]);

    app.controller('TaskInfoCtrl', ['$scope', '$stateParams', 'ProjectService', function($scope, $stateParams, ProjectService) {

        $scope.toggleHps = function() {
            $scope.model.hps = $scope.model.hps === false ? true: false;
            //$scope.$parent.$parent.hps= $scope.hps === false ? true: false;
        };

        $scope.load.is = true;

        $scope.randomKey = function(min, max) {
            return Math.floor(Math.random() * (max - min + 1)) + min;
        }

        ProjectService.get($stateParams.pid, function (data) {
            $scope.project = data;
            $scope.load.is = false;
            $scope.wsInvoke();
        });
    }]);
});