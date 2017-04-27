/* global module:false, __dirname:false */

var path = require('path');
var webpack = require('webpack');
var ExtractTextPlugin = require('extract-text-webpack-plugin');

var ExtractCSS = new ExtractTextPlugin("main.css")
var ExtractCoreCSS = new ExtractTextPlugin("core.css")

module.exports = {
    devtool: 'cheap-module-source-map',
    module: {
        loaders: [
            {
                test:    /\.js$/,
                exclude: /node_modules/,
                loaders: ['babel-loader']
            },
            {
              test: /\.(html|svg)$/,
              exclude: '/app/',
              loader: 'html-loader'
            },
            {
              test: /\.json$/,
              exclude: /node_modules/,
              use: 'json-loader'
            },
            {
                test: /\.scss$/,
                exclude: /core\.scss$/,
                loader: ExtractCSS.extract({
                    fallback: 'style-loader',
                    use: 'css-loader?sourceMap!sass-loader?sourceMap'
                })
            },
            {
                test: /core\.scss$/,
                loader: ExtractCoreCSS.extract({
                    fallback: 'style-loader',
                    use: 'css-loader?sourceMap!sass-loader?sourceMap'
                })
            },
            {
                test: /\.css$/,
                loader: ExtractTextPlugin.extract({
                    fallback: 'style-loader',
                    use: 'css-loader?sourceMap'
                })
            },
            {
                test: /\.woff(2)?(\?v=[0-9].[0-9].[0-9])?$/,
                loader: "url-loader?mimetype=application/font-woff&limit=10000"
            },
            {
                test: /\.(ttf|eot|gif|png)(\?v=[0-9].[0-9].[0-9])?$/,
                loader: "file-loader?name=[name].[ext]"
            }
        ]
    },

    resolve: {
        extensions: ['.js', '.jsx', '.json', '.scss'],
        alias: {
            lib: path.join(__dirname, '..', 'public', 'lib'),
            components: path.join(__dirname, '..', 'public', 'components'),
            layouts: path.join(__dirname, '..', 'public', 'layouts'),
        }
    },

    plugins: [
        ExtractCSS,
        ExtractCoreCSS
    ]
};
