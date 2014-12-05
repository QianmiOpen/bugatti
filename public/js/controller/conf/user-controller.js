'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.controller.conf.userModule', []);

    app.controller('UserCtrl', ['$scope', '$stateParams', '$state', '$modal', 'UserService', 'Auth',
        function($scope, $stateParams, $state, $modal, UserService, Auth) {
            $scope.loginUser = Auth.user;
            $scope.currentPage = 1;
            $scope.pageSize = 20;

            $scope.searchForm = function(jobNo) {
                // count
                UserService.count(jobNo, function(data) {
                    $scope.totalItems = data;
                });

                // list
                UserService.getPage(jobNo, 0, $scope.pageSize, function(data) {
                    $scope.users = data;
                });
            };

            // default list
            $scope.searchForm($scope.s_jobNo);

            // set page
            $scope.setPage = function (pageNo) {
                UserService.getPage($scope.s_jobNo, pageNo - 1, $scope.pageSize, function(data) {
                    $scope.users = data;
                });
            };

            // remove
            $scope.delete = function(jobNo, index) {
                var modalInstance = $modal.open({
                    templateUrl: 'partials/modal.html',
                    controller: function ($scope, $modalInstance) {
                        $scope.ok = function () {
                            UserService.remove(jobNo, function(data) {
                                $modalInstance.close(data);
                            });
                        };
                        $scope.cancel = function () {
                            $modalInstance.dismiss('cancel');
                        };
                    }
                });
                modalInstance.result.then(function(data) {
                    $scope.users.splice(index, 1);
                    UserService.count($scope.s_jobNo, function(num) {
                        $scope.totalItems = num;
                    });
                });
            };
    }]);

    app.controller('UserShowCtrl', ['$scope', '$stateParams', 'UserService', '$timeout', '$http', '$upload',
        function($scope, $stateParams, UserService, $timeout, $http, $upload) {
        // init
        $scope.user = {};
        // show
        UserService.get($stateParams.id, function(data) {
            $scope.user = data;
        });

        // upload
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

        $scope.start = function(index) {
            $scope.progress[index] = 0;
            $scope.errorMsg = null;
            $scope.upload[index] = $upload.upload({
                url: '/user/' + $scope.user.jobNo + '/upload',
                method: 'post',
                headers: {'my-header': 'my-header-value'},
                data : {
                    //jobNo: $scope.user.jobNo
                    //projectId: 2,
                    //versionId: 3
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

    app.controller('UserCreateCtrl', ['$scope', '$stateParams', '$state', 'UserService', 'Auth',
        function($scope, $stateParams, $state, UserService, Auth) {
            $scope.loginUser = Auth.user;
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
                    if (data.r === 'exist') {
                        $scope.form.jobNo.$invalid = true;
                        $scope.form.jobNo.$error.exists = true;
                    } else {
                        $state.go('^');
                    }
                });
            };

        }]);

    app.controller('UserUpdateCtrl', ['$scope', '$filter', '$stateParams', '$state', 'UserService', 'Auth',
        function($scope, $filter, $stateParams, $state, UserService, Auth) {
            $scope.loginUser = Auth.user;
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
                user.lastVisit = $filter('date')(user.lastVisit, "yyyy-MM-dd HH:mm:ss")
                UserService.update($stateParams.id, angular.toJson(user), function(data) {
                    if (data.r === 'exist') {
                        $scope.form.jobNo.$invalid = true;
                        $scope.form.jobNo.$error.exists = true;
                    } else {
                        $state.go('^');
                    }
                });
            };

    }]);

});

