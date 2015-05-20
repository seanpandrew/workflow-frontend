import angular from 'angular';
import moment  from 'moment';
import _       from 'lodash';

import 'jquery'
import 'jquery-ui/draggable'
import 'jquery-ui/droppable'

import 'angular-dragdrop';

import { wfDateView } from 'components/plan-view/date-view/date-view';
import { wfBundleView } from 'components/plan-view/bundle-view/bundle-view';
import { wfDayView } from 'components/plan-view/day-view/day-view';
import { wfPlanItem } from 'components/plan-view/plan-item/plan-item';
import { wfInlineAddItem } from 'components/plan-view/inline-add-item/inline-add-item';
import { wfDateRangeWidget } from  'components/plan-view/date-range-widget/date-range-widget';


angular.module('wfPlan', ['wfPlanService', 'wfPollingService', 'wfFiltersService', 'ngDragDrop'])
    .directive('wfDateView', ['$rootScope','$timeout', 'wfDayNoteService', 'wfPlannedItemService', 'wfFiltersService', '$sce', wfDateView])
    .directive('wfBundleView', ['$rootScope','$timeout', 'wfBundleService', 'wfPlannedItemService', 'wfFiltersService', wfBundleView])
    .directive('wfDayView', ['$rootScope', 'wfPlannedItemService', '$http', '$timeout', 'wfFiltersService', wfDayView])
    .directive('wfPlanItem', ['$rootScope', '$http', '$timeout', 'wfContentService', 'wfBundleService', 'wfPlannedItemService', wfPlanItem])
    .directive('wfInlineAddItem', ['$timeout', wfInlineAddItem])
    .directive('wfDateRangeWidget', ['$timeout', 'wfFiltersService', wfDateRangeWidget])
    .service('wfPlanLoader', [ 'wfHttpSessionService', 'wfPlanService', 'wfPollingService', 'wfFiltersService', '$rootScope', '$http', function (http, planService, PollingService, wfFiltersService, $rootScope, $http) {

        var filterParams = wfFiltersService.getAll();
        function params() {
            return {
                'newsList': filterParams['news-list'],
                'startDate': filterParams['plan-start-date'],
                'endDate': filterParams['plan-end-date']
            }
        }

        this.poller = new PollingService(planService, params);

        this.render = (response) => {
            $rootScope.$broadcast('plan-view__data-load', response.data.data);
        };

        this.renderError = (err) => {
            console.log(err)
        };

        this.poller.onPoll(this.render);
        this.poller.onError(this.renderError);

        this.poller.startPolling();
    }])
    .controller('wfPlanController', ['$scope', '$rootScope', 'wfPlanLoader', '$http', '$timeout', 'wfDayNoteService', 'wfFiltersService', 'wfPlannedItemService', function wfPlanController ($scope, $rootScope, planLoader, $http, $timeout, wfDayNoteService, wfFiltersService, wfPlannedItemService) {

        // ADD ALPHA LABEL
        (function createBetaLabel () {
            let title = angular.element('<span class="plan-view__title">Plan view <span class="plan-view__title--beta">ALPHA</span></span>');
            document.querySelector('.top-toolbar__title').appendChild(title[0]);
        })();
        // ! ADD ALPHA LABEL

        $rootScope.$on('plan-view__ui-loaded', function() {
            $scope.isLoaded = true;
        });

        (function monitorDateRangeForChanges () {

            let ISO_8601 = 'YYYY-MM-DD';

            $scope.planDateRange = null;

            $scope.$watch('planDateRange.startDate', (newValue, oldValue) => {
                if (newValue) {
                    $scope.$emit('plan-view__filters-changed.plan-start-date', newValue.format(ISO_8601));
                }
            }, true);

            $scope.$watch('planDateRange.endDate', (newValue, oldValue) => {
                if (newValue) {
                    $scope.$emit('plan-view__filters-changed.plan-end-date', newValue.format(ISO_8601));
                }
            }, true);
        })();

        $scope.selectedDate = moment().startOf('day');

        $scope.selectDay = function (date) {
            $scope.selectedDate = date;
        };

        $scope.plannedItems = [];
        $scope.plannedItemsByBundle = [];

        $scope.$on('plan-view__data-load', function (ev, data) {

            data.forEach((bundle) => {
                bundle.items.map((item) => {
                    item.plannedDate = moment(item.plannedDate);
                    return item;
                });
            });

            $scope.plannedItemsByBundle = data;

            $scope.plannedItems = $scope.plannedItemsByBundle.map((bundle) => {
                return bundle.items;
            }).flatten();

            $scope.$broadcast('plan-view__planned-items-changed', $scope.plannedItemsByBundle);
        });

        /**
         * These watchers handle changes to various scope properties when they are changed
         * and ensure that the data is correctly processed for use by the UI via updateScopeItems
         */
        (function setUpWatchersForUserActionScopeChanges () {

            // Currently Selected Date
            $scope.$watch('selectedDate', (newValue, oldValue) => {
                $scope.currentlySelectedDay = newValue;
                $timeout(updateScopeItems);
            }, false);

            // arameters loaded from the URL
            $scope.$on('plan-view__filters-changed', function() {

                planLoader.poller.refresh()
                    .then(() => {
                        $timeout(updateScopeItems);
                    });
                $scope.newsList = wfFiltersService.get('news-list');
            });

            // Item added to interface via quick add
            $scope.$on('quick-add-submit', function (ev, item) {

                wfPlannedItemService.add(item)
                    .then((res) => {
                        planLoader.poller.refresh()
                            .then(() => {
                                $timeout(updateScopeItems);
                            });
                        $rootScope.$emit('quick-add-success');
                    })
                    .catch((err) => {
                        $rootScope.$emit('quick-add-failure');
                    });
            });

            // Items altered in Plan View
            $scope.$on('plan-view__planned-items-changed', () => {
                planLoader.poller.refresh()
                    .then(() => {
                        $timeout(updateScopeItems);
                    });
            });

            // Items altered in bundle view TODO <<< merge with above method
            $scope.$on('plan-view__bundles-edited', () => {
                planLoader.poller.refresh()
                    .then(() => {
                        $timeout(updateScopeItems);
                    });
            });
        })();

        /**
         * Create the scope items for the bundle and day view using the currently selected date
         */
        function updateScopeItems() {

            let selectedDay = moment($scope.currentlySelectedDay),
                selectedDayPlusOne = moment($scope.currentlySelectedDay).add(1, 'days');

            /**
             * Does a plannedItems date fall within a date range?
             * @param item
             * @param dateFrom
             * @param dateTo
             * @returns {*}
             */
            function itemIsWithinDateRange (item, dateFrom, dateTo) {

                return (item.plannedDate.isSame(dateFrom) || item.plannedDate.isAfter(dateFrom)) &&
                    item.plannedDate.isBefore(dateTo)
            }

            // Filter items for the bundle view
            $scope.dayItemsByBundle = $scope.plannedItemsByBundle.map((bundle) => {

                bundle.itemsToday = bundle.items.filter((item) => {

                    return itemIsWithinDateRange(item, selectedDay, selectedDayPlusOne);
                });
                return bundle;
            });

            // Filter items for the day view
            $scope.dayItems = $scope.plannedItems.filter((item) => {

                return itemIsWithinDateRange(item, selectedDay, selectedDayPlusOne);
            });
        }

    }]);
