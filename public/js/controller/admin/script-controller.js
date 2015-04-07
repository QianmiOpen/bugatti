'use strict';

define(['angular'], function(angular){
    var app = angular.module('bugattiApp.controller.admin.scriptModule', []);

    app.controller('ScriptCtrl', ['$scope', '$modal', 'growl', 'ScriptService', function($scope, $modal, growl, ScriptService){
        $scope.app.breadcrumb='脚本设置';
        $scope.refresh = function() {
            var modalInstance = $modal.open({
                templateUrl: "partials/modal.html",
                controller: function ($scope, $modalInstance) {
                    $scope.ok = function() {
                        ScriptService.refresh(function(data) {
                            $modalInstance.close(data);
                        });
                    };
                    $scope.cancel = function() {
                        $modalInstance.dismiss("cancel")
                    };
                }
            });
            modalInstance.result.then(function(data) {
                if (data.r === 'ok') {
                    growl.addSuccessMessage("刷新成功");
                } else {
                    growl.addWarnMessage("刷新失败");
                }
            });
        }
    }]);
});

