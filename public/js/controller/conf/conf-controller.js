'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.controller.conf.confModule', []);

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
                $state.go('conf.project.version.conf.list', {eid: e.id})
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
                    var thisEid = param.eid;
                    ConfService.copy(angular.toJson(param), function(data) {
                        if (data.r === 'ok') {
                            $state.go('conf.project.version.conf', {eid: thisEid}, {reload: true})
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
            $modalInstance.close({target_eid: selEnv, target_vid: selVer, eid: curr_eid, vid: curr_vid, ovr: selOver});
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
            $scope.conf = {pid: $stateParams.id, vid: $stateParams.vid};

            $scope.cancel = function() {

                $state.go('conf.project.version.conf.list', {eid: $scope.env.id})

//                $scope.$on('$locationChangeStart', function( event ) {
//                    var answer = confirm("Are you sure you want to leave this page?")
//                    if (!answer) {
//                        event.preventDefault();
//                    }
//                });
            };

            $scope.save = function() {
                $scope.conf.eid = $scope.env.id;
                $scope.conf.updated = $filter('date')(new Date(), "yyyy-MM-dd hh:mm:ss")
                ConfService.save(angular.toJson($scope.conf), function(data) {
                    $state.go('conf.project.version.conf.list', {eid: $scope.env.id})
                });
            }
    }]);


    app.controller('ConfShowCtrl', ['$scope', '$state', '$stateParams', '$modal',
        'ConfService',
        function($scope, $state, $stateParams, $modal, ConfService) {

            ConfService.get($stateParams.cid, function(data) {
                $scope.conf = data.conf;
                $scope.conf.content = data.content.content;
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
                $scope.conf.content = data.content.content;
            });

            $scope.update = function() {
                $scope.conf.updated = $filter('date')(new Date(), "yyyy-MM-dd hh:mm:ss")
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
//                console.log('conf='+angular.toJson(data))
            });


        }]);

});