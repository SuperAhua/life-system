#!/usr/bin/env node
/**
 * 构建：把 README.md 注入应用，并生成 GitHub Pages 的入口 index.html。
 *
 *   node tools/build.mjs
 *
 * 做三件事：
 *   1. README.md → 生命系统.html 里 <script type="text/plain" id="docSrc">（设置里的「使用说明」）
 *   2. 生命系统.html → index.html（Pages 首页必须是 index.html，内容完全一致）
 *   3. 打印体积与章节数，方便核对
 */
import { readFileSync, writeFileSync, statSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const htmlPath = join(root, '生命系统.html');
const mdPath = join(root, 'README.md');
const indexPath = join(root, 'index.html');

const md = readFileSync(mdPath, 'utf8').replace(/\r\n?/g, '\n');
if (/<\/script/i.test(md)) {
  console.error('README.md 里出现了 </script>，无法安全内嵌，请改写这一处。');
  process.exit(1);
}
if (!/^#\s+\S/.test(md)) {
  console.error('README.md 看起来不是 markdown（缺少一级标题）。');
  process.exit(1);
}

let html = readFileSync(htmlPath, 'utf8');
const re = /(<script type="text\/plain" id="docSrc">)[\s\S]*?(<\/script>)/;
if (!re.test(html)) {
  console.error('生命系统.html 里找不到 docSrc 占位脚本。');
  process.exit(1);
}
html = html.replace(re, (m, open, close) => open + '\n' + md.trim() + '\n' + close);
writeFileSync(htmlPath, html, 'utf8');
writeFileSync(indexPath, html, 'utf8');

const sections = (md.match(/^##\s+/gm) || []).length - 1; // 减去「目录」本身
const kb = n => (n / 1024).toFixed(1) + ' KB';
console.log(`说明：README.md → 生命系统.html（${sections} 章 / ${kb(Buffer.byteLength(md, 'utf8'))}）`);
console.log(`页面：生命系统.html → index.html（${kb(statSync(htmlPath).size)}）`);
console.log('部署：commit & push 后 GitHub Pages 会自动重建（约 30 秒）');
