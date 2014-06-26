'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.controller.conf.userModule', []);

    app.controller('UserCtrl', ['$scope', '$stateParams', '$state', '$modal', 'UserService',
        function($scope, $stateParams, $state, $modal, UserService) {
        $scope.currentPage = 1;
        $scope.pageSize = 10;

        // count
        UserService.count(function(data) {
            $scope.totalItems = data;
        });

        // list
        UserService.getPage(0, $scope.pageSize, function(data) {
            $scope.users = data;
        });

        // set page
        $scope.setPage = function (pageNo) {
            UserService.getPage(pageNo - 1, $scope.pageSize, function(data) {
                $scope.users = data;
            });
        };

        // remove
        $scope.delete = function(jobNo, index) {
            var modalInstance = $modal.open({
                templateUrl: 'partials/modal.html',
                controller: function ($scope, $modalInstance) {
                    $scope.ok = function () {
                        UserService.remove(jobNo, function(state) {
                            $modalInstance.close(state);
                        });
                    };
                    $scope.cancel = function () {
                        $modalInstance.dismiss('cancel');
                    };
                }
            });
            modalInstance.result.then(function(state) {
                if (state !== '0') {
                    $scope.users.splice(index, 1);
                    UserService.count(function(num) {
                        $scope.totalItems = num;
                    });
                }
            });
        };
    }]);

    app.controller('UserShowCtrl', ['$scope', '$stateParams', 'UserService', function($scope, $stateParams, UserService) {
        // init
        $scope.user = {};
        // show
        UserService.get($stateParams.id, function(data) {
            $scope.user = data;
        });

    }]);

    app.controller('UserCreateCtrl', ['$scope', '$stateParams', '$state', 'UserService',
        function($scope, $stateParams, $state, UserService) {
            $scope.user = {role: 'user', locked: false}
            $scope.saveOrUpdate = function(user, permission) {
                var func = []
                angular.forEach(permission, function(val) {
                    if (val !== '0') {
                        func.push(val)
                    }
                });
                $scope.user.functions = func.join(",");
                UserService.save(angular.toJson(user), function(data) {
                    console.log('data=' + angular.toJson(data));
                    if (data.r === 2) {
                        $state.go('^');
                    } else if (data.r == 'exist') {
                        $scope.form.jobNo.$invalid = true;
                        $scope.form.jobNo.$error.exists = true;
                    }
                });
            };

        }]);

    app.controller('UserUpdateCtrl', ['$scope', '$stateParams', '$state', 'UserService',
        function($scope, $stateParams, $state, UserService) {
            $scope.user, $scope.master = {};
            $scope.permission = {user:"0", env:"0", project:"0", relation:"0", task:"0"}

            UserService.get($stateParams.id, function(data) {
                $scope.master = data;
                $scope.reset = function() {
                    $scope.user = angular.copy($scope.master);
                };
                $scope.isUnchanged = function(user) {
                    return angular.equals(user, $scope.master);
                };
                $scope.reset();

            });

            UserService.permissions($stateParams.id, function(data) {
                if (data === 'null') {
                    return;
                }
                angular.forEach(data.functions, function(val) {
                    if (val === '用户管理') $scope.permission.user = "1";
                    else if (val === '环境管理') $scope.permission.env = "2";
                    else if (val === '项目管理') $scope.permission.project = "3";
                    else if (val === '关系配置') $scope.permission.relation = "4";
                    else if (val === '任务管理') $scope.permission.task = "5";
                });
            });

            $scope.saveOrUpdate = function(user, permission) {
                var func = []
                angular.forEach(permission, function(val) {
                    if (val !== '0') {
                        func.push(val)
                    }
                });
                $scope.user.functions = func.join(",");
                UserService.update($stateParams.id, angular.toJson(user), function(data) {
                    if (data.r === 2) {
                        $state.go('^');
                    } else if (data.r == 'exist') {
                        $scope.form.jobNo.$invalid = true;
                        $scope.form.jobNo.$error.exists = true;
                    }
                });
            };

    }]);

});

