define([
    'angular',
    'sugar',
    'moment',
    'controllers/stubs'
], function (
    angular,
    sugar,
    moment,
    stubsControllers
) {
    'use strict';



    var StubModalInstanceCtrl = function ($scope, $modalInstance) {
        //default to technology for first pass of testing
        $scope.stubForm = {'section': 'Technology'};
        $scope.ok = function () {
            $modalInstance.close($scope.stubForm);
        };
        $scope.cancel = function () {
            $modalInstance.dismiss('cancel');
        };
    };

    stubsControllers.controller('StubModalInstanceCtrl', ['$scope','$modalInstance','items', StubModalInstanceCtrl]);

    stubsControllers.controller('StubModalCtrl', ['$scope', '$modal', '$http', function($scope, $modal, $http){
        $scope.open = function () {

            var modalInstance = $modal.open({
                templateUrl: 'stubModalContent.html',
                controller: StubModalInstanceCtrl
            });

            modalInstance.result.then(function (stub) {
                var newStub = angular.copy(stub);
                newStub.due = Date.create(stub.due).toISOString();
                $http({
                    method: 'POST',
                    url: '/newStub',
                    params: newStub,
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).success(function(){
                    $scope.$emit('getStubs');
                });
            });
        };
    }])

    return stubsControllers;
});