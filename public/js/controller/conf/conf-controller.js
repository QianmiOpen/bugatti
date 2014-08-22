'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.controller.conf.confModule', ['angularFileUpload']);

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

                if ($stateParams.eid) {
                    EnvService.get($stateParams.eid, function(e) {
                        $scope.envChange(e)
                    });
                } else { // default select first
                    $scope.envChange(data[0])
                }
            });

            // 环境选择
            $scope.envChange = function(e) {
                $scope.env = e;
            };

            // ----------------------------------------------------
            // 一键拷贝
            // ----------------------------------------------------

            VersionService.top($stateParams.id, function(data) {
                $scope.versions = data;
            });

            $scope.openModal = function(curr_eid, curr_vid) {
                var modalInstance = $modal.open({
                    templateUrl: 'modalCopy.html',
                    windowClass: 'modal-copy',
                    controller: ModalInstanceCtrl,
                    resolve: {
                        envs: function () {
                            return $scope.envs;
                        },
                        versions : function() {
                            return $scope.versions;
                        },
                        curr_eid: function () {
                            return curr_eid;
                        },
                        curr_vid: function() {
                            return curr_vid;
                        }
                    }
                });

                modalInstance.result.then(function (param) {
                    param.projectId = $stateParams.id;
                    var thisEid = param.envId;
                    ConfService.copy(angular.toJson(param), function(data) {
                        if (data.r === 'ok') {
                            $state.go('conf.project.version.conf.list', {eid: thisEid}, {reload: true})
                        }
                    });
                }, function () {
                    console.info('Modal dismissed at: ' + new Date());
                });

            }
    }]);

    // 一键拷贝弹出框
    var ModalInstanceCtrl = function ($scope, $modalInstance, envs, versions, curr_eid, curr_vid) {

        $scope.envs = envs;
        if ($scope.envs.length) $scope.env = $scope.envs[0].id;

        $scope.versions = versions;
        if ($scope.versions.length) $scope.version = $scope.versions[0].id;

        $scope.override = false;

        $scope.ok = function (selEnv, selVer, selOver) {
            $modalInstance.close({target_eid: selEnv, target_vid: selVer, envId: curr_eid, versionId: curr_vid, ovr: selOver});
        };

        $scope.cancel = function () {
            $modalInstance.dismiss('cancel');
        };
    };

    app.controller('ConfListCtrl', ['$scope', '$state', '$stateParams', '$modal',
        'ConfService',
        function($scope, $state, $stateParams, $modal, ConfService) {
            if ($stateParams.eid) { // project index forward 'eid:null' error
                ConfService.getAll($stateParams.eid, $stateParams.vid, function(data) {
                    $scope.confs = data;
                });
            }
    }]);

    app.controller('ConfCreateCtrl', ['$scope', '$filter', '$state', '$stateParams', '$modal',
        'ConfService', 'EnvService', 'ProjectService', 'VersionService',
        function($scope, $filter, $state, $stateParams, $modal, ConfService, EnvService, ProjectService, VersionService) {
            $scope.conf = {projectId: $stateParams.id, versionId: $stateParams.vid};

            $scope.cancel = function() {
                $state.go('conf.project.version.conf.list', {eid: $scope.env.id})
            };

            $scope.save = function() {
                $scope.conf.envId = $scope.env.id;
                $scope.conf.updated = $filter('date')(new Date(), "yyyy-MM-dd HH:mm:ss")
                ConfService.save(angular.toJson($scope.conf), function(data) {
                    $state.go('conf.project.version.conf.list', {eid: $scope.env.id})
                });
            };

            $scope.completers = ConfService.completer($scope.env.id, $stateParams.id, $stateParams.vid, function(data) {
                console.log(data.r);
            });

            var langTools = ace.require("ace/ext/language_tools");
            $scope.aceLoaded = function(_editor) {
                _editor.setOptions({
                    enableBasicAutocompletion: true
                });

                _editor.commands.bindKey("f1|command-enter", "startAutocomplete");
                _editor.commands.bindKey("Ctrl-Space|Ctrl-Shift-Space|Alt-Space", null); // do nothing on ctrl-space
//                _editor.commands.on("afterExec", function(e){
//                    console.log('e=', e);
//                    if (e.command.name == "insertstring"&&/^({{)$/.test(e.args)) {
//                        editor.execCommand("startAutocomplete")
//                    }
//                })

                _editor.on("mousedown", function(e) {
                    // Store the Row/column values
                    console.log(e)
                });

                _editor.getSession().on('changeCursor', function(e) {
                    if (editor.$mouseHandler.isMousePressed) {
                        console.log('cursor=', e)
                    }
                    // remove last stored values
                    // Store the Row/column values
                });

                var codeCompleter = {
                    getCompletions: function(editor, session, pos, prefix, callback) {
                        if (prefix.length === 0) { callback(null, []); return }

                        var wordList = [
                            {"word": "d",  "score":0,  meta: 'object'},
                            {"word": "d.cardbase", "score":0, meta: 'object'},
                            {"word": "d.cardbase.name",  "score":0,  meta: 'cardbase'},
                            {"word": "d.cardbase.hosts[0].attrs.aa", "score":0, meta: '66-1'},
                            {"word": "c", "score":0, meta: 'object'},
                            {"word": "c.name", "score":0, meta: 'lin-66-1'}
                        ];

                        callback(null, wordList.map(function(ea) {
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
                $scope.conf.content = data.confContent.content;
            });

            // remove
            $scope.delete = function(cid) {
                var modalInstance = $modal.open({
                    templateUrl: 'partials/modal.html',
                    controller: function ($scope, $modalInstance) {
                        $scope.ok = function () {
                            ConfService.remove(cid, function(state) {
                                $modalInstance.close(state);
                            });
                        };
                        $scope.cancel = function () {
                            $modalInstance.dismiss('cancel');
                        };
                    }
                });
                modalInstance.result.then(function(state) {
                    if (state >= 0) {
                        $state.go('conf.project.version.conf.list', {eid: $scope.env.id})
                    } else {
                        alert('删除失败！')
                    }
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
                    $state.go('conf.project.version.conf.list', {eid: $scope.env.id})
                });
            };

            $scope.cancel = function() {
                $state.go('conf.project.version.conf.detail', {cid: $stateParams.cid})
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


    }]);

});