# CrowdTruth

A browser extension and API platform for crowdsourced fact-checking and reputation tracking of online sources.

## Overview

CrowdTruth empowers users to rate and review web pages, creating a distributed reputation system for online information. See aggregated reputation scores directly in your browser while browsing.

## Features

### Core Functionality
- **User Authentication** - Secure registration and login with PBKDF2-HMAC-SHA256 password hashing
- **Source Reputation** - Aggregated ratings (0-5 stars) based on community feedback
- **Review System** - Post detailed reviews with titles and comments
- **Voting Mechanism** - Vote on reviews with agree/disagree and 0-5 star ratings
- **Anonymous Viewing** - Browse reputation data without logging in

### Enhanced Features
- **Dynamic Badge** - Color-coded reputation indicator on every webpage
  - 🟢 Green (4.0-5.0): Excellent
  - 🟡 Yellow (3.0-3.9): Good
  - 🟠 Orange (2.0-2.9): Fair
  - 🔴 Red (1.0-1.9): Poor
  - ⚪ Gray: No data yet
- **Reputation Banner** - Dismissible in-page display of source credibility
- **Keyboard Shortcut** - Quick access with `Ctrl+Shift+R` (Mac: `Cmd+Shift+R`)
- **Post History** - View all your reviews across sources
- **Data Export/Import** - Back up your account information

### Backend API
- **User Management** - Profile, post history, and statistics endpoints
- **Search** - Find posts and sources across the platform
- **Analytics** - Platform-wide statistics and insights
- **Pagination** - Efficient data loading with limit/offset support

## Tech Stack

**Backend:** Java 11+, SQLite, SLF4J  
**Frontend:** Chrome Extension (Manifest V3), Vanilla JavaScript

## Quick Start

### 1. Start the Backend

```bash
cd server
javac -d bin -cp "lib/*" src/module-info.java src/edu/ncsu/hacknc/*.java
java -cp "bin:lib/*" edu.ncsu.hacknc.Main
```

Server runs on `http://localhost:8080`

### 2. Load the Extension

1. Open Chrome → `chrome://extensions/`
2. Enable "Developer mode"
3. Click "Load unpacked"
4. Select the `extension` folder
5. Click "Allow" on the permissions prompt

### 3. Start Using

1. Visit any webpage
2. Click the CrowdTruth icon or press `Ctrl+Shift+R`
3. Register/login
4. Add reviews, vote on posts, see reputation updates

## API Reference

### Authentication ✅ *Integrated*
- `POST /auth/register` - Create account (requires: id, email, password)
- `POST /auth/login` - Get auth token (requires: email, password)

### Sources ✅ *Integrated*
- `GET /sources?url={url}` - Get reputation data for URL
- `POST /sources` - Create new source (requires: url, title) ⚠️ *Auto-created via posts*

### Posts & Votes ✅ *Integrated*
- `POST /posts` - Submit review (requires: url, title, comment, auth)
  - Returns full post data: postId, sourceId, userId, title, comment, createdAt, sourceUrl, sourceTitle
- `POST /votes` - Vote on post (requires: postId, agree, rating, auth)

### User Data ⚠️ *Partially Integrated*
- `GET /users/{id}/posts?limit=50&offset=0` - User's post history ✅ *Used*
- `GET /users/{id}/stats` - User statistics (post count, vote count) 🔮 *Available*
- `GET /users/{id}` - User profile 🔮 *Available*

### Search & Analytics 🔮 *Available for Future Features*
- `GET /search?q={query}&type={posts|sources}&limit=20` - Full-text search across posts and sources
- `GET /stats` - Platform statistics (total users, sources, posts, votes)

**Legend:**
- ✅ *Integrated* - Actively used in the extension UI
- ⚠️ *Partially Integrated* - Some features used, others available
- 🔮 *Available* - Implemented and tested, ready for UI integration

## Password Requirements

- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter  
- At least one digit
- At least one special character (!@#$%, etc.)

## Security

- PBKDF2-HMAC-SHA256 password hashing (120,000 iterations) with random salts
- Token-based authentication with 7-day expiration
- SQL injection protection via PreparedStatements
- Input validation and sanitization on all endpoints
- Request size limiting (1MB max)
- Secure local storage for tokens

## Browser Compatibility

- **Chrome/Edge**: Fully supported ✅
- **Firefox**: Compatible (load as temporary add-on) ✅

## Project Structure

```
CrowdTruth/
├── extension/         # Browser extension
│   ├── manifest.json  # Extension configuration
│   └── src/           # Extension source files
└── server/            # Java backend API
    ├── src/           # Server source files
    └── lib/           # Dependencies (SLF4J)
```

## Testing

See [extension/TESTING.md](extension/TESTING.md) for comprehensive testing guide.

## Development

**Prerequisites:** Java 11+, Chrome browser

**Build:** Manual compilation (no Maven/Gradle required)

**Database:** SQLite (auto-created on first run at `server/crowdtruth.db`)

## License

Educational test project

## Future Ideas

**Backend Ready (Just Needs UI):**
- 🎯 Search interface for finding posts and sources by keyword
- 📊 Statistics dashboard showing platform-wide metrics
- 👤 User profile page with detailed stats

**Planned Features:**
- Real-time updates via WebSockets
- Machine learning for detecting fake news patterns
- Browser action badge text showing reputation
- Moderation and reporting system
- Social features (follow users, trending posts)
