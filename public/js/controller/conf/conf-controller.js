'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.controller.conf.confModule', ['angularFileUpload']);

    app.controller('ConfCtrl', ['$scope', '$state', '$stateParams', '$modal', 'growl',
        'ConfService', 'EnvService', 'ProjectService', 'VersionService',
        function($scope, $state, $stateParams, $modal, growl, ConfService, EnvService, ProjectService, VersionService) {
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
                    $scope.envs = [];
                } else {
                    $scope.envs = data;
                }
                $scope.envs.unshift({"id": 0, "name": '模板配置', "nfServer": '', "ipRange": '', "level": 'safe', "scriptVersion": '', "jobNo": '', "remark": ''})

                if ($stateParams.eid && $stateParams.eid != 0) {
                    EnvService.get($stateParams.eid, function(e) {
                        $scope.envChange(e)
                    });
                } else { // default select first
                    $scope.envChange($scope.envs[0])
                }
            });

            // 环境选择
            $scope.envChange = function(e) {
                $scope.env = e;
            };

            // 生成模板
            $scope.gen = function() {
                $scope.copyParam = {projectId: $stateParams.id, target_eid: $stateParams.eid, target_vid: $stateParams.vid, envId: 0, versionId: $stateParams.vid, ovr: true, copy: false};
                if (confirm('把当前环境所有配置文件生成模板？')) {
                    ConfService.copy(angular.toJson($scope.copyParam), function(data) {
                        if (data.r === 'ok') {
                            growl.addSuccessMessage("成功");
                        } else if (data.r === 'exist') {
                            growl.addWarnMessage("内容已存在");
                        }
                    });
                }
            }
    }]);

    app.controller('ConfListCtrl', ['$scope', '$state', '$stateParams', '$modal',
        'ConfService',
        function($scope, $state, $stateParams, $modal, ConfService) {
            if ($stateParams.eid) { // project index forward 'eid:null' error
                ConfService.getAll($stateParams.eid, $stateParams.id, $stateParams.vid, function(data) {
                    $scope.confs = data;
                });
            }
    }]);

    app.controller('ConfCreateCtrl', ['$scope', '$filter', '$state', '$stateParams', '$modal', 'ConfService',
        function($scope, $filter, $state, $stateParams, $modal, ConfService) {
            $scope.conf = {envId: $stateParams.eid, projectId: $stateParams.id, versionId: $stateParams.vid};

            $scope.cancel = function() {
                $state.go('conf.project.version.conf.list', {eid: $scope.conf.envId})
            };

            $scope.save = function() {
                $scope.conf.updated = $filter('date')(new Date(), "yyyy-MM-dd HH:mm:ss")
                ConfService.save(angular.toJson($scope.conf), function(data) {
                    if (data.r === 'exist') {
                        $scope.newForm.path.$invalid = true;
                        $scope.newForm.path.$error.exists = true;
                    } else {
                        $state.go('conf.project.version.conf.list', {eid: $scope.conf.envId})
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

    }]);

    app.controller('ConfShowCtrl', ['$scope', '$state', '$stateParams', '$modal',
        'ConfService',
        function($scope, $state, $stateParams, $modal, ConfService) {

            ConfService.get($stateParams.cid, function(data) {
                $scope.conf = data.conf;
                $scope.confContent = data.confContent;
            });

            // remove
            $scope.delete = function(cid) {
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
                    $state.go('conf.project.version.conf.list', {eid: $scope.env.id})
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
    }]);

    app.controller('ConfEditCtrl', ['$scope', '$state', '$filter', '$stateParams', '$modal',
        'ConfService',
        function($scope, $state, $filter, $stateParams, $modal, ConfService) {
            ConfService.get($stateParams.cid, function(data) {
                $scope.conf = data.conf;
                $scope.conf.content = data.confContent.content;
            });

            $scope.update = function() {
                $scope.conf.updated = $filter('date')(new Date(), "yyyy-MM-dd HH:mm:ss")
                ConfService.update($stateParams.cid, angular.toJson($scope.conf), function(data) {
                    if (data.r === 'exist') {
                        $scope.editForm.path.$invalid = true;
                        $scope.editForm.path.$error.exists = true;
                    } else {
                        $state.go('conf.project.version.conf.list', {eid: $scope.env.id})
                    }
                });
            };

            $scope.cancel = function() {
                $state.go('conf.project.version.conf.detail', {cid: $stateParams.cid})
            };

            $scope.wordList = [];
            ConfService.completer($stateParams.eid, $stateParams.id, $stateParams.vid, function(data) {
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

                // Editor part
                var fileName = $scope.conf ? $scope.conf.name : '.properties';
                if (fileName) {
                    var suffix = fileName.substring(fileName.lastIndexOf('.') + 1);
                    if (suffix == 'conf') {
                        suffix = 'properties'
                    }
                    suffix = "ace/mode/" + suffix;
                    _editor.getSession().setMode(suffix);
                }

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
    }]);

    // ----------------------------------------------------
    // 一键拷贝
    // ----------------------------------------------------
    app.controller('ConfCopyCtrl', ['$scope', '$state', 'growl', '$filter', '$stateParams', 'ConfService', 'VersionService',
        function($scope, $state, growl, $filter, $stateParams, ConfService, VersionService) {
            $scope.copyEnvs = angular.copy($scope.envs);
            $scope.copyParam = {projectId: $stateParams.id, target_eid: null, target_vid: null, envId: $stateParams.eid, versionId: $stateParams.vid, ovr: false};

            VersionService.top($stateParams.id, function(data) {
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

            $scope.ok = function (param) {
                ConfService.copy(angular.toJson(param), function(data) {
                    if (data.r === 'ok') {
                        $state.go('conf.project.version.conf.list', {eid: param.envId}, {reload: true})
                    } else if (data.r === 'exist') {
                        growl.addWarnMessage("内容已存在");
                    }
                });
            };
    }]);

    // ------------------------------------------------------
    // 配置文件历史记录
    // ------------------------------------------------------
    app.controller('ConfLogCtrl', ['$scope', '$state', '$stateParams', '$modal',
        'ConfService', 'LogService',
        function($scope, $state, $stateParams, $modal, ConfService, LogService) {

            ConfService.get($stateParams.cid, function(data) {
                $scope.conf = data.conf;
            });

            $scope.currentPage = 1;
            $scope.pageSize = 10;

            // count
            LogService.count($stateParams.cid, function(data) {
                $scope.totalItems = data;
            });

            // list
            LogService.getPage($stateParams.cid, 0, $scope.pageSize, function(data) {
                $scope.logs = data;
            });
    }]);

    app.controller('ConfLogShowCtrl', ['$scope', '$state', '$stateParams', '$modal',
        'ConfService', 'LogService',
        function($scope, $state, $stateParams, $modal, ConfService, LogService) {

            LogService.get($stateParams.lid, function(data) {
                $scope.log = data.log;
                $scope.log.content = data.logContent;
            });

            ConfService.get($stateParams.cid, function(data) {
                $scope.conf = data.conf;
                $scope.conf.content = data.confContent;
            });

        }]);

    app.controller('ConfUploadCtrl', ['$scope', '$state', '$stateParams', '$timeout', '$http', '$upload',
        function($scope, $state, $stateParams, $timeout, $http, $upload) {
            $scope.filePath = "";
            var pid = $stateParams.id;
            var vid = $stateParams.vid;
            var eid = $stateParams.eid;

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


    }]);

});