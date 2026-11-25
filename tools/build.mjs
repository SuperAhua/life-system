#!/usr/bin/env node
/**
 * 构建：把 README.md 注入应用，生成 Pages 入口和 Capacitor 的 www 目录。
 *
 *   node tools/build.mjs
 *
 * 做四件事：
 *   1. README.md → 生命系统.html 里 <script type="text/plain" id="docSrc">（设置里的「使用说明」）
 *   2. 生命系统.html → index.html（Pages 首页必须是 index.html，内容完全一致）
 *   3. 生命系统.html + 图标 + manifest → www/（Capacitor 打包 Android 用的 web 目录）
 *   4. 打印体积与章节数，方便核对
 */
import { readFileSync, writeFileSync, statSync, mkdirSync, copyFileSync, rmSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const htmlPath = join(root, '生命系统.html');
const mdPath = join(root, 'README.md');
const indexPath = join(root, 'index.html');
const wwwDir = join(root, 'www');

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

/* ---- www/：Capacitor 的 webDir ---- */
rmSync(wwwDir, { recursive: true, force: true });
mkdirSync(wwwDir, { recursive: true });
for (const f of ['index.html', 'manifest.webmanifest', 'icon-180.png', 'icon-192.png', 'icon-512.png']) {
  copyFileSync(join(root, f), join(wwwDir, f));
}
console.log(`打包：${['index.html', 'manifest.webmanifest', 'icon-*.png'].join(' / ')} → www/`);
console.log('部署：commit & push 后 GitHub Pages 自动重建（约 30 秒）；打 App 跑 npm run sync');
