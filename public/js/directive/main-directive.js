/*global define */

'use strict';

define(['angular'], function(angular) {

    /* Directives */

    var app = angular.module('bugattiApp.directives', []);

    // 返回上一页(浏览器历史)
    app.directive('backButton', [function() {
        return {
            restrict: 'A',
            link: function(scope, elm, attrs) {
                elm.bind('click', function () {
                    history.back();
                });
            }
        }
    }]);

    // 获取元素焦点
    app.directive('focusIf', ['$timeout', function($timeout) {
        return {
            restrict: 'A',
            link: function(scope, element, attr) {
                scope.$watch(attr.focusIf, function (v) {
                    if (v) {
                        $timeout(function() {
                            element[0].focus();
                        });
                    }
                });
            }
        };
    }]);

    // 动态获取页面窗体高度和宽度
    app.directive('resizable', function($window) {
        return {
            restrict: 'A',
            controller: function($scope) {
                $scope.ignore_h = angular.isUndefined($scope.ignore_h)? 0 : $scope.ignore_h;
            },
            link:function($scope, element, attrs) {
            // On window resize => resize the app
            $scope.initializeWindowSize = function() {
                $scope.windowHeight = $window.innerHeight - $scope.ignore_h;
                $scope.windowWidth = $window.innerWidth;
            };

            angular.element($window).bind('resize', function() {
                $scope.initializeWindowSize();
                $scope.$apply();
            });

            // Initiate the resize function default values
            $scope.initializeWindowSize();

            $scope.theStyle = function() {
                return {
                    'width': $scope.windowWidth+'px',
                    'height': $scope.windowHeight+'px',
                    'background-color': 'violet'
                };
            };
        }}
    });

    // 动态设置页面元素高度
    app.directive('bannerHeight', [function() {
        return {
            restrict: 'A',
            require: '^resizable',
            link: function ($scope, element, attrs) {
                $scope.$watch("windowHeight", function(value) {
                    element.height(value);
                });
            }
        }
    }]);

    app.directive("scroll", ['$state', '$window', '$interval', function ($state, $window, $interval) {
        return function($scope, element, attrs) {

            if (angular.isDefined($state.params.top)) {
                element.animate({scrollTop: $state.params.top}, "slow");
            }

            element.bind("scroll", function() {
                $scope.top = this.scrollTop;
                $scope.$apply();
            });

        };
    }]);

    /* input enter */
    app.directive('ngEnter', function () {
        return function (scope, element, attrs) {
            element.bind("keydown keypress", function (event) {
                if(event.which === 13) {
                    scope.$apply(function (){
                        scope.$eval(attrs.ngEnter);
                    });

                    event.preventDefault();
                }
            });
        };
    });

    /**
     * A generic confirmation for risky actions.
     * Usage: Add attributes: ng-really-message="Are you sure"? ng-really-click="takeAction()" function
     */
    app.directive('ngReallyClick', [function() {
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                element.bind('click', function() {
                    var message = attrs.ngReallyMessage;
                    if (message && confirm(message)) {
                        scope.$apply(attrs.ngReallyClick);
                    }
                });
            }
        }
    }]);

    // 用户权限列表展示
    app.directive('permission', ['UserService', function(UserService) {
        return {
            restrict: 'E',
            scope: {
                jobNo: '@'
            },
            template: '<ul class="horizonal list-group"><li class="list-group-item" ng-repeat="func in functions">{{func}}</li></ul>',
            link: function($scope, element, attrs) {
                $scope.functions = [];
                UserService.permissions($scope.jobNo, function(data) {
                    angular.forEach(data.functions, function(f) {
                        if (f === 'user') {
                            $scope.functions.push('用户管理');
                        } else if (f === 'area') {
                            $scope.functions.push('区域管理');
                        } else if (f === 'env') {
                            $scope.functions.push('环境管理');
                        } else if (f === 'project') {
                            $scope.functions.push('项目管理');
                        } else if (f === 'relation') {
                            $scope.functions.push('关系配置');
                        } else if (f === 'task') {
                            $scope.functions.push('任务管理');
                        }
                    });
                });
            }
        }
    }]);

    // 模板名称显示
    app.directive('templateShow', ['TemplateService', function(TemplateService) {
        return {
            restrict: 'E',
            scope: {
                tid: '@'
            },
            template: '<span>{{template.name}}</span>',
            link: function($scope, element, attrs) {
                TemplateService.get($scope.tid, function(data) {
                    $scope.template = data;
                });
            }
        }
    }]);

    // 项目名称显示
    app.directive('projectShow', ['ProjectService', function(ProjectService) {
        return {
            restrict: 'E',
            scope: {
                pid: '@'
            },
            template: '<span>{{project.name}}</span>',
            link: function($scope, element, attrs) {
                ProjectService.get($scope.pid, function(data) {
                    $scope.project = data;
                });
            }
        }
    }]);

    // 环境名称显示
    app.directive('envShow', ['EnvService', function(EnvService) {
        return {
            restrict: 'E',
            scope: {
                eid: '@'
            },
            template: '<span>{{env.name}}</span>',
            link: function($scope, element, attrs) {
                EnvService.get($scope.eid, function(data) {
                    $scope.env = data;
                });
            }
        }
    }]);

    // 用户名称显示
    app.directive('unameShow', ['UserService', function(UserService) {
        return {
            restrict: 'E',
            scope: {
                jobNo: '@'
            },
            template: '<span>{{user.name}}</span>',
            link: function($scope, element, attrs) {
                UserService.get($scope.jobNo, function(data) {
                    $scope.user = data;
                });
            }
        }
    }]);

    // 页面权限
    app.directive('accessPermission', ['Auth', function(Auth) {
        return {
            restrict: 'A',
            link: function($scope, element, attrs) {
                var prevDisp = element.css('display')
                    , access;
                $scope.$watch('user', function(user) {
                    updateCSS();
                }, true);
                attrs.$observe('accessPermission', function(al) {
                    access = al;
                    updateCSS();
                });
                function updateCSS() {
                    if (access) {
                        if (!Auth.authorize(access))
                            element.css('display', 'none');
                        else
                            element.css('display', prevDisp);
                    }
                }
            }
        }
    }]);

    /* 环境超级管理员 */
    app.directive('ad', ['Auth', function(Auth) {
        return {
            restrict: 'A',
            scope: false,
            link: function($scope, element, attrs) {
                $scope.isAd = true;

                attrs.$observe('hasProject', function(level) {
                    updateCSS(level)
                });
                function updateCSS(level) {
                    if (level === 'safe') {
                        if (Auth.user.role === 'admin' && Auth.user.sa === true) {
                            $scope.isAd = true;
                        } else {
                            $scope.isAd = false;
                        }
                    }
                    else {
                        $scope.isAd = false;
                    }
                }
            }
        }
    }]);


    /* 判断用户是否为项目成员 */
    app.directive('hasProject', ['Auth', 'ProjectService', function(Auth, ProjectService) {
        return {
            restrict: 'A',
            scope: false,
            link: function($scope, element, attrs) {
                $scope.hasProject_ = false;

                attrs.$observe('hasProject', function(pid) {
                    updateCSS(pid)
                });
                function updateCSS(pid) {
                    if (Auth.user.role === 'admin' && Auth.user.sa === true) {
                        $scope.hasProject_ = true;
                    }
                    else {
                        ProjectService.member(pid, Auth.user.username, function(member) {
                            if (member != null && member != 'null') {
                                $scope.hasProject_ = true;
                            }
                        })
                    }
                }
            }
        }
    }]);

    /* 判断用户是否为项目管理员 */
    app.directive('hasProjectSafe', ['Auth', 'ProjectService', function(Auth, ProjectService) {
        return {
            restrict: 'A',
            scope: false,
            link: function($scope, element, attrs) {
                $scope.hasProjectSafe_ = false;

                attrs.$observe('hasProjectSafe', function(pid) {
                    updateCSS(pid)
                });
                function updateCSS(pid) {
                    if (Auth.user.role === 'admin' && Auth.user.sa === true) {
                        $scope.hasProjectSafe_ = true;
                    }
                    else {
                        ProjectService.member(pid, Auth.user.username, function(member) {
                            if (member != null && member.level == 'safe') {
                                $scope.hasProjectSafe_ = true;
                            }
                        })
                    }
                }
            }
        }
    }]);

    /* diff比较内容 */
    app.directive('diffShow', [function() {
        if (angular.isUndefined(window.difflib)) {
            throw new Error('js diff need js difflib to work...(o rly?)');
        }
        if (angular.isUndefined(window.diffview)) {
            throw new Error('js diff need js diffview to work...(o rly?)');
        }
        return {
            restrict: 'A',
            scope: {
                baseText: '@diffBaseText',
                baseTextName: '@diffBaseTextName',
                newText: '@diffNewText',
                newTextName: '@diffNewTextName',
                viewType : '@diffViewType'
            },
            link: function(scope, iElement, iAttrs) {
                iAttrs.$observe('diffViewType', function() {
                    load()
                });
                var load = function() {
                    var base = difflib.stringAsLines(scope.baseText),
                        newtxt = difflib.stringAsLines(scope.newText),
                        sm = new difflib.SequenceMatcher(base, newtxt),
                        opcodes = sm.get_opcodes();
                    var diffViewData = diffview.buildView({
                        baseTextLines: base,
                        newTextLines: newtxt,
                        opcodes: opcodes,
                        baseTextName: scope.baseTextName,
                        newTextName: scope.newTextName,
                        viewType: scope.viewType=='1'?1:0 // the bug?
                    });
                    iElement.empty().append(diffViewData);
                }
            }
        }
    }]);

    // 区域名称
    app.directive('getArea', function() {
        return {
            restrict: 'E',
            scope: {
                aid: '@',
                areas: '='
            },
            template: '<span>{{aName}}</span>',
            controller: function($scope) {
                if (angular.isDefined($scope.areas)) {
                    angular.forEach($scope.areas, function(a, index) {
                        if (a.id == $scope.aid) {
                            $scope.aName = a.name;
                            return;
                        }
                    });
                }
            }
        }
    });

    /* task */
    app.directive('projectTabs', function () {
        return {
            restrict: 'E',
            templateUrl: 'partials/task/project-tabs.html',
            controller: function($scope) {
                $scope.tab = 1;
                $scope.isSet = function(checkTab) {
                    return $scope.tab == checkTab;
                };
                $scope.setTab = function(activeTab) {
                    $scope.tab = activeTab;
                };
            }
        };
    });

    /**
     * ------------ 项目负载 ------------
     */
    app.directive('projectBalance', function () {
        return {
            restrict: 'E',
            scope: {
                tab: "=activeTab",
                env: "=",
                project: "=",
                templates: "=",
                tsData: "=",
                c: "="
            },
            templateUrl: 'partials/task/project-balance.html',
            controller: ['$scope', 'RelationService', 'TaskService', 'AreaService', 'Auth', 'growl', 'VersionService', '$modal', 'ProjectService',
                function($scope, RelationService, TaskService, AreaService, Auth, growl, VersionService, $modal, ProjectService){
                    $scope.cTab = -1 ;
                    $scope.count = 0;
                    //------------------- 绑定 / 解绑 -------------------------
                    $scope.addCluster = function(pid, envId, _ip) {
                        _ip = typeof _ip === 'object' ? _ip.ip : _ip; // fix bug

                        var areaId = $scope.useArea.id;

                        var rel = {projectId: pid, envId: envId, areaId: areaId, ip: _ip}
                        ProjectService.addCluster(angular.toJson(rel), function(data) {
                            if (data.r == 'ok') {
                                $scope.showVm(pid)
                                growl.addSuccessMessage("绑定成功")
                            }
                            else if (data.r == 'none' && data.u == 'host') {
                                growl.addErrorMessage("添加失败,没有互斥主机，请手动添加")
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

                    $scope.removeCluster = function(pid, cid) {
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
                                $scope.showVm(pid)
                                growl.addSuccessMessage("解绑成功")
                            }else {
                                growl.addErrorMessage("解绑失败");
                            }
                        });
                    }

                    $scope.initHosts = function() {
                        var areaId = $scope.useArea.id;
                        RelationService.hosts($scope.env.id, areaId, function(data) {
                            $scope.hosts = data;
                        });
                    };

                    // 加载环境对应可用区域
                    AreaService.list($scope.env.id, function(data) {
                        $scope.preAreas = data;
                        if ($scope.preAreas.length > 0) {
                            $scope.useArea = { id :$scope.preAreas[0].id }
                            $scope.initHosts()
                        }
                    });
                    //-------------------- 任务状态解析 -----------------------
                    //解析每个负载的最后一次任务
                    $scope.changeLastDataCluster = function(cluster){
                        if($scope.lastTasks.length > 0){
                            for(var cIndex in $scope.lastTasks){
                                var c = $scope.lastTasks[cIndex]
                                if(c.clusterName == cluster.name){
                                    cluster.task = c
                                    cluster.task.queueNum = 0
                                    cluster.task.statusTip = $scope.explainTaskStatus(cluster.task.status)
                                }
                            }
                        }
                        return cluster
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

                    //判断对象是否为空
                    $scope.isObjEmpty = function(obj){
                        for (var name in obj)
                        {
                            return false
                        }
                        return true
                    }

                    //查询绑定负载
                    $scope.showVm = function(proId) {
                        // 根据项目proId & envId 获取关联机器
                        TaskService.findClusters($scope.env.id, proId, function(data){
                            $scope.project.clusters = data
                            $scope.vms = data
                            var clusterNames = $scope.vms.map(function(c){return c.name}).join(',')
                            TaskService.findLastStatus($scope.env.id, proId, clusterNames, function(data){
                                $scope.lastTasks = data
                                $scope.project.clusters.map($scope.changeLastDataCluster)
                            })
                        })
                        //-------操作按钮展示---------
                        $scope.project.templates = $scope.templates[$scope.project.templateId]
                    }

                    $scope.showVm($scope.project.id);

                    //------------------- 手动终止正在执行的任务 --------------------------------
                    $scope.forceTerminate = function(pid, clusterName){
                        TaskService.forceTerminate($scope.env.id, pid, clusterName, function(data){})
                    }

//--------------------------------------- 操作按钮逻辑 -----------------------------------------

                    $scope.showVersion = function(pid){
                        $scope.versions = []
                        VersionService.getVersions(pid, $scope.env.id, function(data){
                            $scope.versions = data
                        })
                    }

                    $scope.showMenu = function(versionMenu, projectId, clusterName, templateId){
                        if(!versionMenu){
                            $scope.deploy(projectId, null, clusterName, templateId)
                        } else {//部署
                            $scope.choosedTemplateId = templateId
                            $scope.showVersion(projectId)
                        }
                    }

                    $scope.deploy = function(projectId, versionId, clusterName, templateId){
                        $scope.taskQueue = {}
                        $scope.taskQueue.envId = $scope.env.id
                        $scope.taskQueue.projectId = projectId
                        $scope.taskQueue.versionId = versionId
                        $scope.taskQueue.clusterName = clusterName
                        $scope.taskQueue.templateId = templateId
                        $scope.taskQueue.operatorId = Auth.user.username
                        TaskService.createNewTaskQueue($scope.taskQueue, function(data){
                            if(data == -1){
                                growl.addErrorMessage("模板已更新，请刷新页面");
                            }
                        })
                    }
//-------------------------------------- 任务状态 ---------------------------------------------
                    //根据任务状态判断是否需要显示超链接
                    $scope.showTaskHref = function(status){
                        if(status === 3 || status == 0) {
                            return false
                        } else {
                            return true
                        }
                    }
                    //未查询到任何历史任务
                    $scope.showTaskText = function(status){
                        if(status == 0){
                            return true
                        }else {
                            return false
                        }
                    }
                    //根据任务状态判断是否需要显示进度条
                    $scope.showTaskProgress = function(status){
                        if(status === 3){
                            return true
                        } else {
                            return false
                        }
                    }
                    //--------------- 正在进行的任务 --------------------
                    //获取envId_projectId最后一个任务状态
                    $scope.findLastStatus = function(vmName, vmIndex){
                        TaskService.findLastStatus($scope.env.id, $scope.project.id, vmName, function(data){
                            $scope.lastTasks = data
                            $scope.project.clusters.map($scope.changeLastDataCluster)
                        })
                    }

                    $scope.refreshStatus = function(){
                        var tsData = $scope.tsData;
                        if($scope.tsData != undefined){
                            var clusters = $scope.project.clusters;
                            for(var vmIndex in clusters){
                                var vmName = clusters[vmIndex].name;
                                var key = $scope.env.id + "_" + $scope.project.id + "_" + vmName;
                                var key_last = key + "_last";
                                var projectObj = tsData[key]
                                var projectObj_last = tsData[key_last]

                                if(projectObj != undefined){
                                    $scope.project.clusters[vmIndex].task = projectObj;
                                    //队列
                                    if(projectObj.queues != undefined){
                                        $scope.project.clusters[vmIndex].taskQueues = projectObj.queues.filter($scope.addQueueStatusTip)
                                    }
                                }
                                if(projectObj_last != undefined){
                                    $scope.findLastStatus(vmName, vmIndex)
                                }
                            }
                        }
                    }

//=================================== cluster tab ===============================
                    $scope.isTrShow = function(cIndex){
                        return $scope.chooseIndex == cIndex && $scope.clusterTabStatus[$scope.chooseIndex + "_" + $scope.cTab];
                    }

                    $scope.clusterTabStatus = {}

                    $scope.chooseClusterTab = function(cTab, chooseIndex, taskId){
                        $scope.cTab = cTab;
                        $scope.chooseIndex = chooseIndex;
                        $scope.taskId = taskId;
                        $scope.clusterTabStatus[chooseIndex + "_" + cTab] = !$scope.clusterTabStatus[chooseIndex + "_" + cTab]
                    }


                    //======
                }
            ],
            link: function postLink(scope, iElement, iAttrs) {
                scope.$watch('tsData', function () {
                    scope.refreshStatus();
                });
            }
        }
    });

    app.directive('clusterTabs', function(){
        return {
            restrict: 'E',
            require: 'projectBalance',
            scope: {
                cTab: "=",
                cIndex: "=",
                c: "=",
                project: "=",
                env: "=",
                clusterTabStatus: "=",
                chooseIndex: "="
            },
            templateUrl: 'partials/task/cluster-tabs.html',
            controller: function($scope){
                $scope.isClusterTabShow = function(ctab, c_index){
                    return $scope.cTab == ctab && $scope.chooseIndex == c_index && $scope.clusterTabStatus[$scope.chooseIndex + "_" + $scope.cTab];
                }
            }
        }
    });

    app.directive('logsTabs', function(){
        return {
            restrict: 'E',
            templateUrl: 'partials/task/logs-tabs.html',
            controller: function($scope){
                $scope.ltab = 1;
                $scope.isLogSet = function(ltab){
                    return $scope.ltab == ltab;
                }
                $scope.setLogTab = function(ltab){
                    $scope.ltab = ltab;
                }
            }
        }
    });

    app.directive('hisTabs', function(){
        return {
            restrict: 'E',
            scope: {
                project: "=",
                env: "=",
                c:"=",
                sTab: "=",
                sIndex: "=",
                hisTabStatus: "=",
                chooseIndex: "="
            },
            templateUrl: 'partials/task/his-tabs.html',
            controller: function($scope){
                $scope.isHisShow = function(sTab, sIndex){
                    console.log($scope.sTab, sTab, $scope.chooseIndex, sIndex)
                    return $scope.sTab == sTab && $scope.chooseIndex == sIndex && $scope.hisTabStatus[$scope.chooseIndex + "_" + $scope.sTab];
                }
            }
        }
    });

    app.directive('taskQueue', function(){
        return {
            restrict: 'E',
            scope: {
                c: "=",
                project: "="
            },
            templateUrl: 'partials/task/task-queue.html',
            controller: ['$scope', 'TaskService',
                function($scope, TaskService){
                    $scope.removeQueue = function(qid){
                        TaskService.removeTaskQueue(qid, function(data){
                            //TODO 如果删除的任务在一瞬间刚好变为正在执行，应告知
                        })
                    }
                }]
        }
    });

    app.directive('clusterProperties', function(){
        return {
            restrict: 'E',
            scope: {
                project: "=",
                env: "=",
                c: "="
            },
            templateUrl: 'partials/task/cluster-properties.html',
            controller: ['$scope', '$stateParams', '$state', '$modal', 'RelationService', 'ProjectService', 'EnvService', 'growl',
                function($scope, $stateParams, $state, $modal, RelationService, ProjectService, EnvService, growl) {
                    $scope.delayLoadProperties = function(){
                        RelationService.get($scope.c.id, function(data) {
                            $scope.relation = data;
                            if ($scope.relation) {
                                if (angular.isUndefined($scope.project.id) || angular.isUndefined($scope.env.id) ) {
                                    return;
                                }
                                ProjectService.vars($scope.project.id, $scope.env.id, function(project_vars) {
                                    $scope.vars = project_vars;
                                    angular.forEach($scope.vars, function(pv) {
                                        pv.meta = pv.value;
                                        var defVar = findInVars($scope.relation.globalVariable, pv);
                                        if (defVar !== '') {
                                            pv.meta = defVar;
                                            pv.value = defVar;
                                        } else {
                                            pv.value = '';
                                        }
                                    });
                                });
                            }
                        });
                    };

                    function findInVars(vars, v) {
                        var find = '';
                        angular.forEach(vars, function(_v, index) {
                            if (_v.name == v.name) {
                                find = _v.value;
                                return;
                            }
                        });
                        return find;
                    }

                    $scope.saveOrUpdateProperties = function(vars) {
                        $scope.relation.globalVariable = [];
                        angular.forEach(vars, function(v) {
                            $scope.relation.globalVariable.push({name: v.name, value: v.value})
                        });
                        RelationService.update($scope.c.id, $scope.relation, function(data) {
                            if(data == 1){
                                growl.addSuccessMessage("修改成功")
                            }else {
                                growl.addErrorMessage("修改失败");
                            }
                        });
                    };
                    $scope.delayLoadProperties();
                }]
//            ,
//            link: function postLink(scope, iElement, iAttrs) {
//                scope.$watch('cIndex', function () {
//                    if (scope.cTab == 1 && scope.cIndex == scope.$index) {
//                        scope.delayLoadProperties();
//                    }
//                });
//            }
        }
    });

    app.directive('catalinaLog', function(){
        return {
            restrict: 'E',
//            require: '^projectBalance',
            templateUrl: 'partials/task/catalina-log.html',
            controller: ['$scope', 'TaskService',
                function($scope, TaskService){
                    $scope.delayLoadCatalinaLog = function(){
                        $scope.catalinaMessage = "正在努力加载中,请稍后..."
                        var WS = window['MozWebSocket'] ? MozWebSocket: WebSocket;
                        $scope.logType = "catalina";
                        if($scope.ltab == 1){
                            $scope.logType = "catalina";
                        } else if($scope.ltab == 2) {
                            $scope.logType = "intflog";
                        } else if($scope.ltab == 3){
                            $scope.logType = "applog";
                        }
                        TaskService.getCatalinaWS($scope.activeEnv, function(data){
                            if($scope.hostName != undefined){
                                var indexofdot = $scope.hostName.lastIndexOf(".");
                                var path =
                                    data
//                                    "ws://localhost:3232"
                                    + "/"
                                    + $scope.hostName.substring(0, indexofdot == -1 ?  $scope.hostName.length : indexofdot)
                                    + "/"
                                    + $scope.logType

                                $scope.closeWSCatalina();
                                $scope.catalinaLogSocket = new WS(path)
                                $scope.catalinaLogSocket.onmessage = $scope.receiveCatalina
                                $scope.catalinaLogSocket.onerror = $scope.errorCatalina
                                $scope.catalinaLogSocket.onclose = $scope.closeCatalina
                                $scope.catalinaMessage = ""
                            }
                        })
                    }

                    $scope.closeCatalina = function(event){
                        console.log("websocket is closed !")
                    }

                    $scope.errorCatalina = function(err){
                        console.log(err)
                        $scope.catalinaMessage = err.srcElement.URL + "连接失败";
                    }
                    $scope.receiveCatalina = function(event){
                        var messageJson = angular.fromJson(event.data)
                        $scope.catalinaMessage = $scope.catalinaMessage +
                        messageJson["@timestamp"] +
                        " [" + messageJson.thread_name + "] " +
                        messageJson.level +
                        " " +
                        $scope.getLog(messageJson) +
                        "\n"
                    }

                    $scope.getLog = function(messageJson){
                        if($scope.logType == "catalina"){
                            return messageJson["logger_name"] + " - " + messageJson.message ;
                        } else if($scope.logType == "intflog"){
                            return messageJson["methodName"] + " - " +
                                messageJson["paramTypes"] + " - " +
                                messageJson["paramValues"] + " - " +
                                messageJson["receiverHost"] + " - " +
                                messageJson["receiverName"] + " - " +
                                messageJson["resultValue"] + " - " +
                                messageJson["senderHost"] + " - " +
                                messageJson["source"] + " - " +
                                messageJson["srvGroup"] + " - " +
                                messageJson["message"] + " - "
                        } else if($scope.logType == "applog"){
                            return messageJson["source"] + " - " +
                                messageJson["caller_class_name"] + " - " +
                                "line " + messageJson["caller_line_number"] + " - " +
                                messageJson["message"] + " - "
                        } else {
                            return ""
                        }
                    }

                    $scope.closeWSCatalina = function(){
                        console.log("$scope.closeWSCatalina is invoked")
                        if($scope.catalinaLogSocket){
                            console.log("$scope.catalinaLogSocket is closing")
                            $scope.catalinaLogSocket.close();
                        }
                    }

                }],
            link: function postLink(scope, iElement, iAttrs){
                scope.$watch('ltab', function(){
                    scope.delayLoadCatalinaLog();
                })

                scope.$watch('catalina_ctab', function(){
                    if(scope.ctab == 4 && scope.c_index == scope.$index){
                        scope.delayLoadCatalinaLog();
                    }else {
                        scope.closeWSCatalina();
                    }
                })
            }
        }
    });

    app.directive('taskLog', function(){
        return {
            restrict: 'E',
            scope:{
                c: "=",
                env: "=",
                project: "="
            },
            templateUrl: 'partials/task/task-log.html',
            controller:['$scope', 'TaskService','$state','$stateParams',
                function($scope,TaskService,$state,$stateParams){
                    $scope.delayLoadLog = function(){
                        if($scope.c.task.id != undefined){
                            TaskService.LogReader($scope.env.id, $scope.project.id, $scope.c.task.id, function(data){
                                $scope.logHeader = data.logHeader
                                $scope.logContent = data.logContent
                                $scope.logHeaderShow = true
                            })
                        }
                    }

                    $scope.showHiddenMessage = function(){
                        var len = parseInt($scope.logHeader.split(" ")[0])
                        TaskService.readHeader($scope.env.id, $scope.project.id, $scope.c.task.id, len, function(data){
                            $scope.logHeaderShow = false
                            $scope.logContent = data + $scope.logContent
                        })
                    }
                    $scope.delayLoadLog();
                }]
        }
    });

    /**
     * ------------ 项目属性 ------------
     */
    app.directive('projectItem', function () {
        return {
            restrict: 'E',
            require: '^projectTabs',
            scope: {
                tab: "=activeTab",
                env: "=expanderEnv",
                project: "=expanderProject"
            },
            templateUrl: 'partials/task/project-item.html',
            controller: ['$scope', '$filter', 'growl', 'ProjectService', 'TemplateService',
                function($scope, $filter, growl, ProjectService, TemplateService) {
                    // project variable
                    $scope.vars = [];
                    $scope.addVar = function(v) {
                        $scope.varForm.varName.$error.unique = false;
                        $scope.varForm.varName.$error.required = false;
                        $scope.varForm.varValue.$error.required = false;

                        if (angular.isUndefined($scope.env.id)) {
                            return;
                        }
                        v.envId = $scope.env.id;   // bind env

                        if (findInVars($scope.vars, v) != -1) {
                            $scope.varForm.varName.$invalid = true;
                            $scope.varForm.varName.$error.unique = true;
                            return;
                        }
                        if (v.name.trim().length < 1 && v.value.trim().length < 1) {
                            $scope.varForm.varName.$invalid = true;
                            $scope.varForm.varValue.$invalid = true;
                            $scope.varForm.varName.$error.required = true;
                            $scope.varForm.varValue.$error.required = true;
                            return;
                        }
                        if (v.name.trim().length < 1 ) {
                            $scope.varForm.varName.$invalid = true;
                            $scope.varForm.varName.$error.required = true;
                            return;
                        }
                        if (v.value.trim().length < 1) {
                            $scope.varForm.varValue.$invalid = true;
                            $scope.varForm.varValue.$error.required = true;
                            return;
                        }

                        $scope.vars.push(angular.copy(v));
                        v.name = "", v.value = ""; // clear input value
                    };

                    function findInVars(vars, v) {
                        var find = -1;
                        angular.forEach(vars, function(_v, index) {
                            if (_v.name == v.name && _v.envId == v.envId) {
                                find = index;
                                return;
                            }
                        });
                        return find;
                    }

                    $scope.editVar = function(repeat$scope) {
                        repeat$scope.mode = 'edit';
                    };

                    $scope.deleteVar = function(v) {
                        var index = findInVars($scope.vars, v)
                        if (index != -1) {
                            $scope.vars.splice(index, 1);
                        }
                    };

                    $scope.initItemData = function() {
                        // attrs
                        TemplateService.itemAttrs($scope.project.templateId, $scope.env.scriptVersion, function(data) {
                            $scope.items = data;
                            ProjectService.atts($scope.project.id, function(project_attrs) {
                                angular.forEach($scope.items, function(item) {
                                    angular.forEach(project_attrs, function(att) {
                                        if (att.name == item.itemName) {
                                            item.value = att.value;
                                            item.id = att.id;
                                            return;
                                        }
                                    });
                                });
                            });
                        });

                        // variables
                        TemplateService.itemVars($scope.project.templateId, $scope.env.scriptVersion, function(item_vars) {
                            var _vars = angular.copy($scope.vars);
                            angular.forEach(_vars, function(v, index) {
                                if (v.name.indexOf('t_') === 0) {
                                    delete _vars[index]; // delete object is null
                                }
                            });
                            _vars = _vars.filter(function(e){return e}); // clear null

                            // load init variable
                            ProjectService.vars($scope.project.id, $scope.env.id, function(project_vars) {
                                if (project_vars.length < 1) {
                                    angular.forEach(item_vars, function(iv) {
                                        _vars.push({name: iv.itemName, value: '', envId: $scope.env.id});  // first add
                                    });
                                }
                                else if (item_vars.length < 1) {
                                    angular.forEach(project_vars, function(pv) {
                                        _vars.push({name: pv.name, value: pv.value, envId: $scope.env.id});  // first add
                                    });
                                }
                                else {
                                    angular.forEach(item_vars, function(iv) {
                                        var replaceFlag = false;
                                        project_vars.map(function(pv){
                                            if(pv.name == iv.itemName && pv.envId == $scope.env.id){
                                                replaceFlag = true;
                                                _vars.unshift({name: pv.name, value: pv.value, envId: $scope.env.id});
                                            }
                                        })
                                        if(!replaceFlag){
                                            _vars.push({name: iv.itemName, value: '', envId: $scope.env.id});
                                        }
                                    });
                                }
                            });
                            $scope.vars = _vars;
                        });

                        // update
                        $scope.saveOrUpdate = function(project) {
                            project.items = [];
                            project.variables = angular.copy($scope.vars);
                            angular.forEach($scope.items, function(item) {
                                project.items.push({name: item.itemName, value: item.value, id: item.id})
                            });

                            project.lastUpdated = $filter('date')(project.lastUpdated, "yyyy-MM-dd HH:mm:ss")
                            ProjectService.update($scope.project.id, $scope.env.id, angular.toJson(project), function(data) {
                                if (data.r === 'exist') {
                                    $scope.form.name.$invalid = true;
                                    $scope.form.name.$error.exists = true;
                                } else {
                                    growl.addSuccessMessage("成功");
                                }
                            });

                        };

                    }
                }],
            link: function postLink(scope, iElement, iAttrs) {
                scope.$watch('tab', function () {
                    if (scope.tab === 2) {
                        scope.initItemData();
                    }
                });
            }
        }
    });

    /**
     * ------------ 项目配置 ------------
     */
    app.directive('projectConf', function () {
        return {
            restrict: 'E',
            require: '^projectTabs',
            scope: {
                tab: "=activeTab",
                env: "=expanderEnv",
                envs: "=expanderEnvs",
                project: "=expanderProject"
            },
            templateUrl: 'partials/task/project-conf.html',
            controller: ['$scope', '$filter', 'ConfService', 'VersionService', '$modal', 'growl',
                function($scope, $filter, ConfService, VersionService, $modal, growl) {
                    $scope.initVersions = function() {
                        VersionService.getVersions($scope.project.id, $scope.env.id, function(data) {
                            $scope.versions = data;
                            $scope.versions.unshift({id: 0, projectId: $scope.project.id, updated: 1417228773000, vs: 'default'});

                        });
                    };

                    $scope.changeVersion = function(vid) {
                        $scope.setAction('list');
                        if (vid == null) {
                            $scope.confs = [];
                        } else {
                            ConfService.getAll($scope.env.id, $scope.project.id, vid, function(data) {
                                $scope.confs = data;
                            });
                        }
                    };

                    $scope.action = 'list';
                    $scope.isAction = function(checkTab) {
                        return $scope.action === checkTab;
                    };
                    $scope.setAction = function(_tab, conf_id) {
                        $scope.action = _tab;
                        $scope.conf_id = conf_id;
                    };

                    // ace editor
                    $scope.wordList = [];
                    var langTools = ace.require("ace/ext/language_tools");
                    $scope.aceLoaded = function(_editor) {

                        _editor.setOptions({
                            enableBasicAutocompletion: true
                        });
                        _editor.getSession().setMode("ace/mode/properties");
                        _editor.commands.bindKey("Ctrl-Space|Ctrl-Shift-Space|Alt-Space", null); // do nothing on ctrl-space
                        _editor.commands.bindKey("F1|Command-Enter", "startAutocomplete");

                        var codeCompleter = {
                            getCompletions: function(editor, session, pos, prefix, callback) {
                                if (prefix.length === 0) { callback(null, []); return }
                                callback(null, $scope.wordList.map(function(ea) {
                                    return {name: ea.word, value: ea.word, score: ea.score, meta: ea.meta}
                                }));
                            }
                        };
                        langTools.addCompleter(codeCompleter);
                    };

                    // 生成模板
                    $scope.genTemplateConf = function() {
                        var copyParam = {projectId: $scope.project.id, target_eid: $scope.env.id, target_vid: $scope.versionId, envId: $scope.env.id, versionId: 0, ovr: true, copy: false};
                        var modalInstance = $modal.open({
                            templateUrl: "partials/modal-message.html",
                            controller: function($scope, $modalInstance){
                                $scope.message = "把当前环境所有配置文件生成模板?";
                                $scope.ok = function() {
                                    ConfService.copy(angular.toJson(copyParam), function(data) {
                                        $modalInstance.close(data);
                                    });
                                };
                                $scope.cancel = function() {
                                    $modalInstance.dismiss("cancel");
                                }
                            }
                        });

                        modalInstance.result.then(function(data) {
                            if (data.r === 'ok') {
                                growl.addSuccessMessage("成功");
                            } else if (data.r === 'exist') {
                                growl.addWarnMessage("内容已存在");
                            }
                        });
                    }

                }],
            link: function postLink(scope, iElement, iAttrs) {
                scope.$watch('tab', function () {
                    if (scope.tab === 3) {
                        scope.initVersions();
                    }
                });
            }
        }
    });

    /**
     * ------------ 项目依赖 ------------
     */
    app.directive('projectDependency', function(){
        return {
            restrict: 'E',
            require: '^projectTabs',
            scope: {
                tab: "=activeTab",
                project: "=expanderProject"
            },
            templateUrl: 'partials/task/project-dependency.html',
            controller: ['$scope', '$stateParams', '$filter', '$state', 'DependencyService', 'ProjectService', 'growl',
                function($scope, $stateParams, $filter, $state, DependencyService, ProjectService, growl){
                    $scope.showDependencies = function(){
                        DependencyService.get($scope.project.id, function(data){
                            $scope.groups = data
                        })
                    };
                    $scope.delayLoadDependency = function(){
                        ProjectService.getExceptSelf($scope.project.id, function(data){
                            $scope.projects = data ;
                            $scope.showDependencies() ;
                        })
                    };

                    $scope.removeDependency = function(parent,child){
                        DependencyService.removeDependency(parent.id, child.id, function(data){
                            $scope.showDependencies()
                        })
                    };

                    $scope.addDependency = function(parent,child){
                        DependencyService.addDependency(parent, child, function(data){
                            if(data.r == 0){
                                growl.addWarnMessage("添加失败");
                            }
                            $scope.showDependencies()
                        })
                    };

                    $scope.templateFilter = function(dep){
                        return function(p){return p.templateId == dep.templateId};
                    };

                    $scope.getTemplateProject = function(dep){
                        var subTemplateProjects = $scope.projects.map(
                            function(p){
                                if(p.name == dep.name) {
                                    return p;
                                }
                            }
                        ).filter(function(e){return e})
                        if(subTemplateProjects.length > 0){
                            return subTemplateProjects[0];
                        }
                    };

                    $scope.changeTemplateProject = function(parentId, oldId, newId){
                        if(newId != undefined){
                            DependencyService.changeTemplateProject(parentId, oldId, newId, function(data){
                                if(data.r == 0){
                                    growl.addWarnMessage("修改失败");
                                } else if(data.r == 1){
                                    growl.addSuccessMessage("修改成功");
                                }
                            })
                        }
                    }
                }
            ],
            link: function postLink(scope, iElement, iAttrs) {
                scope.$watch('tab', function () {
                    if (scope.tab === 4) {
                        scope.delayLoadDependency();
                    }
                });
            }
        }
    });

    /**
     * ------------ 项目成员 ------------
     */
    app.directive('projectMember', function(){
        return {
            restrict: 'E',
            require: '^projectTabs',
            scope: {
                tab: "=activeTab",
                project: "=expanderProject"
            },
            templateUrl: 'partials/task/project-member.html',
            controller: ['$scope', '$stateParams', '$modal', 'ProjectService',
                function($scope, $stateParams, $modal, ProjectService) {
                    // ---------------------------------------------
                    // 项目成员管理
                    // ---------------------------------------------
                    $scope.delayLoadMember = function(){
                        ProjectService.members($scope.project.id, function(data) {
                            $scope.members = data;
                        });
                    };

                    $scope.addMember = function(jobNo) {
                        $scope.jobNo$error = '';
                        if (!/^of[0-9]{1,10}$/i.test(jobNo)) {
                            $scope.jobNo$error = '工号格式错误';
                            return;
                        }
                        var exist = false;
                        angular.forEach($scope.members, function(m) {
                            if (m.jobNo === jobNo) {
                                exist = true;
                            }
                        });
                        if (exist) {
                            $scope.jobNo$error = '已存在';
                            return;
                        }

                        ProjectService.saveMember($scope.project.id, jobNo, function(data) {
                            if (data.r === 'none') {
                                $scope.jobNo$error = '用户不存在';
                            }
                            else if (data.r === 'exist') {
                                $scope.jobNo$error = '已存在用户';
                            } else if (data > 0) {
                                ProjectService.members($scope.project.id, function(data) {
                                    $scope.members = data;
                                    $scope.jobNo$error = '';
                                });
                            }
                        });
                    };

                    $scope.memberUp = function(mid, msg) {
                        if (confirm(msg)) {
                            ProjectService.updateMember(mid, "up", function(data) {
                                ProjectService.members($scope.project.id, function(data) {
                                    $scope.members = data;
                                });
                            });
                        }
                    };
                    $scope.memberDown = function(mid, msg) {
                        if (confirm(msg)) {
                            ProjectService.updateMember(mid, "down", function(data) {
                                ProjectService.members($scope.project.id, function(data) {
                                    $scope.members = data;
                                });
                            });
                        }
                    };
                    $scope.memberRemove = function(mid, msg) {
                        if (confirm(msg)) {
                            ProjectService.updateMember(mid, "remove", function(data) {
                                ProjectService.members($scope.project.id, function(data) {
                                    $scope.members = data;
                                });
                            });
                        }
                    };
                }],
            link: function postLink(scope, iElement, iAttrs) {
                scope.$watch('tab', function () {
                    if (scope.tab === 5) {
                        scope.delayLoadMember();
                    }
                });
            }
        }
    });

    /**
     * ------------ 项目历史任务 ------------
     */
    app.directive('projectHistory', function(){
        return{
            restrict: 'E',
            require: '^projectTabs',
            scope: {
                tab: "=activeTab",
                env: "=expanderEnv",
                project: "=expanderProject"
            },
            templateUrl: 'partials/task/task-history.html',
            controller: ['$scope', 'TaskService',
                function($scope, TaskService){
                    $scope.hisTasks = [];
                    $scope.isHisLogShow = [];

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

                    //判断对象是否为空
                    $scope.isObjEmpty = function(obj){
                        for (var name in obj)
                        {
                            return false
                        }
                        return true
                    }
                    $scope.delayLoadHistory = function() {
                        TaskService.findHisTasks($scope.env.id, $scope.project.id, function(data){
                            $scope.hisTasks = data.map($scope.addStatusTipHistory)
                        });
                    };

                    $scope.addStatusTipHistory = function(data){
                        if(!$scope.isObjEmpty(data)){
                            data.statusTip = $scope.explainTaskStatus(data.status)
                            if(data.status == 1){
                                data.color = {"color": "green"}
                            }else if(data.status == 2){
                                data.color = {"color": "red"}
                            }else {
                                data.color = {"color": "yellow"}
                            }
                        } else {
                            data.statusTip = "N/A"
                        }
                        return data
                    };

//------------------------------ log tabs ---------------------------
                    $scope.hisTabStatus = {}

                    $scope.isHisLogShow = function(sIndex){
                        return $scope.chooseIndex == sIndex && $scope.hisTabStatus[$scope.chooseIndex + "_" + $scope.sTab];
                    }

                    $scope.showHisLog = function(chooseIndex, sTab, taskId){
                        $scope.chooseIndex = chooseIndex;
                        $scope.sTab = sTab;
                        $scope.hisTabStatus[chooseIndex + "_" + sTab] = !$scope.hisTabStatus[chooseIndex + "_" + sTab];
                        $scope.c= {task: {id: taskId}};
                    }

                    $scope.delayLoadHistory();
                }
            ]
        }
    });

    /**
     * ------------ 项目配置 ------------
     * 文件列表
     */
    app.directive('confList', function() {
        return {
            restrict: 'E',
            require: '^projectConf',
            templateUrl: 'partials/conf/project/uiview/conf-list.html',
            controller: ['$scope', function($scope) {
                // parent
            }],
            link: function postLink(scope, iElement, iAttrs) {
                scope.$watch('action', function () {
                    if (scope.action === 'list' && !angular.isUndefined(scope.versionId)) {

                        scope.changeVersion(scope.versionId)
                    }
                });
            }
        }
    });

    /**
     * ------------ 项目配置 ------------
     * 文件详情
     */
    app.directive('confDetail', function() {
        return {
            restrict: 'E',
            require: '^projectConf',
            templateUrl: 'partials/conf/project/uiview/conf-show.html',
            controller: ['$scope', '$modal', 'ConfService', function($scope, $modal, ConfService) {
                $scope.initConfData = function() {

                    ConfService.get($scope.conf_id, function(data) {
                        $scope.conf = data.conf;
                        $scope.confContent = data.confContent;
                    });

                    // remove
                    this.delete = function(cid) {
                        var modalInstance = $modal.open({
                            templateUrl: 'partials/modal.html',
                            controller: function ($scope, $modalInstance) {
                                $scope.ok = function () {
                                    ConfService.remove(cid, function(data) {
                                        $modalInstance.close(data);
                                    });
                                };
                                $scope.cancel = function () {
                                    $modalInstance.dismiss('cancel');
                                };
                            }
                        });
                        modalInstance.result.then(function(data) {
                            $scope.setAction('list');
                        });
                    };

                    $scope.aceLoaded = function(_editor) {
                        // Editor part
                        var _session = _editor.getSession();
                        var fileName = $scope.conf ? $scope.conf.name : undefined;
                        if (fileName) {
                            var suffix = fileName.substring(fileName.lastIndexOf('.') + 1);
                            if (suffix == 'conf') {suffix = 'properties'}
                            suffix = "ace/mode/" + suffix;
                            _session.setMode(suffix);
                        }
                    };

                }
            }],
            link: function postLink(scope, iElement, iAttrs) {
                scope.$watch('action', function () {
                    if (scope.action === 'detail') {
                        scope.initConfData();
                    }
                });
            }
        }
    });

    /**
     * ------------ 项目配置 ------------
     * 文件修改
     */
    app.directive('confEdit', function() {
        return {
            restrict: 'E',
            require: '^projectConf',
            templateUrl: 'partials/conf/project/uiview/conf-edit.html',
            controller: ['$scope', '$filter', 'ConfService', function($scope, $filter, ConfService) {
                $scope.initEditConf = function() {

                    ConfService.get($scope.conf_id, function(data) {
                        $scope.conf = data.conf;
                        $scope.conf.content = data.confContent.content;


                        ConfService.completer($scope.conf.envId, $scope.conf.projectId, $scope.conf.versionId, function(data) {
                            var obj = eval(data);
                            for (var prop in obj) {
                                $scope.wordList.push({'word': prop, 'score': 0, meta: obj[prop]});
                            }
                        });
                    });

                    this.update = function() {
                        $scope.conf.updated = $filter('date')(new Date(), "yyyy-MM-dd HH:mm:ss")
                        ConfService.update($scope.conf.id, angular.toJson($scope.conf), function(data) {
                            if (data.r === 'exist') {
                                $scope.editForm.path.$invalid = true;
                                $scope.editForm.path.$error.exists = true;
                            } else {
                                $scope.setAction('list');
                            }
                        });
                    };

                }
            }],
            link: function postLink(scope, iElement, iAttrs) {
                scope.$watch('action', function () {
                    if (scope.action === 'edit') {
                        scope.initEditConf();
                    }
                });
            }
        }
    });

    /**
     * ------------ 项目配置 ------------
     * 文件创建
     */
    app.directive('confCreate', function() {
        return {
            restrict: 'E',
            require: '^projectConf',
            templateUrl: 'partials/conf/project/uiview/conf-new.html',
            controller: ['$scope', '$filter', 'ConfService', function($scope, $filter, ConfService) {
                $scope.initCreate = function() {
                    $scope.conf = {envId: $scope.env.id, projectId: $scope.project.id, versionId: $scope.versionId};

                    this.save = function() {
                        $scope.conf.updated = $filter('date')(new Date(), "yyyy-MM-dd HH:mm:ss")
                        ConfService.save(angular.toJson($scope.conf), function(data) {
                            if (data.r === 'exist') {
                                $scope.newForm.path.$invalid = true;
                                $scope.newForm.path.$error.exists = true;
                            } else {
                                $scope.setAction('list');
                            }
                        });
                    };

                    ConfService.completer($scope.conf.envId, $scope.conf.projectId, $scope.conf.versionId, function(data) {
                        var obj = eval(data);
                        for (var prop in obj) {
                            $scope.wordList.push({'word': prop, 'score': 0, meta: obj[prop]});
                        }
                    });

                }
            }],
            link: function postLink(scope, iElement, iAttrs) {
                scope.$watch('action', function () {
                    if (scope.action === 'create') {
                        scope.initCreate();
                    }
                });
            }
        }
    });

    /**
     * ------------ 项目配置 ------------
     * 一键拷贝
     */
    app.directive('confCopy', function() {
        return {
            restrict: 'E',
            require: '^projectConf',
            templateUrl: 'partials/conf/project/uiview/conf-copy.html',
            controller: ['$scope', 'growl', 'VersionService', 'ConfService', function($scope, growl, VersionService, ConfService) {
                $scope.initCopyConf = function() {
                    $scope.copyEnvs = angular.copy($scope.envs);
                    //$scope.copyEnvs.unshift({"id": 0, "name": '模板配置', "nfServer": '', "ipRange": '', "level": 'safe', "scriptVersion": '', "jobNo": '', "remark": ''})

                    $scope.copyParam = {projectId: $scope.project.id, target_eid: $scope.env.id, target_vid: null, envId: $scope.env.id, versionId: $scope.versionId, ovr: false};

                    VersionService.top($scope.project.id, function(data) {
                        $scope.versions = data;
                        // 模板
                        $scope.versions.unshift({id: 0, projectId: $scope.project.id, updated: 1417228773000, vs: 'default'});

                        // default current version
                        var find = false;
                        angular.forEach($scope.versions, function(v) {
                            if (!find && v.id == $scope.copyParam.versionId) {
                                $scope.copyParam.target_vid = v.id;
                                find = true;
                                return;
                            }
                        });
                    });

                    this.ok = function (param) {
                        //if (param.target_vid == 0) { // 模板
                        //    param.target_eid = $scope.env.id;
                        //}
                        ConfService.copy(angular.toJson(param), function(data) {
                            if (data.r === 'ok') {
                                $scope.setAction('list');
                            } else if (data.r === 'exist') {
                                growl.addWarnMessage("内容已存在");
                            }
                        });
                    };
                }
            }],
            link: function postLink(scope, iElement, iAttrs) {
                scope.$watch('action', function () {
                    if (scope.action === 'copy') {
                        scope.initCopyConf();
                    }
                });
            }
        }
    });
    /**
     * ------------ 项目配置 ------------
     * 文件上传
     */
    app.directive('confUpload', function() {
        return {
            restrict: 'E',
            require: '^projectConf',
            templateUrl: 'partials/conf/project/uiview/conf-upload.html',
            controller: ['$scope', '$timeout', '$upload', function($scope, $timeout, $upload) {
                $scope.initUploadConf = function() {
                    $scope.filePath = "";
                    var pid = $scope.project.id;
                    var vid = $scope.versionId;
                    var eid = $scope.env.id;

                    $scope.fileReaderSupported = window.FileReader != null && (window.FileAPI == null || FileAPI.html5 != false);
                    $scope.uploadRightAway = true;
                    $scope.hasUploader = function(index) {
                        return $scope.upload[index] != null;
                    };
                    $scope.abort = function(index) {
                        $scope.upload[index].abort();
                        $scope.upload[index] = null;
                    };
                    $scope.onFileSelect = function($files) {
                        $scope.selectedFiles = [];
                        $scope.progress = [];
                        if ($scope.upload && $scope.upload.length > 0) {
                            for (var i = 0; i < $scope.upload.length; i++) {
                                if ($scope.upload[i] != null) {
                                    $scope.upload[i].abort();
                                }
                            }
                        }
                        $scope.upload = [];
                        $scope.uploadResult = [];
                        $scope.selectedFiles = $files;
                        $scope.dataUrls = [];
                        for ( var i = 0; i < $files.length; i++) {
                            var $file = $files[i];
                            if ($scope.fileReaderSupported && $file.type.indexOf('image') > -1) {
                                var fileReader = new FileReader();
                                fileReader.readAsDataURL($files[i]);
                                var loadFile = function(fileReader, index) {
                                    fileReader.onload = function(e) {
                                        $timeout(function() {
                                            $scope.dataUrls[index] = e.target.result;
                                        });
                                    }
                                }(fileReader, i);
                            }
                            $scope.progress[i] = -1;
                            if ($scope.uploadRightAway) {
                                $scope.start(i);
                            }
                        }
                    };

                    var uploadUrl = '/conf/upload';
                    $scope.start = function(index) {
                        $scope.progress[index] = 0;
                        $scope.errorMsg = null;
                        $scope.upload[index] = $upload.upload({
                            url: uploadUrl,
                            method: 'post',
                            headers: {'my-header': 'my-header-value'},
                            data : {
                                envId: eid,
                                projectId: pid,
                                versionId: vid,
                                path: '/' + ($scope.filePath?$scope.filePath:'') + ($scope.selectedFiles[index].relativePath || '')
                            },
                            file: $scope.selectedFiles[index],
                            fileFormDataName: 'myFile'
                        });
                        $scope.upload[index].then(function(response) {
                            $timeout(function() {
                                $scope.uploadResult.push(response.data);
                            });
                        }, function(response) {
                            if (response.status > 0) $scope.errorMsg = response.status + ': ' + response.data;
                        }, function(evt) {
                            $scope.progress[index] = Math.min(100, parseInt(100.0 * evt.loaded / evt.total));
                        });
                        $scope.upload[index].xhr(function(xhr){
//				xhr.upload.addEventListener('abort', function() {console.log('abort complete')}, false);
                        });

                    };

                    $scope.dragOverClass = function($event) {
                        var items = $event.dataTransfer.items;
                        var hasFile = false;
                        if (items != null) {
                            for (var i = 0 ; i < items.length; i++) {
                                if (items[i].kind == 'file') {
                                    hasFile = true;
                                    break;
                                }
                            }
                        } else {
                            hasFile = true;
                        }
                        return hasFile ? "dragover" : "dragover-err";
                    };
                }

            }],
            link: function postLink(scope, iElement, iAttrs) {
                scope.$watch('action', function () {
                    if (scope.action === 'upload') {
                        scope.initUploadConf();
                    }
                });
            }
        }
    });


});