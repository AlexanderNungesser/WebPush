const CACHE_NAME = "webpushpwa-cache-v1";
const urlsToCache = [
  "/WebPush/sites/index.html",
  "/WebPush/imgs/icon-192.png",
  "/WebPush/imgs/taube_logo.png"
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(urlsToCache);
    })
  );
});

self.addEventListener("fetch", (event) => {
  event.respondWith(
    caches.match(event.request).then((response) => {
      return response || fetch(event.request);
    })
  );
});

self.addEventListener('push', event => {
  console.log('[Service Worker] Push erhalten:', event);

  let data = {};
  try {
    data = event.data ? event.data.json() : {};
  } catch (e) {
    console.error('Fehler beim Lesen der Push-Daten:', e);
  }

  const title = data.title || 'Test Push';
  const body = data.body || 'Dies ist eine Testnachricht.';
  const options = {
    body: body,
    icon: '/WebPush/imgs/taube_logo.png'
  };

  event.waitUntil(self.registration.showNotification(title, options));
});


