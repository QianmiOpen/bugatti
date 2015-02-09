'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.controller.task.taskModule', []);

    function keepSession($scope, $interval, Auth) {
        var intervalPromise = $interval(function () {
            Auth.ping(function() {}, function() {});
        }, 5000);
        $scope.$on('$destroy', function () { $interval.cancel(intervalPromise); });
    }

    app.controller('TaskCtrl', ['$scope', '$state', '$stateParams', 'EnvService', 'ProjectService', 'AreaService', 'TaskService',
        function($scope, $state, $stateParams, EnvService, ProjectService, AreaService, TaskService) {

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

        $scope.activeEnv = function(e) {
            $scope.env = e;
            if (angular.isDefined($state.params.pid)) {
                $state.go('task.list.info', { eid: e.id, pid: $state.params.pid, top: $state.params.top});
            } else {
                $state.go('task.list', { eid: e.id });
            }
            // load projects
            $scope.load.is = true;
            $scope.projects = []
            ProjectService.getAuth(e.id, function(data) {
                $scope.projects = data;
                $scope.load.is = false;
            });

            $scope.scriptVersion = e.scriptVersion
            $scope.getTemplates();
        };

        $scope.getTemplates = function(){
            //查询项目模板（操作按钮）
            TaskService.getTemplates($scope.scriptVersion, function(data){
                $scope.templates = data;
            })
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
            var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
            var path = PlayRoutes.controllers.task.TaskController.joinProcess($scope.env.id, $scope.project.id).webSocketURL()
            $scope.taskSocket = new WS(path)
            $scope.taskSocket.onmessage = $scope.receiveEvent
        }

        ProjectService.get($stateParams.pid, function (data) {
            $scope.project = data;
            $scope.load.is = false;
            $scope.wsInvoke();
        });
    }]);

    app.controller('TaskLogCtrl',['$scope', 'TaskService','$state','$stateParams',function($scope,TaskService,$state,$stateParams){
        var taskId = $stateParams.taskId

        $scope.envId_search = $stateParams.envId
        $scope.envName_search = $stateParams.envName

        $scope.proId_search = $stateParams.proId
        $scope.proName_search = $stateParams.proName


        var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
        var path = PlayRoutes.controllers.task.TaskController.taskLog(taskId).webSocketURL()
        var logSocket = new WS(path)

        $scope.message = ""
        $scope.data = ""
        $scope.logFirst = ""
        $scope.logHeader = ""

        $scope.receiveEvent = function(event){
            $scope.data = JSON.parse(event.data)
            if(event.data.error){
                console.log("there is errors:"+event.data.error)
            }else{
                $scope.$apply(function () {
                    var data = $scope.data
                    if(data.taskId == taskId){
                        if(data.kind == "logFirst"){
                            if(!$scope.logFirstHidden && data.message.split(" ")[0] == 0){
                                $scope.logFirstHidden = true
                            }
                            if($scope.logFirst.length == 0){
                                $scope.logFirst = data.message
                            }
                        }else if(data.kind == "logHeader"){
                            $scope.logFirstHidden = true
                            if($scope.logHeader.length == 0){
                                $scope.logHeader = data.message
                                $scope.message = $scope.logHeader + $scope.message
                            }
                        }else{
                            if($scope.message.length == 0){
                                $scope.message = data.message
                            }
                        }
                    }
                });
            }
        }

        logSocket.onmessage = $scope.receiveEvent

        $scope.closeWs = function(){
            logSocket.close()
            $scope.returnList()
        }

        $scope.logFirstHidden = false

        $scope.showHiddenMessage = function(){
            var len = parseInt($scope.logFirst.split(" ")[0])
            TaskService.readHeader(taskId, len, function(){})
        }

        $scope.TransferString = function(content)
        {
            var string = content;
            try{
                string=string.replace(/\r\n/gi,"<br>");
                string=string.replace(/\n/gi,"<br>");
            }catch(e) {
                console.log(e.message);
            }
            return string;
        }

        $scope.returnList = function(){
            $state.go('^.^.task',{'envId': $scope.envId_search, 'proId': $scope.proId_search, 'envName': $scope.envName_search, 'proName': $scope.proName_search})
        }

    }]);

    app.controller('Task3InfoCtrl',['$scope', 'TaskService','$state','$stateParams',function($scope,TaskService,$state,$stateParams){
        $scope.taskId = $stateParams.taskId

        $scope.envId_search = $stateParams.envId
        $scope.envName_search = $stateParams.envName

        $scope.proId_search = $stateParams.proId
        $scope.proName_search = $stateParams.proName

        TaskService.find($scope.taskId, function(data){
            $scope.operateName = data.taskOperate.name
            $scope.environmentName = data.environment.name
            $scope.projectName = data.project.name
            $scope.version = data.task.version
            $scope.machines = data.taskMachine
        })

        $scope.returnList = function(){
            $state.go('^.^.task',{'envId': $scope.envId_search, 'proId': $scope.proId_search, 'envName': $scope.envName_search, 'proName': $scope.proName_search})
        }

    }]);


    app.controller('TaskQueueCtrl',['$scope', 'TaskService','$state','$stateParams',function($scope,TaskService,$state,$stateParams){
        $scope.taskQueues = []

        $scope.envId_search = $stateParams.envId
        $scope.projectId_search = $stateParams.projectId

        $scope.randomKey = function(min, max) {
            return Math.floor(Math.random() * (max - min + 1)) + min;
        }

        $scope.wsInvoke = function(){
            var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
            var path = PlayRoutes.controllers.task.TaskController.joinProcess($scope.randomKey(1,10000)).webSocketURL()
            $scope.taskSocket = new WS(path)
            $scope.taskSocket.onmessage = $scope.receiveEvent
        }

        $scope.receiveEvent = function(event){
            $scope.taskQueues = []
            $scope.tsData = JSON.parse(event.data)
            if(event.data.error){
                console.log("there is errors:"+event.data.error)
            }else{
                $scope.$apply(function(){
                    var tsData = $scope.tsData

                    var key = $scope.envId_search + "_" + $scope.projectId_search
                    var taskQueues = tsData[key]

                    $scope.taskQueues = taskQueues.queues.map($scope.addStatusTip)
                })
            }
        }

        //1、创建socket连接
        $scope.wsInvoke()

        $scope.removeQueue = function(qid){
            TaskService.removeTaskQueue(qid, function(data){
                //如果删除的任务在一瞬间刚好变为正在执行，应告知
            })
        }

        $scope.addStatusTip = function(data){
            if(!$scope.isObjEmpty(data)){
                data.statusName = $scope.explainStatus(data.status)
            } else {
                data.status.statusTip = "N/A"
            }
            return data
        }

        $scope.explainStatus = function(status){
            switch(status){
                //等待执行
                case 0 : return "等待执行"
                //执行成功
                case 1 : return "执行成功"
                //执行失败
                case 2 : return "执行失败"
                //正在执行
                case 3 : return "正在执行"
            }
        }

        //判断对象是否为空
        $scope.isObjEmpty = function(obj){
            for (var name in obj)
            {
                return false
            }
            return true
        }

        $scope.returnList = function(){
            $state.go('^.^.task',{'envId': $scope.envId_search})
        }

    }]);
});