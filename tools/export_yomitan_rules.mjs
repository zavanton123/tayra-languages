#!/usr/bin/env node
/*
 * Exports Yomitan's language rules into core/domain's morphology package: which text processors
 * each language uses (YomitanLanguages.kt), and its inflection transforms as data the Kotlin
 * LanguageTransformer reads (rules/<Code>Transforms.kt, listed by hand in TransformRules.kt).
 * Yomitan's own tests of each language's transforms (test/language/*-transforms.test.js) become
 * the cases the Kotlin tests check the port against (commonTest rules/<Code>TransformCases.kt).
 *
 * Usage: node tools/export_yomitan_rules.mjs <yomitan checkout>
 *
 * The checkout needs its third-party modules in ext/lib (npm ci && npm run build:libs there).
 *
 * A Yomitan rule is a regular expression that tells whether a word may be inflected, a function
 * that undoes the inflection, and the conditions (parts of speech, intermediate forms) it goes
 * from and to. The functions are rewritten as data: most cut letters off the ends and add others
 * ("slice"), some always give one word ("const"), the rest run a chain of String.replace calls,
 * which are recorded by calling the function on a probe string. A function that branches on how
 * the word begins becomes one rule per branch, guarded by that beginning; the few that do more
 * (Yiddish umlaut plurals, Spanish reflexive pronouns) are recognised by their source and named.
 *
 * Yomitan is GPL-3.0, Copyright (C) 2023-2026 Yomitan Authors; so are the generated files.
 */
import fs from 'node:fs';
import path from 'node:path';
import {pathToFileURL} from 'node:url';

const root = process.argv[2];
if (!root) {
    console.error('usage: node tools/export_yomitan_rules.mjs <yomitan checkout>');
    process.exit(2);
}
const OUT = 'core/domain/src/commonMain/kotlin/com/tayra/languages/core/domain/morphology';
const TEST_OUT = 'core/domain/src/commonTest/kotlin/com/tayra/languages/core/domain/morphology/rules';
const TEST_FILES = {
    sq: 'albanian', grc: 'ancient-greek', ar: 'arabic', en: 'english', ga: 'irish', de: 'german', eo: 'esperanto',
    la: 'latin', tl: 'tagalog', ko: 'korean', ja: 'japanese', es: 'spanish', el: 'modern-greek', uk: 'ukrainian', yi: 'yiddish',
};

/** The cases of a Yomitan transforms test: its "tests" array, evaluated on its own. */
function testCases(iso) {
    const name = TEST_FILES[iso];
    if (!name) { return null; }
    const source = fs.readFileSync(path.join(root, 'test/language', `${name}-transforms.test.js`), 'utf8');
    const match = /\nconst tests = (\[[\s\S]*?\n\]);/.exec(source);
    if (!match) { throw new Error(`no tests array in ${name}-transforms.test.js`); }
    const groups = Function(`return ${match[1]};`)();
    const cases = [];
    for (const {valid, tests} of groups) {
        for (const {term, source: text, rule, reasons} of tests) {
            cases.push([valid ? '1' : '0', text, term, rule ?? '', reasons === null || reasons === undefined ? '*' : reasons.join(',')].map(field).join('\t'));
        }
    }
    return cases;
}
const {languageDescriptorMap} = await import(pathToFileURL(path.join(root, 'ext/js/language/language-descriptors.js')).href);

const probe = (n, base) => Array.from({length: n}, (_, i) => String.fromCharCode(base + i)).join('');

/** cut letters off the start and end and add others, as most rules do; null when the function does something else. */
function sliceOf(fn) {
    const t1 = probe(60, 0xE000);
    let o1;
    try { o1 = fn(t1); } catch { return null; }
    if (typeof o1 !== 'string') { return null; }
    let a = -1;
    let start = -1;
    for (let i = 0; i < t1.length; i++) {
        const j = o1.indexOf(t1[i]);
        if (j >= 0) { a = i; start = j; break; }
    }
    if (a < 0) { return null; }
    let k = 0;
    while (start + k < o1.length && a + k < t1.length && o1[start + k] === t1[a + k]) { k++; }
    const b = t1.length - a - k;
    const addStart = o1.slice(0, start);
    const addEnd = o1.slice(start + k);
    if (/[-]/.test(addStart + addEnd)) { return null; }
    for (const n of [45, 73]) {
        const t = probe(n, 0xE100);
        if (fn(t) !== addStart + t.slice(a, t.length - b) + addEnd) { return null; }
    }
    return {cutStart: a, cutEnd: b, addStart, addEnd};
}

/** The String.replace and normalize calls the function makes on [input], in order. */
function recordCalls(fn, input) {
    const replace = String.prototype.replace;
    const normalize = String.prototype.normalize;
    const calls = [];
    String.prototype.replace = function (pattern, replacement) {
        calls.push({kind: 'replace', pattern, replacement});
        return replace.call(this, pattern, replacement);
    };
    String.prototype.normalize = function (form) {
        calls.push({kind: 'normalize', form});
        return normalize.call(this, form);
    };
    try { fn(input); } finally {
        String.prototype.replace = replace;
        String.prototype.normalize = normalize;
    }
    return calls;
}

function stepsOf(calls, context) {
    return calls.map((call) => {
        if (call.kind === 'normalize') { return ['#' + call.form.toLowerCase(), '', '']; }
        if (!(call.pattern instanceof RegExp)) { throw new Error(`${context}: replace with a string pattern`); }
        let replacement = call.replacement;
        if (typeof replacement === 'function') {
            const source = replacement.toString().replace(/\s+/g, ' ');
            // Spanish reflexive verbs: "(_match, _pronoun, verb, ending) => `${verb}${ending}se`".
            if (/^\(_match, _pronoun, verb, ending\) => `\$\{verb\}\$\{ending\}se`$/.test(source)) {
                replacement = '$2$3se';
            } else {
                throw new Error(`${context}: unknown replacement function ${source}`);
            }
        }
        return [call.pattern.source, call.pattern.flags, replacement];
    });
}

function opsOf(rule, context) {
    const source = rule.deinflect.toString().replace(/\s+/g, ' ');
    if (source.includes('mutation.new')) {
        // Yiddish: the plural changes the stem's last vowel ("ט" in a closure the probe cannot see).
        if (!rule.yiddish) { throw new Error(`${context}: unrecognised umlaut rule`); }
        return [{guard: '', op: ['y', rule.yiddish.orig, rule.yiddish.new, String(rule.yiddish.cut), rule.yiddish.add]}];
    }
    if (!source.includes('.replace(') && !source.includes('.normalize(')) {
        const slice = sliceOf(rule.deinflect);
        if (slice) { return [{guard: '', op: ['s', String(slice.cutStart), String(slice.cutEnd), slice.addStart, slice.addEnd]}]; }
        const a = rule.deinflect(probe(5, 0xE000));
        const b = rule.deinflect(probe(9, 0xE100));
        if (typeof a === 'string' && a === b) { return [{guard: '', op: ['c', a]}]; }
        throw new Error(`${context}: cannot express ${source.slice(0, 120)}`);
    }
    const branch = /if \(term\.startsWith\('([^']+)'\)\)/.exec(source);
    if (branch) {
        const prefix = branch[1];
        const taken = stepsOf(recordCalls(rule.deinflect, prefix + ''), context);
        const other = stepsOf(recordCalls(rule.deinflect, ''), context);
        return [
            {guard: '+' + prefix, op: ['r', ...taken.flat()]},
            {guard: '-' + prefix, op: ['r', ...other.flat()]},
        ];
    }
    return [{guard: '', op: ['r', ...stepsOf(recordCalls(rule.deinflect, ''), context).flat()]}];
}

function field(value) {
    if (/[\t\n]/.test(value)) { throw new Error(`tab or newline in ${JSON.stringify(value)}`); }
    return value;
}

function kotlinString(text) {
    return '"' + text.replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/\$/g, '\\$').replace(/\n/g, '\\n').replace(/\t/g, '\\t') + '"';
}

/** Yiddish umlaut rules keep their vowels in closures; read them back from the module's own tables. */
function attachYiddishMutations(descriptor) {
    const source = fs.readFileSync(path.join(root, 'ext/js/language/yi/yiddish-transforms.js'), 'utf8');
    const mutations = /const mutations = (\[[\s\S]*?\]);/.exec(source);
    if (!mutations) { throw new Error('Yiddish mutations table not found'); }
    const table = Function(`return ${mutations[1]};`)();
    for (const transform of Object.values(descriptor.transforms)) {
        for (const rule of transform.rules) {
            const text = rule.deinflect.toString();
            if (!text.includes('mutation.new')) { continue; }
            // Find the mutation and suffixes by trying each on a word made to match the rule.
            const hit = findUmlaut(rule, table);
            if (!hit) { throw new Error('Yiddish umlaut rule not resolved: ' + rule.isInflected.source); }
            rule.yiddish = hit;
        }
    }
}

function findUmlaut(rule, mutations) {
    const suffix = /^(.*)\$$/.exec(rule.isInflected.source)?.[1];
    if (suffix === undefined) { return null; }
    for (const mutation of mutations) {
        const word = 'ב' + mutation.new + 'ק' + suffix;
        if (!rule.isInflected.test(word)) { continue; }
        const out = rule.deinflect(word);
        if (out === '') { continue; }
        const expectedStem = 'ב' + mutation.orig + 'ק';
        if (!out.startsWith(expectedStem)) { continue; }
        return {orig: mutation.orig, new: mutation.new, cut: suffix.length, add: out.slice(expectedStem.length)};
    }
    return null;
}

const languages = [];
const ruleFiles = [];
for (const [iso, descriptor] of languageDescriptorMap) {
    const pre = Object.keys(descriptor.textPreprocessors ?? {});
    const post = Object.keys(descriptor.textPostprocessors ?? {});
    const transforms = descriptor.languageTransforms;
    let rulesName = null;
    if (transforms && iso !== 'arz') {
        if (iso === 'yi') { attachYiddishMutations(transforms); }
        const lines = [];
        for (const [name, condition] of Object.entries(transforms.conditions)) {
            lines.push(['C', name, condition.isDictionaryForm ? '1' : '0', (condition.subConditions ?? []).join(',')].map(field).join('\t'));
        }
        let count = 0;
        for (const [id, transform] of Object.entries(transforms.transforms)) {
            lines.push(['T', id, transform.name ?? id, (transform.description ?? '').replace(/\s+/g, ' ')].map(field).join('\t'));
            transform.rules.forEach((rule, i) => {
                for (const {guard, op} of opsOf(rule, `${iso} ${id}[${i}]`)) {
                    lines.push(['R', rule.isInflected.source, rule.isInflected.flags, guard, rule.conditionsIn.join(','), rule.conditionsOut.join(','), ...op].map(field).join('\t'));
                    count++;
                }
            });
        }
        rulesName = `${iso.replace(/[^a-z]/g, '')}Transforms`;
        const chunks = [];
        let current = '';
        for (const line of lines) {
            if (current.length + line.length > 12000 && current) { chunks.push(current); current = ''; }
            current += line + '\n';
        }
        if (current) { chunks.push(current); }
        const file = path.join(OUT, 'rules', `${iso[0].toUpperCase()}${iso.slice(1)}Transforms.kt`);
        const body = [
            'package com.tayra.languages.core.domain.morphology.rules',
            '',
            `// Generated by tools/export_yomitan_rules.mjs from Yomitan's ${descriptor.name} transforms`,
            '// (GPL-3.0, Copyright (C) 2023-2026 Yomitan Authors). Do not edit by hand.',
            `// ${Object.keys(transforms.transforms).length} transforms, ${count} rules.`,
            '@Suppress("MaxLineLength")',
            `internal val ${rulesName}: List<String> = listOf(`,
            ...chunks.map((chunk) => `    ${kotlinString(chunk)},`),
            ')',
            '',
        ].join('\n');
        fs.mkdirSync(path.dirname(file), {recursive: true});
        fs.writeFileSync(file, body);
        ruleFiles.push(file);
        const cases = testCases(iso);
        if (cases) {
            const caseFile = path.join(TEST_OUT, `${iso[0].toUpperCase()}${iso.slice(1)}TransformCases.kt`);
            fs.mkdirSync(TEST_OUT, {recursive: true});
            fs.writeFileSync(caseFile, [
                'package com.tayra.languages.core.domain.morphology.rules',
                '',
                `// Generated by tools/export_yomitan_rules.mjs from Yomitan's test/language/${TEST_FILES[iso]}-transforms.test.js`,
                '// (GPL-3.0, Copyright (C) 2023-2026 Yomitan Authors). Do not edit by hand.',
                '/** Valid (1) or not (0), the inflected text, the dictionary word, its condition, the transforms (or * for any). */',
                '@Suppress("MaxLineLength")',
                `internal val ${iso.replace(/[^a-z]/g, '')}TransformCases: List<String> = listOf(`,
                ...cases.map((c) => `    ${kotlinString(c)},`),
                ')',
                '',
            ].join('\n'));
            console.error(`${iso}: ${cases.length} test cases -> ${caseFile}`);
        }
        console.error(`${iso}: ${count} rules -> ${file}`);
    }
    languages.push({iso, name: descriptor.name, pre, post});
}

const descriptors = [
    'package com.tayra.languages.core.domain.morphology',
    '',
    '// Generated by tools/export_yomitan_rules.mjs from Yomitan\'s language-descriptors.js',
    '// (GPL-3.0, Copyright (C) 2023-2026 Yomitan Authors). Do not edit by hand.',
    '',
    '/** The languages Yomitan describes, with the ids of the text processors it runs on a word before and after deinflecting it. */',
    'internal val yomitanLanguages: List<YomitanLanguage> = listOf(',
    ...languages.map((l) => `    YomitanLanguage(${kotlinString(l.iso)}, ${kotlinString(l.name)}, listOf(${l.pre.map(kotlinString).join(', ')}), listOf(${l.post.map(kotlinString).join(', ')})),`),
    ')',
    '',
].join('\n');
fs.writeFileSync(path.join(OUT, 'YomitanLanguages.kt'), descriptors);
console.error(`wrote ${languages.length} languages, ${ruleFiles.length} rule files`);
