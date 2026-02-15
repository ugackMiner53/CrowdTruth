import { webext } from "./lib/browser.js";
import { getPostsFromServer, hashUrl } from "./api.js";

// Stores tabId -> reputation number for fast icon switching
// const tabIdReputation = new Map();

// chrome.runtime.onInstalled.addListener(() => {
//   console.log("CrowdTruth extension installed");
  
//   chrome.storage.local.get(['authToken'], (result) => {
//     if (!result.authToken) {
//       console.log("No saved auth token");
//     } else {
//       console.log("Auth token found");
//     }
//   });
// });

// chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
//   if (request.action === 'openPopup') {
//     chrome.action.openPopup();
//   }
  
//   if (request.action === 'getAuthToken') {
//     chrome.storage.local.get(['authToken', 'userId'], (result) => {
//       sendResponse(result);
//     });
//     return true;
//   }
  
//   if (request.action === 'setAuthToken') {
//     chrome.storage.local.set({ authToken: request.token, userId: request.userId }, () => {
//       sendResponse({ ok: true });
//     });
//     return true;
//   }
  
//   if (request.action === 'clearAuthToken') {
//     chrome.storage.local.remove(['authToken', 'userId'], () => {
//       sendResponse({ ok: true });
//     });
//     return true;
//   }
// });

// webext.tabs.onActivated.addListener(async (activeInfo) => {
  // Future: Update badge based on current tab reputation

  // console.log("Updating icon!")
  // webext.browserAction.setIcon({
  //   path: "img/icon-green.svg"
  // });
// });

webext.tabs.onUpdated.addListener(async (tabId, changeInfo, tabInfo) => {
  console.log(`Tab ${changeInfo.url} is new!`)

  changeInfo.url ??= tabInfo.url;

  // Check to see if that tab is in the cache
  const cache = await webext.storage.local.get("cache");
  const url = new URL(changeInfo.url);
  const hashedUrl = hashUrl(url.host, url.pathname);

  let posts;
  let reputation;

  if (cache[hashedUrl]) {
    // The cache already has data for this page
    posts = cache[hashedUrl].posts;
    reputation = cache[hashedUrl].reputation;
  } else {
    posts = await getPostsFromServer();
    // const information = await getServerPageInfo(hashedUrl);
    // posts = information.posts;
    // reputation = information.reputation;
  }

  // There's no point injecting anything if we know that
  // there's not any data on the server.
  console.log(`Checked length of posts, got ${posts.length}`)
  if (posts.length > 0) {
    webext.tabs.sendMessage(tabInfo.tabId, { posts: posts });
  }
  
}, {
    properties: [
      "url"
    ]
  })

