// Bundles sql.js for the SQLDelight web worker driver: the WebAssembly binary for the app
// database worker, plus the loader script that dictionary.worker.js imports.
config.resolve = config.resolve || {};
config.resolve.fallback = Object.assign({}, config.resolve.fallback, {
    fs: false,
    path: false,
    crypto: false,
});

const CopyWebpackPlugin = require('copy-webpack-plugin');
config.plugins.push(
    new CopyWebpackPlugin({
        patterns: [
            '../../node_modules/sql.js/dist/sql-wasm.wasm',
            '../../node_modules/sql.js/dist/sql-wasm.js',
        ]
    })
);
