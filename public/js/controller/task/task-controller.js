'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.controller.task.taskModule', []);

    app.controller('TaskCtrl', ['$scope','TaskService','EnvService','ProjectService','$state', '$stateParams', '$interval', 'Auth', function($scope,TaskService,EnvService,ProjectService,$state,$stateParams, $interval, Auth) {
        $scope.user = Auth.user;
        console.log($scope.user)
//=====================================变量========================================
        $scope.projectStatus = []

        $scope.versions = []

//=====================================环境========================================
        //环境列表(根据用户类型判断是否显示安全区环境)
        EnvService.getAuth($scope.user.username, function(data){
            $scope.envs = [];
            for(var d in data){
                $scope.envs.push(data[d])
            }
        });
        //默认选中的环境id，应根据用户偏好获取
        $scope.activeEnv = 1
        //选择默认选中的环境
        $scope.activeTab = function(envId){
            if(envId == $scope.activeEnv){
                return "active"
            }else{
                return ""
            }
        }
        //选择tab页
        $scope.chooseEnv = function(envId){
            $scope.activeEnv = envId
            //触发查询环境下的项目列表

        }
//=====================================项目========================================
        //项目列表
        ProjectService.getAll(function(data){
            $scope.pros = []
            for(var p in data){
                $scope.pros.push(data[p])
            }
            //查询任务表task 返回 projectId, status, string, taskId
            TaskService.getLastTaskStatus($scope.activeEnv, $scope.pros, function(data){
                $scope.lastTasks = data
                console.log(data)
                console.table($scope.projectStatus)
                $scope.projectStatus = $scope.pros.map($scope.changeData).map($scope.addStatusTip)
                console.table($scope.projectStatus)
                $scope.getTemplates()

                //过滤正在执行任务的项目集 -> 使用websocket
                $scope.wsInvoke()
            })
        });

        $scope.getTemplates = function(){
            //查询项目模板（操作按钮）
            TaskService.getTemplates(function(data){
                console.log(data)
                $scope.templates = data
                //按钮
                $scope.projectStatus.map($scope.getProjectTemplates)
                console.table($scope.projectStatus)
            })
        }

        $scope.getProjectTemplates = function(data){
            console.log(data)
            console.log(data.templateId)
            data.templates = $scope.templates[data.templateId]
            console.table(data.templates)
            data
        }

        $scope.randomKey = function(min, max) {
            var num = Math.floor(Math.random() * (max - min + 1)) + min;
            console.log("num:" + num)
            return num
        }

        $scope.wsInvoke = function(){
            var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
            var path = PlayRoutes.controllers.task.TaskController.joinProcess($scope.randomKey(1,10000)).webSocketURL()
//            path = "ws://bugatti.dev.ofpay.com/task/joinProcess"
            $scope.taskSocket = new WS(path)
            $scope.taskSocket.onmessage = $scope.receiveEvent
        }

        $scope.receiveEvent = function(event){
            $scope.tsData = JSON.parse(event.data)
            console.log($scope.tsData)
            if(event.data.error){
                console.log("there is errors:" + event.data.error)
            }else{
                $scope.$apply(function(){
                    var tsData = $scope.tsData
                    console.log(tsData)
                    for(var pIndex in $scope.projectStatus){
                        var p = $scope.projectStatus[pIndex]
                        //envId + projectId
                        var key = $scope.activeEnv + "_" + p.id
                        console.log(key)
                        console.log(tsData)
                        var projectObj = tsData[key]
                        console.log(projectObj)
                        if(projectObj != undefined){
                            p.status.currentNum = projectObj.currentNum
                            p.status.totalNum = projectObj.totalNum
//                            if(projectObj.queueNum == undefined){
//                             projectObj.queueNum = 0
//                            }
                            p.status.queueNum = projectObj.queueNum
                            p.status.sls = projectObj.sls
                            p.status.machine = projectObj.machine
                            p.status.taskName = projectObj.taskName
                            p.status.status = projectObj.status
                            p.task = projectObj.task
                        }
                    }
                    $scope.projectStatus = $scope.projectStatus.map($scope.addStatusTip)
                    console.log($scope.projectStatus)
                })
            }
        }

        //更新所有项目的status
        $scope.updateProjectStatus = function(taskId, finishNum){
            for(var index in $scope.projectStatus){
                var p = $scope.projectStatus[index]
                if(p.status.taskId == taskId){
                    p.status.finishNum = finishNum
                }
            }
        }

        //合并两个数组 pros & projectStatus
        $scope.changeData = function(data){
            data.status = {}
            for(var index in $scope.lastTasks){
                var p = $scope.lastTasks[index]
                if(p.projectId === data.id){
                    data.status = p
                }
            }
            data.status.queueNum = 0
            return data
        }
        //增加状态描述
        $scope.addStatusTip = function(data){
            if(!$scope.isObjEmpty(data.status)){
                data.status.statusTip = $scope.explainTaskStatus(data.status.status)
            } else {
                data.status.statusTip = "N/A"
            }
            return data
        }
        //解析task status
        $scope.explainTaskStatus = function(status){
            switch(status){
                //执行成功
                case 1 : return "执行成功"
                //执行失败
                case 2 : return "执行失败"
                //正在执行
                case 3 : return "正在执行"
            }
        }
        //根据任务状态判断是否需要显示超链接
        $scope.showHref = function(status){
            if(status === 3 || status == null) {
                return false
            } else {
                return true
            }
        }
        //根据任务状态判断是否需要显示进度条
        $scope.showProgress = function(status){
            if(status === 3){
                return true
            } else {
              return false
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
//=====================================新建任务 （部署 + 启动 + 关闭 + 重启）========================================
        $scope.showVersion = function(pid){
            console.log(pid)

            $scope.versions = []
            TaskService.getVersions(pid, $scope.activeEnv, function(data){
                for(var dIndex in data) {
                    $scope.versions.push(data[dIndex])
                }
                console.log($scope.versions)
            })
        }

        $scope.showNexusVersion = function(pid){
            TaskService.getNexusVersions(pid, function(data){
                console.log(data)
            })
        }

        $scope.deploy = function(projectId, versionId){
            $scope.taskQueue = {}
            $scope.taskQueue.envId = $scope.activeEnv
            $scope.taskQueue.projectId = projectId
            $scope.taskQueue.versionId = versionId
            $scope.taskQueue.templateId = $scope.choosedTemplateId
            TaskService.createNewTaskQueue($scope.taskQueue, function(data){

            })
        }

        $scope.start = function(){

        }

        $scope.stop = function(){

        }

        $scope.restart = function(){

        }

        $scope.showClass = function(versionMenu){
            if(versionMenu){
                console.log("dropdown-toggle")
                return "dropdown-toggle"
            } else {
                return "false"
            }
        }

        $scope.changeDataToggle = function(projectId){
            for(var dIndex in $scope.projectStatus){
                var obj = $scope.projectStatus[dIndex]
                if(obj.id == projectId){
                    obj.dropClass = "dropdown"
                }
            }
            console.table($scope.projectStatus)
        }

        $scope.showDropdown = function(versionMenu){
            if(versionMenu){
                return "dropdown"
            } else {
                return "false"
            }
        }

        $scope.showClick = function(versionMenu, projectId, templateId){
            if(!versionMenu){
                $scope.taskQueue = {}
                $scope.taskQueue.envId = $scope.activeEnv
                $scope.taskQueue.projectId = projectId
//                $scope.taskQueue.versionId = versionId
                $scope.taskQueue.templateId = templateId
                TaskService.createNewTaskQueue($scope.taskQueue, function(data){})
                $scope.versionShow = false
            } else {//部署
                $scope.choosedTemplateId = templateId
                $scope.showVersion(projectId)
                $scope.versionShow = true
            }

        }

        //用来控制是否展示下拉菜单
        $scope.showVersionMenu = function(){
            return $scope.versionShow
        }

//=====================================页面跳转========================================
        $scope.goTaskQueue = function(envId, projectId){
            console.log(envId + ", " + projectId)
            $state.go('.queue', {envId: envId, projectId: projectId})
        }


    }]);

    app.controller('TaskLogCtrl',['$scope', 'TaskService','$state','$stateParams',function($scope,TaskService,$state,$stateParams){
        console.log($stateParams)
        var taskId = $stateParams.taskId

        $scope.envId_search = $stateParams.envId
        $scope.envName_search = $stateParams.envName

        $scope.proId_search = $stateParams.proId
        $scope.proName_search = $stateParams.proName


        var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
//        var path = PlayRoutes.controllers.task.TaskController.taskLog(taskId).webSocketURL()
        var path = PlayRoutes.controllers.task.TaskController.taskLog(1).webSocketURL()
        var logSocket = new WS(path)
//        var logSocket = new WS("ws://bugatti.dev.ofpay.com/#/task/joinProcess")

        $scope.message = ""
        $scope.data = ""
        $scope.logFirst = ""

        $scope.receiveEvent = function(event){
            $scope.data = JSON.parse(event.data)
            console.log("Data received, Message is =>" + $scope.data.message)
            if(event.data.error){
                console.log("there is errors:"+event.data.error)
            }else{
                $scope.$apply(function () {
                    var data = $scope.data
                    if(!$scope.logFirstHidden && data.from == 0){
                        $scope.logFirstHidden = true
                    }
                    if(data.kind == "logFirst"){
//                       if(data.from > 0){
                           $scope.logFirst = data.message
//                       }
                    }else{
                        $scope.message = $scope.message + $scope.TransferString(data.message)
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
            $scope.logFirstHidden = true
            console.log("this is new actor")
            var len = parseInt($scope.logFirst.split(" ")[0])
            console.log(len)
            var logHeaderPath = PlayRoutes.controllers.task.TaskController.taskLogFirst(taskId, len).webSocketURL()
            console.log(logHeaderPath)
            var logHeaderSocket = new WS(logHeaderPath)

            logHeaderSocket.onmessage = $scope.receiveHeaderEvent
        }

        $scope.receiveHeaderEvent = function(event){
            $scope.$apply(function () {
                console.log(event.data)
                $scope.logSecond = $scope.TransferString(event.data)
            })
        }

        $scope.TransferString = function(content)
        {
            var string = content;
            try{
                string=string.replace(/\r\n/gi,"<br>")
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

    app.controller('TaskInfoCtrl',['$scope', 'TaskService','$state','$stateParams',function($scope,TaskService,$state,$stateParams){
        console.log($stateParams)
        $scope.taskId = $stateParams.taskId

        $scope.envId_search = $stateParams.envId
        $scope.envName_search = $stateParams.envName

        $scope.proId_search = $stateParams.proId
        $scope.proName_search = $stateParams.proName


        TaskService.find($scope.taskId, function(data){
            console.table(data)
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

        console.log($stateParams)
        $scope.envId_search = $stateParams.envId
        $scope.projectId_search = $stateParams.projectId

        $scope.randomKey = function(min, max) {
            var num = Math.floor(Math.random() * (max - min + 1)) + min;
            console.log("num:" + num)
            return num
        }

        $scope.wsInvoke = function(){
            var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
            var path = PlayRoutes.controllers.task.TaskController.joinProcess($scope.randomKey(1,10000)).webSocketURL()
//            path = "ws://bugatti.dev.ofpay.com/task/joinProcess"
            $scope.taskSocket = new WS(path)
            $scope.taskSocket.onmessage = $scope.receiveEvent
        }

        $scope.receiveEvent = function(event){
            $scope.taskQueues = []
            $scope.tsData = JSON.parse(event.data)
            console.log("Data received, Message is =>" + $scope.tsData.message)
            if(event.data.error){
                console.log("there is errors:"+event.data.error)
            }else{
                $scope.$apply(function(){
                    var tsData = $scope.tsData
                    console.log(tsData)

                    var key = $scope.envId_search + "_" + $scope.projectId_search
                    console.log("key==>" + key)
                    var taskQueues = tsData[key]

                    $scope.taskQueues = taskQueues.queues.map($scope.addStatusTip)
                    console.table($scope.taskQueues)
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