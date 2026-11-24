/* 只为「手机上打开过一次，之后没网也能开」。
   策略：导航请求走网络优先，失败时读缓存 —— 不会把旧版本锁死。 */
const CACHE = 'life-system-v1';
const ENTRY = './index.html';

self.addEventListener('install', e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll([ENTRY, './manifest.webmanifest'])).catch(() => {}));
  self.skipWaiting();
});

self.addEventListener('activate', e => {
  e.waitUntil(
    caches.keys()
      .then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', e => {
  if (e.request.mode !== 'navigate') return;
  e.respondWith(
    fetch(e.request)
      .then(res => {
        const copy = res.clone();
        caches.open(CACHE).then(c => c.put(ENTRY, copy)).catch(() => {});
        return res;
      })
      .catch(() => caches.match(ENTRY).then(r => r || caches.match(e.request)))
  );
});
