'use strict';

define(['angular'], function(angular){
    var app = angular.module('bugattiApp.controller.logs.logsModule', []);

    app.controller('LogsCtrl', ['$scope', '$filter', '$modal', 'LogsService', function($scope, $filter, $modal, LogsService){
        var date = new Date();
        $scope.logs = {
            startTime: $filter('date')(date.setYear(date.getFullYear() - 1), 'yyyy-MM-dd'),
            endTime: $filter('date')(new Date(), "yyyy-MM-dd")
        };

        $scope.dateOptions = {
            formatYear: 'yy',
            startingDay: 1,
            showWeeks: false
        };

        $scope.open = function($event) {
            $event.preventDefault();
            $event.stopPropagation();
            $scope.opened = true;
        };

        $scope.modes = [
            {'key': '用户模块', 'val':'user'},
            {'key': '区域模块', 'val':'area'},
            {'key': '环境模块', 'val':'env'},
            {'key': '项目模块', 'val':'project'},
            {'key': '关系模块', 'val':'relation'},
            {'key': '任务模块', 'val':'task'},
            {'key': '项目配置文件模块', 'val':'conf'},
            {'key': '项目成员模块', 'val':'member'},
            {'key': '项目模板模块', 'val':'template'},
            {'key': '项目版本模块', 'val':'version'}
        ];

        $scope.submit = function() {
            var paramLogs = angular.copy($scope.logs)
            paramLogs.startTime = paramLogs.startTime + ' 00:00:00'
            paramLogs.endTime = paramLogs.endTime + ' 23:59:59'
            LogsService.search(angular.toJson(paramLogs), function(data) {
                $scope.data = data;
            });
        };


    }]);

});
