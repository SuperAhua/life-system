#!/usr/bin/env node
/**
 * 把 README.md 同步进 生命系统.html 里的 <script type="text/plain" id="docSrc">。
 * 说明页是运行时用 js 里的迷你 markdown 渲染器转成 HTML 的，
 * 所以只要 README 保持纯 markdown（不含 </script>），同步即可。
 *
 *   node tools/sync-doc.mjs
 */
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const htmlPath = join(root, '生命系统.html');
const mdPath = join(root, 'README.md');

const md = readFileSync(mdPath, 'utf8').replace(/\r\n?/g, '\n');
if (/<\/script/i.test(md)) {
  console.error('README.md 里出现了 </script>，无法安全内嵌，请改写这一处。');
  process.exit(1);
}
if (!/^#\s+\S/.test(md)) {
  console.error('README.md 看起来不是 markdown（缺少一级标题）。');
  process.exit(1);
}

const html = readFileSync(htmlPath, 'utf8');
const re = /(<script type="text\/plain" id="docSrc">)[\s\S]*?(<\/script>)/;
if (!re.test(html)) {
  console.error('生命系统.html 里找不到 docSrc 占位脚本。');
  process.exit(1);
}

const next = html.replace(re, (m, open, close) => open + '\n' + md.trim() + '\n' + close);
writeFileSync(htmlPath, next, 'utf8');

const sections = (md.match(/^##\s+/gm) || []).length;
const kb = (Buffer.byteLength(md, 'utf8') / 1024).toFixed(1);
console.log(`已同步 README.md → 生命系统.html（${sections} 章 / ${kb} KB）`);
