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


});