import angular from 'angular';

import 'lib/date-service';
import 'lib/filters-service';

angular.module('wfDashboardSidebar', ['wfFiltersService'])
    .controller('wfDashboardSidebarController', ['$scope', 'wfFiltersService', 'wfDateParser', 'prodOfficeService', 'sections', function ($scope, wfFiltersService, prodOfficeService, sections) {

        $scope.filters = [
            {
                title: 'Status',
                namespace: 'status',
                filterOptions: (function (s) {
                    return s.map(function (v, i) {
                        return {
                            caption: v,
                            value: v // TODO: normalise this to lower case...
                        };
                    });
                })($scope.statuses)
            },
            {
                title: 'Content',
                namespace: 'content-type',
                filterOptions: [
                    { caption: 'Article', value: 'article', icon: 'file' },
                    { caption: 'Liveblog', value: 'liveblog', icon: 'th-list' },
                    { caption: 'Gallery', value: 'gallery', icon: 'camera' },
                    { caption: 'Interactive', value: 'interactive', icon: 'hand-up' }
                ]
            },
            {
                title: 'Creation date',
                namespace: 'created',
                filterOptions: [
                    { caption: 'Yesterday', value: 'yesterday' },
                    { caption: 'Today', value: 'today' },
                    { caption: 'Last 24 Hours', value: 'last24' },
                    { caption: 'Last 48 Hours', value: 'last48' }
                ]
            },
            {
                title: 'State',
                namespace: 'state',
                filterOptions: [
                    { caption: 'Draft', value: 'draft' },
                    { caption: 'Published', value: 'published' }
                ]
            },
            {
                title: 'Flags',
                namespace: 'flags',
                filterOptions: [
                    { caption: 'Needs legal', value: 'needsLegal' }
                ]
            }
        ];
    }])

    .directive('wfSidebarFilter', ['wfFiltersService', function (wfFiltersService) {

        return {
            restrict: 'E',
            replace: true,
            templateUrl: '/assets/components/sidebar-filter/sidebar-filter.html',
            link: function ($scope, elem, attrs) {
                $scope.defaultFilter = { caption: "All", value: null };
                $scope.selectedFilter = wfFiltersService.get($scope.filter.namespace);

                if (!$scope.selectedFilter) {
                    $scope.selectedFilter = null;
                }

                $scope.filterIsSelected = function(filter) {
                    return (filter != null && filter.value === $scope.selectedFilter);
                };

                $scope.filterClick = function(filter) {
                    if($scope.filterIsSelected(filter)) {
                        $scope.selectedFilter = null;
                    } else {
                        $scope.selectedFilter = filter.value;
                    }

                    $scope.$emit('filtersChanged.' + $scope.filter.namespace, $scope.selectedFilter);
                };

                $scope.toggleSidebarSection = function () {

                    // TODO: Refactor...

                    var list = elem[0].querySelector('.sidebar__filter-list');

                    if (elem[0].getAttribute('data-list-height') == null) {
                        elem[0].setAttribute('data-list-height', list.offsetHeight);
                        list.style.maxHeight = elem[0].getAttribute('data-list-height') + 'px';
                    }

                    if (elem[0].getAttribute('data-is-open') == 'true') {
                        elem[0].setAttribute('data-is-open', 'false');
                        setTimeout(function () { // Ensure that the max-height style has been set before setting it to 0 to ensure animation triggers...
                            list.style.maxHeight = '0px';
                        }, 0);

                    } else {
                        elem[0].setAttribute('data-is-open', 'true');
                        list.style.maxHeight = elem[0].getAttribute('data-list-height') + 'px';
                    }

                };
            }
        };
    }]);
