import angular from 'angular';

angular.module('wfComposerService', [])
    .service('wfComposerService', ['$http', '$q', 'config', '$log', 'wfHttpSessionService', wfComposerService]);

function wfComposerService($http, $q, config, $log, wfHttpSessionService) {

    const request = wfHttpSessionService.request;

    const composerContentFetch = config.composerContentDetails;

    function composerUpdateFieldUrl(fieldName, contentId) {
        function liveOrPreview(isPreview) {
            return `${composerContentFetch}${contentId}/${isPreview ? "preview" : "live"}/fields/${fieldName}`
        }
        return {
            preview: liveOrPreview(true),
            live:    liveOrPreview(false)
        }
    }

    // budget composer url parser - just gets the portion after the last '/'
    function parseComposerId(url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }

    function deepSearch(obj, path) {
        if (path.length === 0) return obj;
        const next = path[0];
        if (obj[next]) return deepSearch(obj[next], path.slice(1));
        else return null;
    }

    // Mapping of workflow content fields to transform functions on composer response
    const composerParseMap = {
        composerId: (d) => d.id,
        contentType: (d) => d.type,
        headline: (d) => deepSearch(d, ['preview', 'data', 'fields', 'headline', 'data']) || undefined,
        published: (d) => d.published,
        timePublished: (d) => new Date(deepSearch(d, ['contentChangeDetails', 'data', 'published', 'date']) || undefined),
        path: (d) => deepSearch(d, ['identifiers', 'path', 'data']),
        commentable: (d) => deepSearch(d, ['preview', 'data', 'settings', 'commentable', 'data']) === 'true',
        takenDown: (d) => false, // TODO: takenDown from composer feed
        activeInInCopy: (d) => deepSearch(d, ['toolSettings', 'activeInInCopy', 'data']) === 'true',
        composerProdOffice: (d) => deepSearch(d, ['preview', 'data', 'settings', 'productionOffice', 'data']) || undefined,
        optimisedForWeb: (d) => deepSearch(d, ['toolSettings', 'seoOptimised', 'data']) === 'true',
        optimisedForWebChanged: (d) => deepSearch(d, ['toolSettings', 'seoChanged', 'data']) === 'true',
        revision: (d) => deepSearch(d, ['contentChangeDetails', 'data', 'revision']),
        lastModified: (d) => new Date(deepSearch(d, ['contentChangeDetails', 'data', 'lastModified', 'date']) || undefined),
        lastModifiedBy: (d) => deepSearch(d, ['contentChangeDetails', 'data', 'lastModified', 'user', 'firstName']) + ' ' + deepSearch(d, ['contentChangeDetails', 'data', 'lastModified', 'user', 'lastName'])
    };


    function parseComposerData(response, target) {
        target = target || {};
        if (!response.data || !response.data.data || !response.data.data.id) {
            $log.error("Composer response missing id field. Response: " + JSON.stringify(response) + " \n Stub metadata: " + JSON.stringify(target))
            return Promise.reject({
                message: "composer response did not contain id, response: " + JSON.stringify(response)})
        } else {
            const data = response.data.data;

            Object.keys(composerParseMap).forEach((key) => {
                target[key] = composerParseMap[key](data);
            });

            return Promise.resolve(target);
        }
    }

    function getComposerContent(url) {
        return $http({
            method: 'GET',
            url: composerContentFetch + parseComposerId(url),
            params: {'includePreview': 'true'},
            withCredentials: true
        });
    }

    this.getComposerContent = getComposerContent;
    this.parseComposerData = parseComposerData;


    this.create = function createInComposer(type, commissioningDesks, commissionedLength) {
        var params = {
            'type': type,
            'tracking': commissioningDesks
        };

        if(commissionedLength) params['initialCommissionedLength'] = commissionedLength;

        return request({
            method: 'POST',
            url: config.composerNewContent,
            params: params,
            withCredentials: true
        });
    };

    this.updateField = function (composerId, fieldName, value, live = false) {
        let urls = composerUpdateFieldUrl(fieldName, composerId);
        let url = live ? urls.live : urls.preview;
        let req = {
            method: 'PUT',
            url: url,
            data: `"${value}"`,
            withCredentials: true
        };
        return request(req);
    };

}
