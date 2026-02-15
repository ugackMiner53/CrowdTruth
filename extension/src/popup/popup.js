const API_BASE = 'http://localhost:8080';

// DOM elements
const authSection = document.getElementById('authSection');
const mainSection = document.getElementById('mainSection');
const anonymousSection = document.getElementById('anonymousSection');

// Initialize popup
document.addEventListener('DOMContentLoaded', async () => {
  const token = await getStoredToken();
  const currentUrl = await getCurrentTabUrl();
  
  if (token) {
    showMainSection(currentUrl);
  } else {
    showAnonymousSection(currentUrl);
  }

  setupEventListeners();
});

// Event listeners
function setupEventListeners() {
  // Auth section (always visible initially)
  const loginBtn = document.getElementById('loginBtn');
  const registerBtn = document.getElementById('registerBtn');
  const switchToLoginBtn = document.getElementById('switchToLoginBtn');
  
  if (loginBtn) loginBtn.addEventListener('click', handleLogin);
  if (registerBtn) registerBtn.addEventListener('click', handleRegister);
  if (switchToLoginBtn) {
    switchToLoginBtn.addEventListener('click', () => {
      anonymousSection.style.display = 'none';
      authSection.style.display = 'block';
    });
  }
  
  // Main section elements (hidden initially)
  const logoutBtn = document.getElementById('logoutBtn');
  const submitPostBtn = document.getElementById('submitPostBtn');
  const viewMyPostsBtn = document.getElementById('viewMyPostsBtn');
  const exportDataBtn = document.getElementById('exportDataBtn');
  const importDataBtn = document.getElementById('importDataBtn');
  const importFileInput = document.getElementById('importFileInput');
  
  if (logoutBtn) logoutBtn.addEventListener('click', handleLogout);
  if (submitPostBtn) submitPostBtn.addEventListener('click', handleSubmitPost);
  if (viewMyPostsBtn) viewMyPostsBtn.addEventListener('click', handleViewMyPosts);
  if (exportDataBtn) exportDataBtn.addEventListener('click', handleExportData);
  if (importDataBtn) {
    importDataBtn.addEventListener('click', () => {
      if (importFileInput) importFileInput.click();
    });
  }
  if (importFileInput) importFileInput.addEventListener('change', handleImportData);
}

// Authentication
async function handleRegister() {
  const email = document.getElementById('email').value.trim();
  const password = document.getElementById('password').value;
  const id = generateUserId();

  if (!email || !password) {
    showAuthStatus('Email and password are required', 'error');
    return;
  }

  if (!email.includes('@')) {
    showAuthStatus('Please enter a valid email address', 'error');
    return;
  }

  showAuthStatus('Creating account...', 'info');

  try {
    const response = await fetch(`${API_BASE}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id, email, password })
    });

    const data = await response.json();

    if (data.ok) {
      showAuthStatus('Registration successful! Logging in...', 'success');
      setTimeout(() => handleLogin(), 1000);
    } else {
      const errorMsg = data.error || 'Registration failed';
      showAuthStatus(errorMsg.replace('Invalid Password, ', ''), 'error');
    }
  } catch (err) {
    showAuthStatus('Cannot connect to server. Is it running on port 8080?', 'error');
    console.error('Registration error:', err);
  }
}

async function handleLogin() {
  const email = document.getElementById('email').value.trim();
  const password = document.getElementById('password').value;

  if (!email || !password) {
    showAuthStatus('Email and password are required', 'error');
    return;
  }

  showAuthStatus('Logging in...', 'info');

  try {
    const response = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });

    const data = await response.json();

    if (data.ok && data.token) {
      await chrome.storage.local.set({ authToken: data.token, userId: data.userId });
      showAuthStatus('Login successful!', 'success');
      const currentUrl = await getCurrentTabUrl();
      setTimeout(() => showMainSection(currentUrl), 500);
    } else {
      showAuthStatus(data.error === 'Invalid credentials' ? 'Incorrect email or password' : data.error || 'Login failed', 'error');
    }
  } catch (err) {
    showAuthStatus('Cannot connect to server. Please check if it is running.', 'error');
    console.error('Login error:', err);
  }
}

async function handleLogout() {
  await chrome.storage.local.remove(['authToken', 'userId']);
  const currentUrl = await getCurrentTabUrl();
  showAnonymousSection(currentUrl);
}

// Main section
async function showMainSection(url) {
  authSection.style.display = 'none';
  anonymousSection.style.display = 'none';
  mainSection.style.display = 'block';

  document.getElementById('currentUrl').textContent = url;
  await loadSourceData(url, false);
}

async function showAnonymousSection(url) {
  authSection.style.display = 'none';
  mainSection.style.display = 'none';
  anonymousSection.style.display = 'block';

  document.getElementById('anonUrl').textContent = url;
  await loadSourceData(url, true);
}

// Load source data
async function loadSourceData(url, isAnonymous) {
  try {
    const response = await fetch(`${API_BASE}/sources?url=${encodeURIComponent(url)}`);
    const data = await response.json();

    if (response.ok && data.sourceId) {
      displaySourceInfo(data, isAnonymous);
    } else {
      if (isAnonymous) {
        document.getElementById('anonSourceInfo').innerHTML = '<p class="info">No data for this source yet.</p>';
      } else {
        document.getElementById('sourceInfo').innerHTML = '<p class="info">No data for this source yet. Be the first to post!</p>';
      }
    }
  } catch (err) {
    console.error('Error loading source:', err);
  }
}

function displaySourceInfo(data, isAnonymous) {
  // Helper to get correct ID with optional "anon" prefix
  const getId = (base) => {
    if (isAnonymous) {
      return 'anon' + base.charAt(0).toUpperCase() + base.slice(1);
    }
    return base;
  };
  
  document.getElementById(getId('reputationScore')).textContent = data.reputation.toFixed(1);
  document.getElementById(getId('agreeCount')).textContent = `${data.agreeCount} agree`;
  document.getElementById(getId('disagreeCount')).textContent = `${data.disagreeCount} disagree`;
  document.getElementById(getId('sourceInfo')).style.display = 'block';

  const postsList = document.getElementById(getId('postsList'));
  postsList.innerHTML = '';

  if (data.posts && data.posts.length > 0) {
    data.posts.forEach(post => {
      const postEl = createPostElement(post, isAnonymous);
      postsList.appendChild(postEl);
    });
  } else {
    postsList.innerHTML = '<p class="info">No posts yet.</p>';
  }
}

function createPostElement(post, isAnonymous) {
  const div = document.createElement('div');
  div.className = 'post-item';
  
  const header = document.createElement('div');
  header.className = 'post-header';
  header.textContent = post.title;
  
  const comment = document.createElement('div');
  comment.className = 'post-comment';
  comment.textContent = post.comment;
  
  const meta = document.createElement('div');
  meta.className = 'post-meta';
  meta.textContent = `Rating: ${post.rating.toFixed(1)} | ${post.agreeCount} agree / ${post.disagreeCount} disagree`;
  
  div.appendChild(header);
  div.appendChild(comment);
  div.appendChild(meta);
  
  if (!isAnonymous) {
    const voteControls = createVoteControls(post.postId);
    div.appendChild(voteControls);
  }
  
  return div;
}

function createVoteControls(postId) {
  const div = document.createElement('div');
  div.className = 'vote-controls';
  
  const agreeLabel = document.createElement('label');
  agreeLabel.textContent = 'Agree:';
  
  const agreeSelect = document.createElement('select');
  agreeSelect.innerHTML = '<option value="true">Yes</option><option value="false">No</option>';
  
  const ratingLabel = document.createElement('label');
  ratingLabel.textContent = 'Rating:';
  
  const ratingInput = document.createElement('input');
  ratingInput.type = 'number';
  ratingInput.min = '0';
  ratingInput.max = '5';
  ratingInput.value = '3';
  ratingInput.style.width = '50px';
  
  const voteBtn = document.createElement('button');
  voteBtn.textContent = 'Vote';
  voteBtn.addEventListener('click', () => handleVote(postId, agreeSelect.value === 'true', parseInt(ratingInput.value)));
  
  div.appendChild(agreeLabel);
  div.appendChild(agreeSelect);
  div.appendChild(ratingLabel);
  div.appendChild(ratingInput);
  div.appendChild(voteBtn);
  
  return div;
}

// Post submission
async function handleSubmitPost() {
  const title = document.getElementById('postTitle').value.trim();
  const comment = document.getElementById('postComment').value.trim();
  const url = await getCurrentTabUrl();
  const submitBtn = document.getElementById('submitPostBtn');

  if (!title || !comment) {
    showPostStatus('Both title and comment are required', 'error');
    return;
  }

  if (title.length < 3) {
    showPostStatus('Title must be at least 3 characters', 'error');
    return;
  }

  const token = await getStoredToken();
  if (!token) {
    showPostStatus('Please login to post reviews', 'error');
    return;
  }

  submitBtn.disabled = true;
  submitBtn.textContent = 'Posting...';
  showPostStatus('Submitting your review...', 'info');

  try {
    const response = await fetch(`${API_BASE}/posts`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({ url, title, comment })
    });

    const data = await response.json();

    if (data.ok) {
      showPostStatus('Review posted successfully!', 'success');
      document.getElementById('postTitle').value = '';
      document.getElementById('postComment').value = '';
      await loadSourceData(url, false);
      notifyContentScriptRefresh();
    } else {
      showPostStatus(data.error || 'Failed to post review', 'error');
    }
  } catch (err) {
    showPostStatus('Cannot connect to server', 'error');
    console.error('Post error:', err);
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = 'Post Comment';
  }
}

// Vote submission
async function handleVote(postId, agree, rating) {
  const token = await getStoredToken();
  if (!token) {
    showPostStatus('Please login to vote', 'error');
    return;
  }

  if (rating < 0 || rating > 5) {
    showPostStatus('Rating must be between 0 and 5', 'error');
    return;
  }

  try {
    const response = await fetch(`${API_BASE}/votes`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({ postId, agree, rating })
    });

    const data = await response.json();

    if (data.ok) {
      const url = await getCurrentTabUrl();
      await loadSourceData(url, false);
      notifyContentScriptRefresh();
      showPostStatus('Vote recorded successfully', 'success');
    } else {
      const errorMsg = data.error === 'Already voted or invalid post' 
        ? 'You have already voted on this post' 
        : (data.error || 'Failed to record vote');
      showPostStatus(errorMsg, 'error');
    }
  } catch (err) {
    showPostStatus('Cannot connect to server', 'error');
    console.error('Vote error:', err);
  }
}

// Utilities
async function getCurrentTabUrl() {
  const tabs = await chrome.tabs.query({ active: true, currentWindow: true });
  return tabs[0]?.url || 'about:blank';
}

async function getStoredToken() {
  const result = await chrome.storage.local.get('authToken');
  return result.authToken || null;
}

function generateUserId() {
  return 'user_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
}

function showAuthStatus(message, type) {
  const status = document.getElementById('authStatus');
  status.textContent = message;
  status.className = `status ${type}`;
}

function showPostStatus(message, type) {
  const status = document.getElementById('postStatus');
  status.textContent = message;
  status.className = `status ${type}`;
  setTimeout(() => status.textContent = '', 3000);
}

// Notify content script to refresh badge/banner
async function notifyContentScriptRefresh() {
  const tabs = await chrome.tabs.query({ active: true, currentWindow: true });
  if (tabs[0]) {
    chrome.tabs.sendMessage(tabs[0].id, { action: 'refreshReputation' }, () => {
      if (chrome.runtime.lastError) {
        console.debug('CrowdTruth: No content script on this tab');
      }
    });
  }
}

// View user's post history
async function handleViewMyPosts() {
  const myPostsList = document.getElementById('myPostsList');
  const btn = document.getElementById('viewMyPostsBtn');
  
  // Toggle visibility
  if (myPostsList.style.display === 'block') {
    myPostsList.style.display = 'none';
    btn.textContent = 'View My Posts';
    return;
  }
  
  btn.textContent = 'Loading...';
  
  const userId = (await chrome.storage.local.get('userId')).userId;
  if (!userId) {
    myPostsList.innerHTML = '<p class="info">Not logged in</p>';
    myPostsList.style.display = 'block';
    btn.textContent = 'View My Posts';
    return;
  }
  
  try {
    const response = await fetch(`${API_BASE}/users/${userId}/posts?limit=50`);
    const data = await response.json();
    
    if (!data.ok || !data.posts || data.posts.length === 0) {
      myPostsList.innerHTML = '<p class="info">You haven\'t posted any reviews yet.</p>';
      myPostsList.style.display = 'block';
      btn.textContent = 'Hide My Posts';
      return;
    }
    
    // Display posts
    myPostsList.innerHTML = '';
    data.posts.forEach(post => {
      const postDiv = document.createElement('div');
      postDiv.className = 'post-item';
      postDiv.innerHTML = `
        <div class="post-header">${post.title}</div>
        <div class="post-comment">${post.comment}</div>
        <div class="post-meta">
          On: <a href="${post.sourceUrl}" target="_blank">${post.sourceTitle || post.sourceUrl}</a><br>
          Rating: ${post.rating.toFixed(1)} | ${post.agreeCount} agree / ${post.disagreeCount} disagree<br>
          Posted: ${new Date(post.createdAt).toLocaleDateString()}
        </div>
      `;
      myPostsList.appendChild(postDiv);
    });
    
    myPostsList.style.display = 'block';
    btn.textContent = 'Hide My Posts';
  } catch (err) {
    myPostsList.innerHTML = '<p class="info">Failed to load post history. Please check server connection.</p>';
    myPostsList.style.display = 'block';
    btn.textContent = 'View My Posts';
    console.error('Post history error:', err);
  }
}

// Export user data
async function handleExportData() {
  const result = await chrome.storage.local.get(['authToken', 'userId']);
  
  if (!result.userId) {
    showDataStatus('No user data to export. Please login first.', 'error');
    return;
  }
  
  const exportData = {
    version: '1.0',
    exportDate: new Date().toISOString(),
    userId: result.userId,
    note: 'CrowdTruth user data export. Token excluded for security.'
  };
  
  try {
    const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `crowdtruth-${result.userId}-${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);
    
    showDataStatus('Data exported successfully!', 'success');
  } catch (err) {
    showDataStatus('Failed to export data', 'error');
    console.error('Export error:', err);
  }
}

// Import user data
async function handleImportData(event) {
  const file = event.target.files[0];
  if (!file) return;
  
  if (!file.name.endsWith('.json')) {
    showDataStatus('Please select a JSON file', 'error');
    event.target.value = '';
    return;
  }
  
  const reader = new FileReader();
  reader.onload = async (e) => {
    try {
      const importData = JSON.parse(e.target.result);
      
      if (!importData.userId || !importData.version) {
        showDataStatus('Invalid CrowdTruth export file', 'error');
        return;
      }
      
      showDataStatus(`Import successful! User ID: ${importData.userId}`, 'success');
    } catch (err) {
      showDataStatus('Failed to parse file. Please check the format.', 'error');
      console.error('Import error:', err);
    }
  };
  
  reader.onerror = () => {
    showDataStatus('Failed to read file', 'error');
  };
  
  reader.readAsText(file);
  event.target.value = '';
}

function showDataStatus(message, type) {
  const status = document.getElementById('dataStatus');
  status.textContent = message;
  status.className = `status ${type}`;
  setTimeout(() => status.textContent = '', 3000);
}
