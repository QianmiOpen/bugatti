'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.controller.conf.projectModule', []);

    app.controller('ProjectCtrl', ['$scope', '$stateParams', '$modal', 'ProjectService', 'VersionService', function($scope, $stateParams, $modal, ProjectService, VersionService) {

        // search
        if ($stateParams.search_text) {
            $scope.search_text = $stateParams.search_text;
        }

        $scope.currentPage = 1;
        $scope.pageSize = 10;

        // count
        ProjectService.count(function(data) {
            $scope.totalItems = data;
        });

        // list
        ProjectService.getPage(0, $scope.pageSize, function(data) {
            $scope.projects = data;
        });

        // page
        $scope.setPage = function (pageNo) {
            ProjectService.getPage(pageNo - 1, $scope.pageSize, function(data) {
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
                    ProjectService.count(function(num) {
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

                project.lastUpdated = $filter('date')(project.lastUpdated, "yyyy-MM-dd hh:mm:ss")
                ProjectService.update($stateParams.id, angular.toJson(project), function(data) {
                    if (data !== '0') {
                        $state.go("^");
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
            $scope.version = {pid: $stateParams.id, vs: ''}

            $scope.saveOrUpdate = function(version) {
                version.updated = $filter('date')(new Date(), "yyyy-MM-dd hh:mm:ss")
                VersionService.save(angular.toJson(version), function(data) {
                    if (data.r >= 0) {
                        $state.go('^');
                    } else if (data.r == 'exist') {
                        $scope.form.vs.$invalid = true;
                        $scope.form.vs.$error.exists = true;
                    }
                });
            };
    }]);


    app.controller('VersionUpdateCtrl', ['$scope', '$stateParams', '$filter', '$state', 'VersionService',
        function($scope, $stateParams, $filter, $state, VersionService) {
            $scope.saveOrUpdate = function(version) {
                version.updated = $filter('date')(new Date(), "yyyy-MM-dd hh:mm:ss")

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
    }]);

});
