'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.controller.task.taskModule', []);

    function keepSession($scope, $interval, Auth) {
        var intervalPromise = $interval(function () {
            Auth.ping(function() {}, function() {});
        }, 5000);
        $scope.$on('$destroy', function () { $interval.cancel(intervalPromise); });
    }

    app.controller('TaskCtrl', ['$scope', 'TaskService','EnvService','ProjectService', 'VersionService', 'AreaService', 'RelationService',
        '$state', '$stateParams', '$interval', 'Auth', '$modal', 'growl', '$http',
        function($scope, TaskService, EnvService, ProjectService, VersionService, AreaService, RelationService,
                 $state, $stateParams, $interval, Auth, $modal, growl, $http) {

        keepSession($scope, $interval, Auth);
//=====================================变量========================================
        $scope.projectStatus = []

        $scope.versions = []

//=====================================环境========================================
        //默认选中的环境id，应根据用户偏好获取
        $scope.activeEnv = 1
        //环境列表(根据用户类型判断是否显示安全区环境)
        EnvService.getAuth(function(data){
            $scope.envs = [];
            for(var d in data){
                $scope.envs.push(data[d])
            }
            $scope.activeEnv = $scope.envs.length == 0 ? 1 : $scope.envs[0].id
            if($scope.envs.length > 0){
                $scope.chooseEnv($scope.activeEnv)
            }
        });

        //选择默认选中的环境
        $scope.activeTab = function(envId){
            if(envId == $scope.activeEnv){
                return "active"
            }else{
                return ""
            }
        }
        $scope.wsBool = true

        //选择tab页
        $scope.chooseEnv = function(envId) {
            // 加载环境对应可用区域
            AreaService.list(envId, function(data) {
                $scope.preAreas = data;
                if ($scope.preAreas.length > 0) {
                    $scope.useArea = $scope.preAreas[0].id
                }
            });

            $scope.scriptVersion = ""
            $scope.activeEnv = envId
            //获取模板
            for(var index in $scope.envs){
                var envObj = $scope.envs[index]
                if(envObj.id == envId){
                    $scope.scriptVersion = envObj.scriptVersion
                }
            }
//            console.table($scope.envs)
            $scope.getTemplates()
        }

//=====================================项目========================================
//        $scope.projectAllFlag = false
//        $scope.projectListName = $scope.projectAllFlag ? "只显示我的项目": "显示所有项目"

//        $scope.changeProjects = function(){
//            $scope.projectAllFlag = ! $scope.projectAllFlag
//            $scope.projectListName = $scope.projectAllFlag ? "只显示我的项目": "显示所有项目"
//            $scope.showProjects()
//        }
        $scope.showProjects = function(){
            ProjectService.getAuth($scope.activeEnv, function(data){
                $scope.pros = []
                for(var p in data){
                    $scope.pros.push(data[p])
                }

                //过滤正在执行任务的项目集 -> 使用websocket
                if($scope.wsBool){
                    $scope.wsInvoke()
                    $scope.wsBool = false
                }
                //查询任务表task 返回 projectId, status, string, taskId
//                TaskService.getLastTaskStatus($scope.activeEnv, $scope.pros, function(data){
//                    $scope.lastTasks = data
//                    $scope.projectStatus = $scope.pros.map($scope.changeData).map($scope.addStatusTip)
//                    $scope.mergeTemplates()
//                })
                $scope.projectStatus = $scope.pros.map($scope.changeData).map($scope.addStatusTip)
                $scope.mergeTemplates()
                $scope.projectStatus = $scope.projectStatus.map(function(data){
                    data.isOpen = false;
                    return data;
                })
            })
        }

        $scope.mergeTemplates = function(){
            //按钮
            $scope.projectStatus.map($scope.getProjectTemplates)
        }
        $scope.getTemplates = function(){
            //查询项目模板（操作按钮）
            TaskService.getTemplates($scope.scriptVersion, function(data){
                $scope.templates = data
                //触发查询环境下的项目列表
                $scope.showProjects()
            })
        }

        $scope.getProjectTemplates = function(data){
            data.templates = $scope.templates[data.templateId]
            data
        }

        $scope.getClusterTemplates = function(data){
//            data.
        }

        $scope.randomKey = function(min, max) {
            var num = Math.floor(Math.random() * (max - min + 1)) + min;
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
            if(event.data.error){
                console.log("there is errors:" + event.data.error)
            }else{
                $scope.$apply(function(){
                    var tsData = $scope.tsData
                    for(var pIndex in $scope.projectStatus){
                        var p = $scope.projectStatus[pIndex]
                        for(var vmIndex in $scope.vms){
                            var vmName = $scope.vms[vmIndex].name
                            //envId + projectId
                            var key = $scope.activeEnv + "_" + p.id + "_" + vmName
                            var key_last = key + "_last"
                            var projectObj = tsData[key]
                            var projectObj_last = tsData[key_last]

                            if(projectObj != undefined){
                                /**
                                 * {"4_1":
                             *      {"queueNum":0,
                             *       "queues":[
                             *          {"id":1,"envId":4,"projectId":1,"versionId":2,"taskTemplateId":7,"status":3,"importTime":1406172301000,"taskId":1,"operatorId":1,"taskTemplateName":"安装应用"}
                             *        ],
                             *       "status":3,
                             *       "totalNum":9,
                             *       "currentNum":1,
                             *       "taskName":"安装应用",
                             *       "command":{"sls":"加固os","machine":"d6a597315b01"}
                             *      }
                             *  }
                                 **/
//                                p.task = projectObj
                                var pcIndex = $scope.findVmIndex(vmName, p)
                                if(p.clusters.length > 0){
                                    p.clusters[pcIndex].task = projectObj
                                    //队列
                                    if(projectObj.queues != undefined){
                                        p.clusters[pcIndex].taskQueues = projectObj.queues.filter($scope.addQueueStatusTip)
                                    }
                                }
//                            p.status.currentNum = projectObj.currentNum
//                            p.status.totalNum = projectObj.totalNum
//                            p.status.queueNum = projectObj.queueNum
//                            p.status.sls = projectObj.sls
//                            p.status.machine = projectObj.machine
//                            p.status.status = projectObj.status
//                            p.task = projectObj.task
//                            if(p.task != undefined){
//                                p.task.taskName = projectObj.taskName
//                            }
                            }
                            if(projectObj_last != undefined){
                                $scope.findLastStatus(projectObj_last)
                            }
                        }
                    }
                    $scope.projectStatus = $scope.projectStatus.map($scope.addStatusTip)
                })
            }
        }

        $scope.addQueueStatusTip = function(data){
            //只返回等待执行的任务
            if(data.status != 0){
                return null
            }
            if(!$scope.isObjEmpty(data)){
                data.statusName = $scope.explainQueueStatus(data.status)
            } else {
                data.status.statusTip = "N/A"
            }
            return data
        }

        $scope.explainQueueStatus = function(status){
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

        $scope.findVmIndex = function(name, project){
            var index = -1
            for(var pcIndex in project.clusters){
                if(project.clusters[pcIndex].name == name){
                    index = pcIndex
                }
            }
            return index
        }
        //获取envId_projectId最后一个任务状态
        $scope.findLastStatus = function(data){
            var tmp = data.split("_")
            var clusterName = null
            if(tmp.length > 2){
                clusterName = tmp[2]
            }
            TaskService.findLastStatus(tmp[0], tmp[1], clusterName, function(data){
                $scope.lastTasks = data
                if(clusterName == null){
                    $scope.projectStatus = $scope.projectStatus.map($scope.changeLastData).map($scope.addStatusTip)
                }else {
                    $scope.projectStatus = $scope.projectStatus.map($scope.changeLastDataCluster)
                }
//                $scope.mergeTemplates()
            })
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

        $scope.changeLastData = function(data){
            for(var index in $scope.lastTasks){
                var p = $scope.lastTasks[index]
                if(p.projectId === data.id){
                    data.task = p
                    data.task.queueNum = 0
                }
            }
            return data
        }
        $scope.changeLastDataCluster = function(data){
            if($scope.lastTasks.length > 0){
                for(var cIndex in $scope.lastTasks){
                    var c = $scope.lastTasks[cIndex]
                    for(var cIndex in data.clusters){
                        var cluster = data.clusters[cIndex]
                        if(c.clusterName == cluster.name){
                            data.clusters[cIndex].task = c
                            data.clusters[cIndex].task.queueNum = 0
                            data.clusters[cIndex].task.statusTip = $scope.explainTaskStatus(data.clusters[cIndex].task.status)
                        }
                    }
                }
            }
            return data
        }
        //合并两个数组 pros & projectStatus
        $scope.changeData = function(data){
            data.task = {}
            data.task.queueNum = 0
            data.task.status = 0
            for(var index in $scope.lastTasks){
                var p = $scope.lastTasks[index]
                if(p.projectId === data.id){
                    data.task = p
                    data.task.queueNum = 0
                }
            }
            return data
        }
        //增加状态描述
        $scope.addStatusTip = function(data){
            if(!$scope.isObjEmpty(data.task)){
                data.task.statusTip = $scope.explainTaskStatus(data.task.status)
            } else {
                data.task.statusTip = "N/A"
            }
            return data
        }

        //解析task status
        $scope.explainTaskStatus = function(status){
            switch(status){
                //未查询到历史任务
                case 0 : return "未查询到历史任务"
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
            if(status === 3 || status == 0) {
                return false
            } else {
                return true
            }
        }
        //未查询到任何历史任务
        $scope.showText = function(status){
            if(status == 0){
                return true
            }else {
                return false
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

        $scope.showVersionTitle = function(version){
            if(version){
                return true
            }else{
                return false
            }
        }

        $scope.showTaskNameTitle = function(taskName) {
            if(taskName){
                return true
            }else {
                return false
            }
        }
//=====================================新建任务 （部署 + 启动 + 关闭 + 重启）========================================
        $scope.showVersion = function(pid){

            $scope.versions = []
            TaskService.getVersions(pid, $scope.activeEnv, function(data){
                for(var dIndex in data) {
                    $scope.versions.push(data[dIndex])
                }
            })
        }

        $scope.showNexusVersion = function(pid){
            VersionService.getNexusVersions(pid, function(data){
                console.log(data)
            })
        }

        $scope.deploy = function(projectId, versionId, clusterName){
            $scope.taskQueue = {}
            $scope.taskQueue.envId = $scope.activeEnv
            $scope.taskQueue.projectId = projectId
            $scope.taskQueue.versionId = versionId
            $scope.taskQueue.clusterName = clusterName
            $scope.taskQueue.templateId = $scope.choosedTemplateId
            $scope.taskQueue.operatorId = Auth.user.username
            TaskService.createNewTaskQueue($scope.taskQueue, function(data){

            })
        }

        $scope.start = function(){

        }

        $scope.stop = function(){

        }

        $scope.restart = function(){

        }

        $scope.showClick = function(versionMenu, projectId, templateId, clusterName){
            if(!versionMenu){
                $scope.taskQueue = {}
                $scope.taskQueue.envId = $scope.activeEnv
                $scope.taskQueue.projectId = projectId
                $scope.taskQueue.clusterName = clusterName
                $scope.taskQueue.templateId = templateId
                $scope.taskQueue.operatorId = Auth.user.username
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
            $state.go('.queue', {envId: envId, projectId: projectId})
        }

        $scope.goTaskLog = function(taskId){
            $state.go('.log', {taskId: taskId})
        }

//=====================================终止任务========================================
        $scope.forceTerminate = function(pid, clusterName){
            TaskService.forceTerminate($scope.activeEnv, pid, clusterName, function(data){})
        }

    //=============================== new vm task =====================================
        $scope.addCluster = function(pid, envId, areaId, _ip) {
            _ip = typeof _ip === 'object' ? _ip.ip : _ip; // fix bug

            var rel = {projectId: pid, envId: envId, areaId: areaId, ip: _ip}
            ProjectService.addCluster(angular.toJson(rel), function(data) {
                if (data.r == 'ok') {
                    $scope.showVm(pid, areaId)
                    growl.addSuccessMessage("绑定成功")
                }
                else if (data.r == 'none') {
                    growl.addErrorMessage("添加失败,没有空闲机器")
                }
                else if (data.r == 'exist') {
                    growl.addErrorMessage("添加失败,该ip已经被使用")
                }
                else {
                    growl.addErrorMessage("添加失败,无效的ip")
                }
            })
        };

        $scope.removeCluster = function(pid, cid, areaId){
            var modalInstance = $modal.open({
                templateUrl: "partials/modal-message.html",
                controller: function ($scope, $modalInstance) {
                    $scope.message = "您确认要取消绑定?"
                    $scope.ok = function() {
                        ProjectService.removeCluster(cid, function(data){
                            $modalInstance.close(data);
                        })
                    };
                    $scope.cancel = function() {
                        $modalInstance.dismiss("cancel")
                    };
                }
            });
            modalInstance.result.then(function(data) {
                if(data.r == 1) {
                    $scope.showVm(pid, areaId)
                    growl.addSuccessMessage("解绑成功")
                }else {
                    growl.addErrorMessage("解绑失败");
                }
            });
        }

        $scope.isQueueShow = [];
        $scope.isHisLogShow = [];
        $scope.vms = [];

        $scope.initHosts = function(areaId) {
            RelationService.hosts($scope.activeEnv, areaId, function(data) {
                $scope.hosts = data;
            });
        };

        $scope.showVm = function(proId, areaId) {
            $scope.initHosts(areaId)
            // 根据项目proId & envId 获取关联机器
            TaskService.findClusters($scope.activeEnv, proId, function(data){
                data.map(function(t){
                    $scope.isQueueShow.push(false)
                })
                $scope.projectStatus.map(function(t){
                    if($scope.isObjEmpty(data)||(!angular.isUndefined(data[0]) && t.id == data[0].projectId)){
                        t.clusters = data
                        $scope.vms = data
                    }
                    return t
                })
                var clusterNames = $scope.vms.map(function(c){return c.name}).join(',')
                TaskService.findLastStatus($scope.activeEnv, proId, clusterNames, function(data){
                    $scope.lastTasks = data
                    $scope.projectStatus = $scope.projectStatus.map($scope.changeLastDataCluster)
//                $scope.mergeTemplates()
                })
            })
        }

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
//        var logSocket = new WS("ws://bugatti.dev.ofpay.com/#/task/joinProcess")

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

    app.controller('TaskInfoCtrl',['$scope', 'TaskService','$state','$stateParams',function($scope,TaskService,$state,$stateParams){
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

        $scope.envId_search = $stateParams.envId
        $scope.projectId_search = $stateParams.projectId

        $scope.randomKey = function(min, max) {
            var num = Math.floor(Math.random() * (max - min + 1)) + min;
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