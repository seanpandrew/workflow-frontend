/**
 * Global environment variables.
 *
 * Hostname specific variables available, otherwise uses "default".
 */

var env = {
    'default': {
        'sentry': {
            'url': 'https://b92970b12e8948f2848523b719270c15@app.getsentry.com/33125'
        }
    },

    'workflow.gutools.co.uk': {
        'sentry': {
            'url': 'https://c1747e2a3b9542b18e1bef52c45c05c6@app.getsentry.com/33134'
        }
    }
};


export function getEnvironment(host = window.location.host) {
    return Object.freeze(env[host] || env.default);
}
