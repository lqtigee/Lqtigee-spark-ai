const STATIC_CACHE_NAME = "lqtigee-static-v5";
const STATIC_SHELL_URLS = ["/", "/sessions", "/manifest.webmanifest", "/icons/icon-192.png", "/icons/icon-512.png"];

self.addEventListener("install", (event) => {
  event.waitUntil(caches.open(STATIC_CACHE_NAME).then((cache) => cache.addAll(STATIC_SHELL_URLS)));
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((cacheNames) =>
        Promise.all(
          cacheNames
            .filter((cacheName) => cacheName !== STATIC_CACHE_NAME)
            .map((cacheName) => caches.delete(cacheName))
        )
      )
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (event) => {
  const url = new URL(event.request.url);

  if (url.pathname.startsWith("/api/")) {
    event.respondWith(fetch(event.request));
    return;
  }

  if (url.pathname.startsWith("/downloads/")) {
    event.respondWith(fetch(event.request));
    return;
  }

  if (event.request.mode === "navigate") {
    event.respondWith(
      fetch(event.request)
        .then((response) => {
          const responseCopy = response.clone();
          event.waitUntil(caches.open(STATIC_CACHE_NAME).then((cache) => cache.put("/", responseCopy)));
          return response;
        })
        .catch(() => caches.match(event.request).then((cachedResponse) => cachedResponse ?? caches.match("/")))
    );
    return;
  }

  if (event.request.method !== "GET") {
    event.respondWith(fetch(event.request));
    return;
  }

  event.respondWith(
    caches.match(event.request).then((cachedResponse) => {
      if (cachedResponse) {
        return cachedResponse;
      }
      return fetch(event.request).then((response) => {
        if (response.ok) {
          const responseCopy = response.clone();
          event.waitUntil(caches.open(STATIC_CACHE_NAME).then((cache) => cache.put(event.request, responseCopy)));
        }
        return response;
      });
    })
  );
});
