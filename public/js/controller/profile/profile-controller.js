'use strict';

define(['angular'], function(angular){
    var app = angular.module('bugattiApp.controller.profile.profileModule', []);

    app.controller('ProfileCtrl', ['$scope', '$filter', 'growl', 'UserService', function($scope, $filter, growl, UserService) {

        $scope.user = {};
        UserService.get($scope.loginUser.username, function(data) {
            $scope.user = data;
        });

        $scope.saveOrUpdate = function(user) {
            user.lastVisit = $filter('date')(user.lastVisit, "yyyy-MM-dd HH:mm:ss")
            UserService.update($scope.loginUser.username, angular.toJson(user), function(data) {
                if (data == 1) {
                    growl.addSuccessMessage("修改成功");
                } else {
                    growl.addErrorMessage("修改失败");
                }
            });
        };
    }]);

    app.controller('ProfileKeysCtrl', ['$scope', '$filter', 'growl', 'UserService', function($scope, $filter, growl, UserService) {
        $scope.user = {};
        UserService.get($scope.loginUser.username, function(data) {
            $scope.user = data;
        });

        $scope.saveOrUpdate = function(user) {
            user.lastVisit = $filter('date')(user.lastVisit, "yyyy-MM-dd HH:mm:ss")
            UserService.update($scope.loginUser.username, angular.toJson(user), function(data) {
                if (data == 1) {
                    growl.addSuccessMessage("保存成功");
                } else {
                    growl.addErrorMessage("保存失败");
                }
            });
        };
    }]);
});

