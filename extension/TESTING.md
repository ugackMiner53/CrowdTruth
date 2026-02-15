# CrowdTruth Testing Guide

## Setup

### Start the Server
```bash
cd server
java -cp "bin:lib/*" edu.ncsu.hacknc.Main
```
Verify: `curl http://localhost:8080/stats`

### Load Extension
1. Chrome → `chrome://extensions/`
2. Enable "Developer mode"
3. Click "Load unpacked" → Select `extension` folder
4. Accept permissions prompt

## Feature Testing

### Authentication
**Register New User**
1. Click extension icon
2. Enter email + password (8+ chars, mixed case, digit, special char)
3. Click "Register"
4. ✓ "Registration successful! Logging in..."

**Login**
1. Enter credentials
2. Click "Login"  
3. ✓ Main interface appears with current URL

**Logout**
- Click "Logout" → Returns to login screen

### Posting Reviews
1. Navigate to any webpage
2. Open extension
3. Enter title and comment
4. Click "Post Comment"
5. ✓ Post appears in "Vote on Existing Posts"
6. ✓ Page badge updates color/score

### Voting
1. Find existing post
2. Select Agree/Disagree
3. Enter rating (0-5)
4. Click "Vote"
5. ✓ Reputation score recalculates
6. ✓ Badge and banner update

### Visual Features
**Badge**
- Look for colored circle badge (bottom-right of pages)
- Color changes based on reputation:
  - Green: 4.0-5.0 (Excellent)
  - Yellow: 3.0-3.9 (Good)  
  - Orange: 2.0-2.9 (Fair)
  - Red: 1.0-1.9 (Poor)
  - Gray: No data
- Click badge to open popup

**Banner**
- Appears at page top when reputation exists
- Shows: rating, review count, agree/disagree totals
- Click × to dismiss

**Keyboard Shortcut**
- Press `Ctrl+Shift+R` (Mac: `Cmd+Shift+R`)
- ✓ Popup opens immediately

### Advanced Features
**My Post History**
1. Click "View My Posts"
2. ✓ Shows all your reviews with links to sources

**Export Data**
1. Click "Export My Data"
2. ✓ Downloads JSON file with user info

**Import Data**
1. Click "Import Data"
2. Select previously exported JSON
3. ✓ Data loaded successfully

### Anonymous Mode
1. Logout
2. ✓ Can view reputation data
3. ✓ Cannot post or vote
4. Click "Login to Post/Vote" → Returns to login

## API Testing

### Manual Endpoint Tests
```bash
# Global stats
curl http://localhost:8080/stats

# Search posts
curl "http://localhost:8080/search?q=test&type=posts"

# User posts (replace USER_ID)
curl http://localhost:8080/users/USER_ID/posts

# User stats
curl http://localhost:8080/users/USER_ID/stats

# Source reputation (URL-encoded)
curl "http://localhost:8080/sources?url=https://example.com"
```

## Troubleshooting

### Extension Not Loading
- Check manifest.json version field (must be numeric: `0.1.0`)
- Reload extension after code changes
- Check extension console for errors (Inspect popup)

### Server Connection Failed
- Verify server is running on port 8080
- Check firewall settings
- Ensure `host_permissions` includes `http://localhost:8080/*`

### Database Issues
- Delete `server/crowdtruth.db` to reset
- Server will recreate schema on next startup

### Badge Not Appearing
- Check browser console for errors
- Verify content script is running: `console.log` in content.js
- Some sites (chrome://, about:) block content scripts

### Posts/Votes Not Saving
- Open Network tab in DevTools
- Check for 401 errors (auth token expired - re-login)
- Verify JSON payload format

### Password Validation Fails
- Must have: 8+ chars, uppercase, lowercase, digit, special char
- Example valid password: `MyPass123!`

## Test Checklist

- [ ] Register new user
- [ ] Login with credentials
- [ ] Post review on page
- [ ] Vote on another post
- [ ] See badge change color
- [ ] See banner at top of page
- [ ] Use keyboard shortcut
- [ ] View post history
- [ ] Export data
- [ ] Logout and view as anonymous
- [ ] Test on multiple websites

## Firefox Testing

1. Firefox → `about:debugging#/runtime/this-firefox`
2. Click "Load Temporary Add-on"
3. Select `manifest.json`
4. Test all features (should work identically to Chrome)

## Known Limitations

- Content scripts don't run on browser internal pages (chrome://, about:)
- Badge may not appear on some sites with strict CSP policies
- Post history limited to 50 most recent posts (pagination available via API)
- Search is case-insensitive but doesn't support advanced queries
2. Open Firefox and go to `about:debugging#/runtime/this-firefox`
3. Click "Load Temporary Add-on"
4. Select the `manifest.json` file
5. Test all features as described above


## Next Steps (Future Phases)

- [ ] Add visual indicators for page reputation
- [ ] Implement real-time updates
- [ ] Add user profile management
- [ ] Enhance UI/UX with better styling
- [ ] Add export/import features for posts
