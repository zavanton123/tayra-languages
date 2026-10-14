#!/usr/bin/env node
/*
 * Renders flags from flag-icons (MIT) into the 96×72 PNGs core/ui shows, named flag_<code>.png.
 *
 * Usage: node tools/render_flags.mjs <node_modules dir with flag-icons and @resvg/resvg-js> <code>...
 *   e.g. node tools/render_flags.mjs ~/flags/node_modules ie mt gb-sct
 */
import fs from 'node:fs';
import path from 'node:path';
import {createRequire} from 'node:module';

const [modules, ...codes] = process.argv.slice(2);
if (!modules || codes.length === 0) {
    console.error('usage: node tools/render_flags.mjs <node_modules dir> <code>...');
    process.exit(2);
}
const require = createRequire(path.join(path.resolve(modules), 'index.js'));
const {Resvg} = require('@resvg/resvg-js');
const OUT = 'core/ui/src/commonMain/composeResources/drawable';
for (const code of codes) {
    const svg = fs.readFileSync(path.join(modules, 'flag-icons', 'flags', '4x3', `${code}.svg`));
    const png = new Resvg(svg, {fitTo: {mode: 'width', value: 96}}).render().asPng();
    const file = path.join(OUT, `flag_${code.replace('-', '_')}.png`);
    fs.writeFileSync(file, png);
    console.error(`wrote ${file}`);
}
