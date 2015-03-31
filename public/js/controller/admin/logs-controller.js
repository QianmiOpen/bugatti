'use strict';

define(['angular'], function(angular){
    var app = angular.module('bugattiApp.controller.admin.logsModule', []);

    app.controller('LogsCtrl', ['$scope', '$filter', '$modal', 'LogsService', function($scope, $filter, $modal, LogsService){
        $scope.app.breadcrumb='日志查看';
        $scope.currentPage = 1;
        $scope.pageSize = 20;

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
            {'key': '用户管理', 'val':'user'},
            {'key': '区域管理', 'val':'area'},
            {'key': '关系设置', 'val':'relation'},
            {'key': '系统配置', 'val':'system'},
            {'key': '网关管理', 'val':'spirit'},
            {'key': '脚本设置', 'val':'script'},
            {'key': '任务管理(+负载)', 'val':'task'},
            {'key': '环境管理', 'val':'env'},
            {'key': '项目管理(+属性,变量)', 'val':'project'},
            {'key': '成员管理(=项目,环境)', 'val':'member'},
            {'key': '项目配置', 'val':'conf'},
            {'key': '项目模板', 'val':'template'},
            {'key': '项目版本', 'val':'version'},
            {'key': '项目依赖', 'val':'depend'}
        ];

        $scope.submit = function(pageNo) {
            var paramLogs = angular.copy($scope.logs)
            paramLogs.startTime = paramLogs.startTime + ' 00:00:00';
            paramLogs.endTime = paramLogs.endTime + ' 23:59:59';
            var start = angular.isUndefined(pageNo) ? 0 : pageNo - 1;
            LogsService.count(angular.toJson(paramLogs), function(data) {
                $scope.totalItems = data;
            });
            LogsService.search(angular.toJson(paramLogs), start, $scope.pageSize, function(data) {
                $scope.data = data;
            });
        };

        $scope.setPage = function (pageNo) {
            $scope.submit(pageNo);
        }

    }]);

});
