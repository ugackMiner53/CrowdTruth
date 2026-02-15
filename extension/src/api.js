const SERVER_ADDRESS = "http://localhost:8080"

// Should just return a Post[]
async function getPostsFromServer(hashUrl) {
  const postsRequest = await fetch(`${SERVER_ADDRESS}/posts?url=${hashUrl}`);
  if (!postsRequest.ok) {
    throw new Error(`Response status was: ${response.status}`);
  }

  const posts = await postsRequest.json();
  return posts;
}


// This is where I wish we were just using typescript for everything
// Should return an object in the form of
// {
//   posts: Post[],
//   reputation: int,
//   
// }
async function getServerPageInfo(hashUrl) {
  const pageRequest = await fetch(`${SERVER_ADDRESS}/info?url=${hashUrl}`);
  if (!pageRequest.ok) {
    throw new Error(`Response status was: ${response.status}`);
  }

  return await pageRequest.json();
}

// Takes in a host and path and SHA-256 hashes them,
// converting to a base64 encoded url along the way
async function hashUrl(host, path) {
    const currentAddress = location.host + location.pathname;
    const addressBuffer = new TextEncoder().encode(currentAddress);
    const hashBuffer = await crypto.subtle.digest("SHA-256", addressBuffer);
    const hashedUrl = encodeURIComponent(new Uint8Array(hashBuffer).toBase64());
    return hashedUrl;
}

export { getPostsFromServer, hashUrl }
