console.log("CrowdTruth: Content script initialized");

const API_BASE = 'http://localhost:8080';
let badge = null;
let reputationBanner = null;

chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
  if (request.action === 'getCurrentUrl') {
    sendResponse({ url: window.location.href });
  }
  if (request.action === 'refreshReputation') {
    fetchAndUpdateReputation();
  }
  return true;
});

function getBadgeColor(reputation) {
  if (reputation >= 4.0) return '#28a745';
  if (reputation >= 3.0) return '#ffc107';
  if (reputation >= 2.0) return '#fd7e14';
  if (reputation >= 1.0) return '#dc3545';
  return '#6c757d';
}

async function fetchAndUpdateReputation() {
  try {
    const url = window.location.href;
    const response = await fetch(`${API_BASE}/sources?url=${encodeURIComponent(url)}`);
    
    if (response.ok) {
      const data = await response.json();
      updateBadge(data.reputation, data);
      updateReputationBanner(data);
    } else {
      updateBadge(null, null);
      hideReputationBanner();
    }
  } catch (err) {
    console.error('CrowdTruth: Cannot connect to server', err);
    updateBadge(null, null);
  }
}

function updateBadge(reputation, data) {
  if (!badge) return;
  
  const color = getBadgeColor(reputation || 0);
  badge.style.background = color;
  
  if (reputation !== null) {
    badge.textContent = reputation.toFixed(1);
    badge.title = `CrowdTruth Rating: ${reputation.toFixed(1)}/5.0\n${data.postCount} reviews\nClick to view details`;
  } else {
    badge.textContent = 'CT';
    badge.title = 'No ratings yet. Click to be the first!';
  }
}

function injectIndicator() {
  badge = document.createElement('div');
  badge.id = 'crowdtruth-badge';
  badge.textContent = 'CT';
  badge.title = 'Click to rate this page with CrowdTruth';
  badge.style.cssText = `
    position: fixed;
    bottom: 20px;
    right: 20px;
    width: 48px;
    height: 48px;
    background: #6c757d;
    color: white;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: bold;
    font-size: 14px;
    cursor: pointer;
    box-shadow: 0 2px 8px rgba(0,0,0,0.2);
    z-index: 999999;
    transition: all 0.3s;
  `;
  
  badge.addEventListener('mouseenter', () => {
    badge.style.transform = 'scale(1.1)';
  });
  
  badge.addEventListener('mouseleave', () => {
    badge.style.transform = 'scale(1)';
  });
  
  badge.addEventListener('click', () => {
    chrome.runtime.sendMessage({ action: 'openPopup' });
  });
  
  document.body.appendChild(badge);
  fetchAndUpdateReputation();
}

function updateReputationBanner(data) {
  if (reputationBanner) {
    reputationBanner.remove();
  }
  
  reputationBanner = document.createElement('div');
  reputationBanner.id = 'crowdtruth-banner';
  
  const color = getBadgeColor(data.reputation);
  const bgColor = color + '20'; // Add transparency
  
  reputationBanner.style.cssText = `
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    background: ${bgColor};
    border-bottom: 2px solid ${color};
    padding: 10px 20px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    z-index: 999998;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif;
    font-size: 14px;
    color: #333;
    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  `;
  
  reputationBanner.innerHTML = `
    <div style="display: flex; align-items: center; gap: 15px;">
      <strong style="color: ${color};">CrowdTruth Rating: ${data.reputation.toFixed(1)}/5.0</strong>
      <span>${data.postCount} review${data.postCount !== 1 ? 's' : ''}</span>
      <span style="color: #28a745;">↑${data.agreeCount} agree</span>
      <span style="color: #dc3545;">↓${data.disagreeCount} disagree</span>
    </div>
    <button id="crowdtruth-banner-close" style="
      background: transparent;
      border: none;
      font-size: 18px;
      cursor: pointer;
      padding: 0 5px;
      color: #666;
    ">×</button>
  `;
  
  document.body.appendChild(reputationBanner);
  document.body.style.paddingTop = '44px';
  
  document.getElementById('crowdtruth-banner-close').addEventListener('click', () => {
    hideReputationBanner();
  });
}

function hideReputationBanner() {
  if (reputationBanner) {
    reputationBanner.remove();
    reputationBanner = null;
    document.body.style.paddingTop = '';
  }
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', injectIndicator);
} else {
  injectIndicator();
}
