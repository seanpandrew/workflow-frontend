define([
    'angular',
    'controllers/stubs'
], function(
    angular,
    stubsControllers
) {
    'use strict';

    var ComposerModalInstanceCtrl = function ($scope, $modalInstance) {

        $scope.type = {'contentType': 'article'};

        $scope.ok = function () {
            $modalInstance.close($scope.type.contentType);
        };
        $scope.cancel = function () {
            $modalInstance.dismiss('cancel');
        };
    };

    stubsControllers.controller('ComposerModalInstanceCtrl', ['$scope','$modalInstance','items', ComposerModalInstanceCtrl]);

    stubsControllers.controller('ComposerModalCtrl', ['$scope', '$modal', '$http', function($scope, $modal, $http){

        $scope.open = function (stub, composerUrl) {
            var modalInstance = $modal.open({
                templateUrl: 'composerModalContent.html',
                controller: ComposerModalInstanceCtrl
            });

            modalInstance.result.then(function (type) {
                $http({
                    method: 'POST',
                    url: composerUrl,
                    params: {'type': type},
                    withCredentials: true
                }).success(function(data){
                    var composerId = data.data.id;
                    $http({
                       method: 'POST',
                       url: '/updateStub/' + stub.id,
                       params: {'composerId': composerId}
                    }).success(function(){
                        $scope.$emit('getStubs');
                    });
                });
            });
        };
    }])

    return stubsControllers;
});