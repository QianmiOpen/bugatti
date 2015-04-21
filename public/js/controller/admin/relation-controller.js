'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.controller.admin.relationModule', []);

    app.controller('RelationCtrl', ['$scope', '$modal', 'growl', 'RelationService', 'ProjectService', 'EnvService',
        function($scope, $modal, growl, RelationService, ProjectService, EnvService) {
            $scope.app.breadcrumb='关系设置';
            $scope.currentPage = 1;
            $scope.pageSize = 10;

            // init
            EnvService.getAll(function(data) {
                $scope.envs = data;
            });

            ProjectService.getAll(function(data) {
                $scope.projects = data;
            });

            $scope.env = $scope.project = {};

            $scope.searchForm = function() {
                // count
                RelationService.count($scope.s_ip, $scope.s_env, $scope.s_project, function(data) {
                    $scope.totalItems = data;
                });

                RelationService.getPage($scope.s_ip, $scope.s_env, $scope.s_project, 0, $scope.pageSize, function(data) {
                    $scope.relations = data;
                });
            };

            $scope.searchForm();

            $scope.envSelect = function(e) {
                e = e == null ? undefined : e;
                $scope.s_env = e;
            };
            $scope.proSelect = function(p) {
                p = p == null ? undefined : p;
                $scope.s_project = p;
            };

            $scope.setPage = function (pageNo) {
                $scope.currentPage = pageNo;
                $scope.ids = {};
                $scope.master = false;

                if (!angular.isDefined($scope._sort)) {
                    RelationService.getPage($scope.s_ip, $scope.s_env, $scope.s_project, pageNo - 1, $scope.pageSize, function(data) {
                        $scope.relations = data;
                    });

                } else {
                    $scope.sort($scope._sort, pageNo - 1)
                }
            };

            // 字段排序
            $scope.orderBy = false;
            $scope.sort = function(col, pageNo) {
                var sort = $scope._sort = col;
                var direction = $scope.orderBy ? 'asc' : 'desc';
                RelationService.getPageSort($scope.s_ip, $scope.s_env, $scope.s_project, sort, direction, pageNo, $scope.pageSize, function(data) {
                    $scope.relations = data;
                });
            };

            // unbind
            $scope.unbind = function(id, index) {
                var modalInstance = $modal.open({
                    templateUrl: 'partials/modal.html',
                    controller: function ($scope, $modalInstance) {
                        $scope.ok = function () {
                            RelationService.unbind(id, function(data) {
                                $modalInstance.close(data);
                            });
                        };
                        $scope.cancel = function () {
                            $modalInstance.dismiss('cancel');
                        };
                    }
                });
                modalInstance.result.then(function(data) {
                    $scope.setPage($scope.currentPage);
                    RelationService.count($scope.s_ip, $scope.s_env, $scope.s_project, function(num) {
                        $scope.totalItems = num;
                    });
                });
            };

            // remove
            $scope.delete = function(id, index) {
                var modalInstance = $modal.open({
                    templateUrl: 'partials/modal.html',
                    controller: function ($scope, $modalInstance) {
                        $scope.ok = function () {
                            RelationService.remove(id, function(data) {
                                $modalInstance.close(data);
                            });
                        };
                        $scope.cancel = function () {
                            $modalInstance.dismiss('cancel');
                        };
                    }
                });
                modalInstance.result.then(function(data) {
                    if (data == 0) {
                        growl.addWarnMessage('删除失败');
                    } else {
                        $scope.relations.splice(index, 1);
                        $scope.searchForm();
                        growl.addSuccessMessage("删除成功");
                    }
                });
            };
            // checked all
            $scope.$watch('master', function(checked) {
                if (!checked) { $scope.ids = {}; return }
                angular.forEach($scope.relations, function(rel) {
                    $scope.ids[rel.id] = true;
                });
            });
            $scope.isEmpty = function (obj) {
                return angular.equals({}, obj);
            };
            $scope.checked = function(id, value) {
                if (!value) delete $scope.ids[id];
            };
            $scope.deleteBatch = function() {
                var modalInstance = $modal.open({
                    templateUrl: 'partials/modal.html',
                    controller: function ($scope, $modalInstance) {
                        $scope.ok = function () {
                            $modalInstance.close();
                        };
                        $scope.cancel = function () {
                            $modalInstance.dismiss('cancel');
                        };
                    }
                });
                modalInstance.result.then(function() {
                    angular.forEach($scope.ids, function(value, key) {
                        angular.forEach($scope.relations, function(user, i) {
                            if (user.id == key) {
                                $scope.relations.splice(i, 1);
                            }
                        });
                        RelationService.remove(key, function(data) {});
                    });

                    // refresh
                    $scope.master = null;
                    $scope.searchForm();
                    growl.addSuccessMessage("批量删除成功");
                });
            };

        }]);


    app.controller('RelationCreateCtrl', ['$scope', '$state', '$modal', 'RelationService', 'ProjectService', 'EnvService',
        function($scope, $state, $modal, RelationService, ProjectService, EnvService) {
            // init
            EnvService.getAll(function(data) {
                $scope.envs = data;
            });

            ProjectService.getAll(function(data) {
                $scope.projects = data;
            });

            $scope.selectEnv = function() {
                var env = $scope.env || {};
                if (env.id != undefined) {
                    $scope.load = true;
                    RelationService.ips(env.id, function(data) {
                        $scope.ips = data;
                        $scope.load = false;
                    })
                }
            };

            $scope.refresh = function() {
                var valid = false;
                if ($scope.project == undefined || $scope.project.id == undefined) {
                    $scope.form.projectId.$dirty = true;
                    $scope.form.projectId.$invalid = true;
                    $scope.form.projectId.$error.required = true;
                    valid = true;
                }
                if ($scope.env == undefined || $scope.env.id == undefined) {
                    $scope.form.envId.$dirty = true;
                    $scope.form.envId.$invalid = true;
                    $scope.form.envId.$error.required = true;
                    valid = true;
                }
                if (!valid) $scope.selectEnv();
            };

            // insert
            $scope.save = function() {
                var relation = { ids: []};

                angular.forEach($scope.ck_ips, function(value, key) {
                    if (typeof value === 'boolean' && value === true) {
                        relation.ids.push(key);
                    }
                });
                $scope.ips.$error = false;
                if (relation.ids.length < 1) {
                    $scope.ips.$error = true;
                    return;
                }

                relation.envId = $scope.env.id;
                relation.projectId = $scope.project.id;

                RelationService.bind(angular.toJson(relation), function(data) {
                    $state.go("^");
                });
            };
    }]);

    app.controller('RelationAddCtrl', ['$scope', '$state', '$modal', 'growl', 'RelationService', 'ProjectService', 'EnvService', 'AreaService', 'SpiritService',
        function($scope, $state, $modal, growl, RelationService, ProjectService, EnvService, AreaService, SpiritService) {

            $scope.ip = {a: '', b: '', c: '', d: '', e: ''};

            $scope.rel = {envId: '', projectId: '', areaId: '', syndicName: '', spiritId: '',
                name: '', ip: '', state: 'no salt key', containerType: 'vm', hostIp: '', hostName: ''};

            EnvService.getAll(function(data) {
                $scope.envs = data;
            });

            ProjectService.getAll(function(data) {
                $scope.projects = data;
            });

            AreaService.getAll(function(data) {
                $scope.areas = data;
            });

            SpiritService.getAll(function(data) {
                $scope.spirits = data;
            });

            $scope.states = [
                {id: 'no salt key', name: 'no salt key'},
                {id: "can't ping and test ping", name: "can't ping and test ping"},
                {id: 'only can ping', name: "only can ping"},
                {id: 'can ping and test ping', name: 'can ping and test ping'}
            ];
            $scope.containerTypes = [
                {id: 'vm', name: 'vm'},
                {id: 'docker', name: 'docker'}
            ];

            $scope.saveBatch = function() {
                $scope.rel.ip = $scope.ip;
                RelationService.saveBatch($scope.rel, function(data) {
                    $scope.results = [];
                    angular.forEach(data, function(r) {
                        if (r.result < 1) {
                            $scope.results.push(r);
                        }
                    });
                    if ($scope.results.length > 0) {
                        growl.addWarnMessage('增加失败');
                    } else {
                        growl.addSuccessMessage("增加成功");
                        $state.go('admin.relation');
                    }
                });
            };

            $scope.save = function() {
                RelationService.save($scope.rel, function(data) {
                    if (data > 0) {
                        growl.addSuccessMessage("增加成功");
                    } else {
                        growl.addWarnMessage('增加失败');
                    }
                });
            }

        }]);

    app.controller('RelationShowCtrl', ['$scope', '$stateParams', '$state', '$modal', 'RelationService', 'ProjectService', 'EnvService',
        function($scope, $stateParams, $state, $modal, RelationService, ProjectService, EnvService) {
            RelationService.get($stateParams.id, function(data) {
                $scope.relation = data;
                if ($scope.relation) {
                    if (angular.isUndefined($scope.relation.projectId) || angular.isUndefined($scope.relation.envId) ) {
                        return;
                    }
                    ProjectService.vars($scope.relation.projectId, $scope.relation.envId, function(project_vars) {
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

            $scope.saveOrUpdate = function(vars) {
                $scope.relation.globalVariable = [];
                angular.forEach(vars, function(v) {
                    $scope.relation.globalVariable.push({name: v.name, value: v.value})
                });

                RelationService.update($stateParams.id, $scope.relation, function(data) {
                    $state.go("admin.relation");
                });
            };


        }]);

});