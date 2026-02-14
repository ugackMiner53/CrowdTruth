import ApiManager from "api.js";

const CACHE_TIME = 24 * 60 * 60 * 1000


async function checkServer() {
  // Get the current url
  const currentAddress = location.host + location.pathname;

  // Hash the address
  const addressBuffer = new TextEncoder().encode(currentAddress);
  const hashBuffer = await crypto.subtle.digest("SHA-256", addressBuffer);
  const hashedUrl = hashBuffer.toBase64();

  // Check with the cache 
  const cache = await storage.session.get(hashedUrl);
  if (cache) {
    console.debug("Found cache!")
    console.debug(cache)

    if (Date.now() - new Date(cache.lastUpdate).getTime() < CACHE_TIME) {
      return cache;
    }
  } else {
    console.debug("No cache found!");
    const posts = await ApiManager.getPostsFromServer(urlHash);
    console.log(posts)
    await chrome.storage.session.set(urlHash, posts);
    return posts;
  }
}

function onPageLoad() {
  checkServer().then((posts) => {
    if (posts.length > 0) {
      // Alert the user
    }
  })
}
