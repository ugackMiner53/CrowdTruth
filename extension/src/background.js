const API_BASE = 'http://localhost:8080';

chrome.runtime.onInstalled.addListener(() => {
  console.log("CrowdTruth extension installed");
  
  chrome.storage.local.get(['authToken'], (result) => {
    if (!result.authToken) {
      console.log("No saved auth token");
    } else {
      console.log("Auth token found");
    }
  });
});

chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
  if (request.action === 'fetchReputation') {
    const url = request.url || '';
    fetch(`${API_BASE}/sources?url=${encodeURIComponent(url)}`)
      .then(async (response) => {
        const data = await response.json().catch(() => null);
        sendResponse({ ok: response.ok, status: response.status, data });
      })
      .catch((error) => {
        sendResponse({ ok: false, status: 0, error: String(error) });
      });
    return true;
  }

  if (request.action === 'openPopup') {
    chrome.action.openPopup();
  }
  
  if (request.action === 'getAuthToken') {
    chrome.storage.local.get(['authToken', 'userId'], (result) => {
      sendResponse(result);
    });
    return true;
  }
  
  if (request.action === 'setAuthToken') {
    chrome.storage.local.set({ authToken: request.token, userId: request.userId }, () => {
      sendResponse({ ok: true });
    });
    return true;
  }
  
  if (request.action === 'clearAuthToken') {
    chrome.storage.local.remove(['authToken', 'userId'], () => {
      sendResponse({ ok: true });
    });
    return true;
  }
});

chrome.tabs.onActivated.addListener(async (activeInfo) => {
  // Future: Update badge based on current tab reputation
});
