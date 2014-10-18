/*global define */

'use strict';

define(['angular'], function(angular) {

    /* Directives */

    var app = angular.module('bugattiApp.directives', []);

    /* 返回上一页(浏览器历史) */
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

    /* 用户权限列表展示 */
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

    /* task */
    app.directive('projectTabs', function () {
        return {
            restrict: 'E',
            templateUrl: 'partials/task/project-tabs.html',
            controller: function($scope) {
                $scope.tab = 1;
                $scope.isSet = function(checkTab) {
                    return $scope.tab === checkTab;
                };
                $scope.setTab = function(activeTab) {
                    $scope.tab = activeTab;
                };
            }
        };
    });

    app.directive('projectBalance', function () {
        return {
            restrict: 'E',
            templateUrl: 'partials/task/project-balance.html',
            controller: ['$scope',
                function($scope){
                    $scope.ctab = 1 ;
                    $scope.c_index = -1;
                    $scope.setCTab =function(ctab){
                        $scope.ctab = ctab ;
                    }
                    $scope.setCIndex =function(cIndex){
                        $scope.c_index = cIndex ;
                    }
                    $scope.showQueues = function(index, ctab, taskId){
                        var clusterFlag = true;
                        if($scope.isQueueShow[index] && $scope.ctab == ctab){
                            clusterFlag = false;
                        }
                        //隐藏其他的index
                        $scope.isQueueShow = $scope.isQueueShow.map(function(q){
                            return false ;
                        })
                        if(clusterFlag){
                            $scope.ctabFlag = !$scope.ctabFlag;
                            $scope.isQueueShow[index] = !$scope.isQueueShow[index];
                        }
                        $scope.taskId = taskId
                        $scope.setCTab(ctab);
                        $scope.setCIndex(index);
                    }
                }
            ]
        }
    });

    app.directive('projectItem', function () {
        return {
            restrict: 'E',
            require: '^projectTabs',
            templateUrl: 'partials/task/project-item.html',
            controller: ['$scope', '$filter', 'growl', 'ProjectService', 'TemplateService',
                function($scope, $filter, growl, ProjectService, TemplateService) {
                // project variable
                $scope.vars = [];
                $scope.addVar = function(v) {
                    $scope.varForm.varName.$error.unique = false;
                    $scope.varForm.varName.$error.required = false;
                    $scope.varForm.varValue.$error.required = false;

                    if (angular.isUndefined($scope.activeEnv )) {
                        return;
                    }
                    v.envId = $scope.activeEnv;   // bind env

                    if (findInVars($scope.vars, v) != -1) {
                        $scope.varForm.varName.$invalid = true;
                        $scope.varForm.varName.$error.unique = true;
                        return;
                    };
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
                };

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
                    TemplateService.itemAttrs($scope.pro.templateId, $scope.scriptVersion, function(data) {
                        $scope.items = data;
                        ProjectService.atts($scope.pro.id, function(project_attrs) {
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
                    TemplateService.itemVars($scope.pro.templateId, $scope.scriptVersion, function(item_vars) {
                        var _vars = angular.copy($scope.vars);
                        angular.forEach(_vars, function(v, index) {
                            if (v.name.indexOf('t_') === 0) {
                                delete _vars[index]; // delete object is null
                            }
                        });
                        _vars = _vars.filter(function(e){return e}); // clear null

                        // load init variable
                        ProjectService.vars($scope.pro.id, $scope.activeEnv, function(project_vars) {
                            if (project_vars.length < 1) {
                                angular.forEach(item_vars, function(iv) {
                                    _vars.push({name: iv.itemName, value: '', envId: $scope.activeEnv});  // first add
                                });
                            }
                            else {
                                angular.forEach(item_vars, function(iv) {
                                    var replaceFlag = false;
                                    project_vars.map(function(pv){
                                        if(pv.name == iv.itemName && pv.envId == $scope.activeEnv){
                                            replaceFlag = true;
                                            _vars.unshift({name: pv.name, value: pv.value, envId: $scope.activeEnv});
                                        }
                                    })
                                    if(!replaceFlag){
                                        _vars.push({name: iv.itemName, value: '', envId: $scope.activeEnv});
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
                        ProjectService.update($scope.pro.id, $scope.activeEnv, angular.toJson(project), function(data) {
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

    app.directive('projectConf', function () {
        return {
            restrict: 'E',
            require: '^projectTabs',
            templateUrl: 'partials/task/project-conf.html',
            controller: ['$scope', '$filter', 'ConfService', 'VersionService', function($scope, $filter, ConfService, VersionService) {
                $scope.initVersions = function() {
                    VersionService.top($scope.pro.id, function(data) {
                        $scope.versions = data;
                    });
                };

                $scope.changeVersion = function(vid) {
                    $scope.setAction('list');
                    if (vid == null) {
                        $scope.confs = [];
                    } else {
                        ConfService.getAll($scope.activeEnv, vid, function(data) {
                            $scope.confs = data;
                        });
                    }
                };

                $scope.action = 'list';
                $scope.isAction = function(checkTab) {
                    return $scope.action === checkTab;
                };
                $scope.setAction = function(activeTab, conf_id) {
                    $scope.action = activeTab;
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

    app.directive('projectDependency', function(){
        return {
            restrict: 'E',
            require: '^projectTabs',
            templateUrl: 'partials/task/project-dependency.html',
            controller: ['$scope', '$stateParams', '$filter', '$state', 'DependencyService', 'ProjectService', 'growl',
                function($scope, $stateParams, $filter, $state, DependencyService, ProjectService, growl){
                    $scope.showDependencies = function(){
                        DependencyService.get($scope.pro.id, function(data){
                            $scope.groups = data
                        })
                    }
                    $scope.delayLoadDependency = function(){
                        ProjectService.getExceptSelf($scope.pro.id, function(data){
                            $scope.projects = data ;
                            $scope.showDependencies() ;
                        })
                    }

                    $scope.removeDependency = function(parent,child){
                        DependencyService.removeDependency(parent.id, child.id, function(data){
                            $scope.showDependencies()
                        })
                    }

                    $scope.addDependency = function(parent,child){
                        DependencyService.addDependency(parent, child, function(data){
                            if(data.r == 0){
                                growl.addWarnMessage("添加失败");
                            }
                            $scope.showDependencies()
                        })
                    }

                    $scope.templateFilter = function(dep){
                        return function(p){return p.templateId == dep.templateId};
                    }

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
                    }

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

    app.directive('projectMember', function(){
        return {
            restrict: 'E',
            require: '^projectTabs',
            templateUrl: 'partials/task/project-member.html',
            controller: ['$scope', '$stateParams', '$modal', 'ProjectService', 'EnvService',
                function($scope, $stateParams, $modal, ProjectService, EnvService) {
                // ---------------------------------------------
                // 项目成员管理
                // ---------------------------------------------
                $scope.delayLoadMember = function(){
                    ProjectService.members($scope.pro.id, function(data) {
                        $scope.members = data;
                    });
                }

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

                    ProjectService.saveMember($scope.pro.id, jobNo, function(data) {
                        if (data.r === 'none') {
                            $scope.jobNo$error = '用户不存在';
                        }
                        else if (data.r === 'exist') {
                            $scope.jobNo$error = '已存在用户';
                        } else if (data > 0) {
                            ProjectService.members($scope.pro.id, function(data) {
                                $scope.members = data;
                                $scope.jobNo$error = '';
                            });
                        }
                    });
                }

                $scope.memberUp = function(mid, msg) {
                    if (confirm(msg)) {
                        ProjectService.updateMember(mid, "up", function(data) {
                            ProjectService.members($scope.pro.id, function(data) {
                                $scope.members = data;
                            });
                        });
                    }
                };
                $scope.memberDown = function(mid, msg) {
                    if (confirm(msg)) {
                        ProjectService.updateMember(mid, "down", function(data) {
                            ProjectService.members($scope.pro.id, function(data) {
                                $scope.members = data;
                            });
                        });
                    }
                };
                $scope.memberRemove = function(mid, msg) {
                    if (confirm(msg)) {
                        ProjectService.updateMember(mid, "remove", function(data) {
                            ProjectService.members($scope.pro.id, function(data) {
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

    app.directive('clusterTabs', function(){
        return {
            restrict: 'E',
            templateUrl: 'partials/task/cluster-tabs.html',
            controller: function($scope){
                $scope.isClusterShow = function(ctab, c_index){
                    return ($scope.ctab == ctab && $scope.c_index == c_index);
                }
            }
        }
    });

    app.directive('hisTabs', function(){
        return {
            restrict: 'E',
            templateUrl: 'partials/task/his-tabs.html',
            controller: function($scope){
                $scope.isHisShow = function(stab, s_index){
                    return ($scope.stab == stab && $scope.s_index == s_index);
                }
            }
        }
    });

    app.directive('taskQueue', function(){
        return {
            restrict: 'E',
            require: '^clusterTabs',
            templateUrl: 'partials/task/task-queue.html',
            controller: ['$scope', 'TaskService',
            function($scope, TaskService){
                $scope.removeQueue = function(qid){
                    TaskService.removeTaskQueue(qid, function(data){
                        //如果删除的任务在一瞬间刚好变为正在执行，应告知
                    })
                }
            }]
        }
    });

    app.directive('clusterProperties', function(){
       return {
           restrict: 'E',
           require: '^clusterTabs',
           templateUrl: 'partials/task/cluster-properties.html',
           controller: ['$scope', '$stateParams', '$state', '$modal', 'RelationService', 'ProjectService', 'EnvService', 'growl',
           function($scope, $stateParams, $state, $modal, RelationService, ProjectService, EnvService, growl) {
               $scope.delayLoadProperties = function(){
                   RelationService.get($scope.c.id, function(data) {
                       $scope.relation = data;
                       if ($scope.relation) {
                           if (angular.isUndefined($scope.pro.id) || angular.isUndefined($scope.activeEnv) ) {
                               return;
                           }
                           ProjectService.vars($scope.pro.id, $scope.activeEnv, function(project_vars) {
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
               }

               function findInVars(vars, v) {
                   var find = '';
                   angular.forEach(vars, function(_v, index) {
                       if (_v.name == v.name) {
                           find = _v.value;
                           return;
                       }
                   });
                   return find;
               };

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
           }],
           link: function postLink(scope, iElement, iAttrs) {
               scope.$watch('c_index', function () {
                   if (scope.ctab == 1 && scope.c_index == scope.$index) {
                       scope.delayLoadProperties();
                   }
               });
           }
       }
    });

    app.directive('taskLog', function(){
        return {
            restrict: 'E',
            require: '^projectTabs',
            templateUrl: 'partials/task/task-log.html',
            controller:['$scope', 'TaskService','$state','$stateParams',
                function($scope,TaskService,$state,$stateParams){
                $scope.delayLoadLog = function(){
                    if($scope.taskId != undefined){
                        var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
                        var path = PlayRoutes.controllers.task.TaskController.taskLog($scope.taskId).webSocketURL()
                        $scope.logSocket = new WS(path)
                        $scope.logSocket.onmessage = $scope.receiveEvent

                        $scope.logHeader = ""
                    }
                }

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
                            if(data.taskId == $scope.taskId){
                                if(data.kind == "logFirst"){
                                    $scope.logFirstHidden = false
                                    $scope.logFirst = data.message
                                }else if(data.kind == "logHeader"){
                                    $scope.logFirstHidden = true
                                    if($scope.logHeader.length == 0){
                                        $scope.logHeader = data.message
                                        $scope.message = $scope.logHeader + $scope.message
                                    }
                                }else{
                                    $scope.message = data.message
                                }
                            }
                        });
                    }
                }

                $scope.closeWs = function(){
                    $scope.logSocket.close()
                }

                $scope.logFirstHidden = false

                $scope.showHiddenMessage = function(){
                    var len = parseInt($scope.logFirst.split(" ")[0])
                    TaskService.readHeader($scope.taskId, len, function(){})
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
            }],
            link: function postLink(scope, iElement, iAttrs) {
                scope.$watch('c_index', function () {
                    if (scope.ctab == 3 && scope.c_index == scope.$index) {
                        scope.delayLoadLog();
                    }
                });
                scope.$watch('ctab', function () {
                    if (scope.ctab == 3 && scope.c_index == scope.$index) {
                        scope.delayLoadLog();
                    }
                });
                scope.$watch('ctabFlag', function () {
                    if (scope.ctab == 3 && scope.c_index == scope.$index) {
                        scope.delayLoadLog();
                    }
                });
                //历史任务查看
                scope.$watch('s_index', function () {
                    if (scope.stab == 1 && scope.s_index == scope.$index) {
                        scope.delayLoadLog();
                    }
                });
                scope.$watch('stabFlag', function () {
                    if (scope.stab == 1 && scope.s_index == scope.$index) {
                        scope.delayLoadLog();
                    }
                });
            }
        }
    });

    app.directive('projectHistory', function(){
        return{
            restrict: 'E',
            require: '^projectTabs',
            templateUrl: 'partials/task/task-history.html',
            controller: ['$scope', 'TaskService',
                function($scope, TaskService){
                    $scope.hisTasks = []
                    $scope.delayLoadHistory = function(){
                        TaskService.findHisTasks($scope.activeEnv, $scope.pro.id, function(data){
                            $scope.hisTasks = data.map($scope.addStatusTipHistory).map($scope.addShowFlag)
                        });
                    }

                    $scope.addShowFlag = function(data){
                        if(!$scope.isObjEmpty(data)){
                            $scope.isHisLogShow.push(false)
                        }
                        return data ;
                    }

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
                    }

                    $scope.stab = 1 ;
                    $scope.s_index = -1;
                    $scope.setSTab =function(stab){
                        $scope.stab = stab ;
                    }
                    $scope.setSIndex =function(sIndex){
                        $scope.s_index = sIndex ;
                    }
                    $scope.showHisLog = function(index, stab, taskId){
                        var clusterFlag2 = true;
                        if($scope.isHisLogShow[index] && $scope.stab == stab){
                            clusterFlag2 = false;
                        }
                        //隐藏其他的index
                        $scope.isHisLogShow = $scope.isHisLogShow.map(function(q){
                            return false ;
                        })
                        if(clusterFlag2){
                            $scope.stabFlag = !$scope.stabFlag;
                            $scope.isHisLogShow[index] = !$scope.isHisLogShow[index];
                        }
                        $scope.taskId = taskId
                        $scope.setSTab(stab);
                        $scope.setSIndex(index);
                    }
                }
            ],
            link: function postLink(scope, iElement, iAttrs){
                scope.$watch('tab', function () {
                    if (scope.tab == 6) {
                        scope.delayLoadHistory();
                    }
                });
            }
        }
    });

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
                    if (scope.action === 'edit') {
                        scope.initEditConf();
                    }
                });
            }
        }
    });


    app.directive('confCreate', function() {
        return {
            restrict: 'E',
            require: '^projectConf',
            templateUrl: 'partials/conf/project/uiview/conf-new.html',
            controller: ['$scope', '$filter', 'ConfService', function($scope, $filter, ConfService) {
                $scope.initCreate = function() {
                    $scope.conf = {envId: $scope.activeEnv, projectId: $scope.pro.id, versionId: $scope.versionId};

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

    app.directive('confCopy', function() {
        return {
            restrict: 'E',
            require: '^projectConf',
            templateUrl: 'partials/conf/project/uiview/conf-copy.html',
            controller: ['$scope', 'growl', 'VersionService', 'ConfService', function($scope, growl, VersionService, ConfService) {
                $scope.initCopyConf = function() {
                    $scope.copyEnvs = angular.copy($scope.envs);
                    $scope.copyEnvs.unshift({"id": 0, "name": '模板配置', "nfServer": '', "ipRange": '', "level": 'safe', "scriptVersion": '', "jobNo": '', "remark": ''})

                    $scope.copyParam = {projectId: $scope.pro.id, target_eid: null, target_vid: null, envId: $scope.activeEnv, versionId: $scope.versionId, ovr: false};

                    VersionService.top($scope.pro.id, function(data) {
                        $scope.versions = data;

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

    app.directive('confUpload', function() {
        return {
            restrict: 'E',
            require: '^projectConf',
            templateUrl: 'partials/conf/project/uiview/conf-upload.html',
            controller: ['$scope', '$timeout', '$upload', function($scope, $timeout, $upload) {
                $scope.initUploadConf = function() {
                    var pid = $scope.pro.id;
                    var vid = $scope.versionId;
                    var eid = $scope.activeEnv;

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
                                path: $scope.filePath
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