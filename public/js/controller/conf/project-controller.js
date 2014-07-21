'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.controller.conf.projectModule', []);

    app.controller('ProjectCtrl', ['$scope', '$state', '$stateParams', '$modal', 'ProjectService', 'VersionService', 'EnvService',
        function($scope, $state, $stateParams, $modal, ProjectService, VersionService, EnvService) {
        $scope.currentPage = 1;
        $scope.pageSize = 30;
        $scope.my = false;
        if($state.current.name === 'conf.project.my') {
            $scope.my = true;
        }

        // load env
        EnvService.getAll(function(data) {
            if (data == null || data.length == 0) {
                return;
            }
            $scope.envId = data[0].id;
        });

        // count
        ProjectService.count($scope.my, function(data) {
            $scope.totalItems = data;
        });

        // list
        ProjectService.getPage($scope.my, 0, $scope.pageSize, function(data) {
            $scope.projects = data;
        });

        // page
        $scope.setPage = function (pageNo) {
            ProjectService.getPage($scope.my, pageNo - 1, $scope.pageSize, function(data) {
                $scope.projects = data;
            });
        };

        $scope.vs_all = function(pid) {
            $scope.versions = [];
            VersionService.top(pid, function(data) {
                $scope.versions = data;
            })
        };


        // remove
        $scope.delete = function(id, index) {
            var modalInstance = $modal.open({
                templateUrl: 'partials/modal.html',
                controller: function ($scope, $modalInstance) {
                    $scope.ok = function () {
                        ProjectService.remove(id, function(state) {
                            $modalInstance.close(state);
                        });
                    };
                    $scope.cancel = function () {
                        $modalInstance.dismiss('cancel');
                    };
                }
            });
            modalInstance.result.then(function(data) {
                if (data.r >= 0) {
                    $scope.projects.splice(index, 1);
                    ProjectService.count($scope.my, function(num) {
                        $scope.totalItems = num;
                    });
                } else if (data.r == 'exist') {
                    alert('还有版本存在该项目，请删除后再操作。。。')
                } else if (data.r == 'none') {
                    alert('不存在的项目？')
                }
            });
        };



    }]);

    app.controller('ProjectShowCtrl', ['$scope', '$stateParams', '$modal', 'ProjectService',
        function($scope, $stateParams, $modal, ProjectService) {
            ProjectService.get($stateParams.id, function(data) {
                $scope.project = data;
            });

            ProjectService.atts($stateParams.id, function(data) {
                $scope.atts = data;
            });

            // ---------------------------------------------
            // 项目成员管理
            // ---------------------------------------------
            ProjectService.members($stateParams.id, function(data) {
                $scope.members = data;
            });

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

                ProjectService.saveMember($stateParams.id, jobNo, function(data) {
                    if (data.r === 'none') {
                        $scope.jobNo$error = '不存在的用户，请在用户管理添加';
                    } else if (data.r > 0) {
                        ProjectService.members($stateParams.id, function(data) {
                            $scope.members = data;
                            $scope.jobNo$error = '';
                        });
                    } else {
                        $scope.jobNo$error = '添加错误';
                    }
                });
            }

            $scope.memberUp = function(mid, msg) {
                if (confirm(msg)) {
                    ProjectService.updateMember(mid, "up", function(data) {
                        if (data.r > 0) {
                            ProjectService.members($stateParams.id, function(data) {
                                $scope.members = data;
                            });
                        }
                    });
                }
            };
            $scope.memberDown = function(mid, msg) {
                if (confirm(msg)) {
                    ProjectService.updateMember(mid, "down", function(data) {
                        if (data.r > 0) {
                            ProjectService.members($stateParams.id, function(data) {
                                $scope.members = data;
                            });
                        }
                    });
                }
            };
            $scope.memberRemove = function(mid, msg) {
                if (confirm(msg)) {
                    ProjectService.updateMember(mid, "remove", function(data) {
                        if (data.r > 0) {
                            ProjectService.members($stateParams.id, function(data) {
                                $scope.members = data;
                            });
                        }
                    });
                }
            };
    }]);


    app.controller('ProjectCreateCtrl', ['$scope', '$stateParams', '$state', 'ProjectService', 'TemplateService',
        function($scope, $stateParams, $state, ProjectService, TemplateService) {

            $scope.saveOrUpdate = function(project) {

                project.items = [];
                angular.forEach($scope.items, function(item) {
                    project.items.push({name: item.itemName, value: item.value})
                });

                ProjectService.save(angular.toJson(project), function(data) {
                    if (data.r >= 0) {
                        $state.go('^');
                    } else if (data.r == 'exist') {
                        $scope.form.name.$invalid = true;
                        $scope.form.name.$error.exists = true;
                    }
                });
            };

            // load template all
            TemplateService.all(function(data) {
                $scope.templates = data;
            });

            // template change
            $scope.change = function(x) {
                $scope.items = [];
                if(typeof x === 'undefined') {
                    return;
                }
                TemplateService.items(x, function(data) {
                    $scope.items = data;
                    // default init <input> ng-model value
                    angular.forEach($scope.items, function(item) {
                        if (item.default) {
                            item.value = item.default;
                        }
                    })
                });
            }
    }]);

    app.controller('ProjectUpdateCtrl', ['$scope', '$stateParams', '$filter', '$state', 'ProjectService', 'TemplateService',
        function($scope, $stateParams, $filter, $state, ProjectService, TemplateService) {

            // update
            $scope.saveOrUpdate = function(project) {
                project.items = [];
                angular.forEach($scope.items, function(item) {
                    project.items.push({name: item.itemName, value: item.value})
                });

                project.lastUpdated = $filter('date')(project.lastUpdated, "yyyy-MM-dd HH:mm:ss")
                ProjectService.update($stateParams.id, angular.toJson(project), function(data) {
                    if (data !== '0') {
                        $state.go("conf.project.my");
                    }
                });

            };

            ProjectService.get($stateParams.id, function(data) {
                // update form reset
                $scope.master = data;
                $scope.reset = function() {
                    $scope.project = angular.copy($scope.master);
                };
                $scope.isUnchanged = function(project) {
                    return angular.equals(project, $scope.master);
                };
                $scope.reset();

                $scope.change(data.templateId)


            });

            // load template all
            TemplateService.all(function(data) {
                $scope.templates = data;
            });

            // template change
            $scope.change = function(x) {
                $scope.items = [];
                if(typeof x === 'undefined') {
                    return;
                }
                TemplateService.items(x, function(data) {
                    $scope.items = data;
                    // attrs
                    ProjectService.atts($stateParams.id, function(attdata) {
                        angular.forEach($scope.items, function(item) {
                            angular.forEach(attdata, function(att) {
                                if (att.name == item.itemName) {
                                    item.value = att.value;
                                    return;
                                }
                            });
                        });
                    });

                });
            }
    }]);


    // ===================================================================
    // ------------------------------项目版本-----------------------------—
    // ===================================================================
    app.controller('VersionCtrl', ['$scope', '$stateParams', '$modal', 'VersionService',
        function($scope, $stateParams, $modal, VersionService) {

            $scope.currentPage = 1;
            $scope.pageSize = 10;

            // count
            VersionService.count($stateParams.id, function(data) {
                $scope.totalItems = data;
            });

            // list
            VersionService.getPage($stateParams.id, 0, $scope.pageSize, function(data) {
                $scope.versions = data;
            });

            // page
            $scope.setPage = function (pageNo) {
                VersionService.getPage($stateParams.id, pageNo - 1, $scope.pageSize, function(data) {
                    $scope.versions = data;
                });
            };

            // remove
            $scope.delete = function(id, index) {
                var modalInstance = $modal.open({
                    templateUrl: 'partials/modal.html',
                    controller: function ($scope, $modalInstance) {
                        $scope.ok = function () {
                            VersionService.remove(id, function(state) {
                                $modalInstance.close(state);
                            });
                        };
                        $scope.cancel = function () {
                            $modalInstance.dismiss('cancel');
                        };
                    }
                });
                modalInstance.result.then(function(data) {
                    if (data.r >= 0) {
                        $scope.versions.splice(index, 1);
                        VersionService.count($stateParams.id, function(num) {
                            $scope.totalItems = num;
                        });
                    } else if (data.r == 'exist') {
                        alert('还有配置在使用该版本，请删除后再操作。。。')
                    } else if (data.r == 'none') {
                        alert('不存在的版本？')
                    }
                });
            };
    }]);


    app.controller('VersionCreateCtrl', ['$scope', '$filter', '$stateParams', '$state', 'VersionService',
        function($scope, $filter, $stateParams, $state, VersionService) {
            $scope.version = {projectId: $stateParams.id, vs: ''}

            $scope.saveOrUpdate = function(version) {
                version.updated = $filter('date')(new Date(), "yyyy-MM-dd HH:mm:ss")
                VersionService.save(angular.toJson(version), function(data) {
                    if (data.r >= 0) {
                        $state.go('^');
                    } else if (data.r == 'exist') {
                        $scope.form.vs.$invalid = true;
                        $scope.form.vs.$error.exists = true;
                    }
                });
            };

            VersionService.getNexusVersions($stateParams.id, function(data) {
                $scope.versions = data;
            });

    }]);


    app.controller('VersionUpdateCtrl', ['$scope', '$stateParams', '$filter', '$state', 'VersionService',
        function($scope, $stateParams, $filter, $state, VersionService) {
            $scope.saveOrUpdate = function(version) {
                version.updated = $filter('date')(new Date(), "yyyy-MM-dd HH:mm:ss")

                VersionService.update($stateParams.vid, angular.toJson(version), function(data) {
                    if (data.r >= 0) {
                        $state.go('^');
                    } else if (data.r == 'exist') {
                        $scope.form.vs.$invalid = true;
                        $scope.form.vs.$error.exists = true;
                    }
                });
            };

            VersionService.get($stateParams.vid, function(data) {
                // update form reset
                $scope.master = data;
                $scope.reset = function() {
                    $scope.version = angular.copy($scope.master);
                };
                $scope.isUnchanged = function(version) {
                    return angular.equals(version, $scope.master);
                };
                $scope.reset();
            });

            VersionService.getNexusVersions($stateParams.id, function(data) {
                $scope.versions = data;
            });
    }]);

});
