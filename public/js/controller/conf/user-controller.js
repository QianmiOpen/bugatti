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
                    if (data.r >= 0) {
                        $state.go('^');
                    } else if (data.r == 'exist') {
                        $scope.form.jobNo.$invalid = true;
                        $scope.form.jobNo.$error.exists = true;
                    }
                });
            };

        }]);

    app.controller('UserUpdateCtrl', ['$scope', '$filter', '$stateParams', '$state', 'UserService',
        function($scope, $filter, $stateParams, $state, UserService) {
            $scope.user, $scope.master = {};
            $scope.permission = {user:"0", area: "0", env:"0", project:"0", relation:"0", task:"0"}
            $scope.taskChecked = function(task, user) {
                if (task !== '0') {
                    $scope.permission = {user: user, area: "2", env:"3", project:"4", relation:"5", task: task}
                }
            };
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
                    if (val === 'user') $scope.permission.user = "1";
                    else if (val === 'area') $scope.permission.area = "2";
                    else if (val === 'env') $scope.permission.env = "3";
                    else if (val === 'project') $scope.permission.project = "4";
                    else if (val === 'relation') $scope.permission.relation = "5";
                    else if (val === 'task') $scope.permission.task = "6";
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
                user.lastVisit = $filter('date')(user.lastVisit, "yyyy-MM-dd hh:mm:ss")
                UserService.update($stateParams.id, angular.toJson(user), function(data) {
                    if (data.r >= 0) {
                        $state.go('^');
                    } else if (data.r == 'exist') {
                        $scope.form.jobNo.$invalid = true;
                        $scope.form.jobNo.$error.exists = true;
                    }
                });
            };

    }]);

});

