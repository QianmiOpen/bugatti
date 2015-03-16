'use strict';

define(['angular'], function(angular){
    var app = angular.module('bugattiApp.controller.profile.profileModule', []);

    app.controller('ProfileCtrl', ['$scope', '$filter', 'growl', 'Auth', 'UserService',
        function($scope, $filter, growl, Auth, UserService) {
        $scope.loginUser = Auth.user;

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
});

