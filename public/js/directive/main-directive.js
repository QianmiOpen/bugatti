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
            templateUrl: 'partials/task/project-balance.html'
        }
    });

    app.directive('projectItem', function () {
        return {
            restrict: 'E',
            require: '^projectTabs',
            templateUrl: 'partials/task/project-item.html',
            controller: ['$scope', '$filter', 'ProjectService', 'TemplateService',
                function($scope, $filter, ProjectService, TemplateService) {
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
                                angular.forEach(project_vars, function(pv) {
                                    if (findInVars(_vars, pv) === -1) {
                                        _vars.unshift({name: pv.name, value: pv.value, envId: $scope.activeEnv});  // first add
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
                                alert('成功');
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
                $scope.setAction = function(activeTab, conf) {
                    $scope.action = activeTab;
                    $scope.conf = conf;
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

                    ConfService.get($scope.conf.id, function(data) {
                        $scope.conf = data.conf;
                        $scope.confContent = data.confContent;
                        $scope.conf.content = data.confContent.content;
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

                    this.aceLoaded = function(_editor) {
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
                    ConfService.get($scope.conf.id, function(data) {
                        $scope.conf = data.conf;
                        $scope.conf.content = data.confContent.content;
                    });

                    this.update = function() {
                        $scope.conf.updated = $filter('date')(new Date(), "yyyy-MM-dd HH:mm:ss")
                        ConfService.update($scope.conf.id, angular.toJson($scope.conf), function(data) {
                            if (data.r === 'exist') {
                                $scope.form.path.$invalid = true;
                                $scope.form.path.$error.exists = true;
                            } else {
                                $scope.setAction('list');
                            }
                        });
                    };

                    $scope.wordList = [];
                    $scope.completers = ConfService.completer($scope.conf.envId, $scope.conf.projectId, $scope.conf.versionId, function(data) {
                        var obj = eval(data);
                        for (var prop in obj) {
                            $scope.wordList.push({'word': prop, 'score': 0, meta: obj[prop]});
                        }
                    });
                    var langTools = ace.require("ace/ext/language_tools");
                    $scope.aceLoaded = function(_editor) {
                        _editor.setOptions({
                            enableBasicAutocompletion: true
                        });

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
                                $scope.form.path.$invalid = true;
                                $scope.form.path.$error.exists = true;
                            } else {
                                $scope.setAction('list');
                            }
                        });
                    };

                    $scope.wordList = [];
                    $scope.completers = ConfService.completer($scope.conf.envId, $scope.conf.projectId, $scope.conf.versionId, function(data) {
                        var obj = eval(data);
                        for (var prop in obj) {
                            $scope.wordList.push({'word': prop, 'score': 0, meta: obj[prop]});
                        }
                    });

                    var langTools = ace.require("ace/ext/language_tools");
                    $scope.aceLoaded = function(_editor) {
                        _editor.setOptions({
                            enableBasicAutocompletion: true
                        });

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
            controller: ['$scope', '$window', 'VersionService', 'ConfService', function($scope, $window, VersionService, ConfService) {
                $scope.initCopyConf = function() {
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
                                $window.alert('内容已存在')
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