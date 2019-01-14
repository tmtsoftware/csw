var webpack = require('webpack');

module.exports = require('./scalajs.webpack.config');
module.exports.entry.polyfill = './polyfills.js';
