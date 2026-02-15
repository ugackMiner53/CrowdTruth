const SERVER_ADDRESS = "http://localhost:8080"

const ApiManager = {

  getFromServer() {
    
  },

  // Authenticated send to server
  sendToServer() {
    
  },

  /**
   * Gets the posts from the server given a certain URL hash
   */
  async getPostsFromServer(urlHash) {
    const postsRequest = await fetch(SERVER_ADDRESS + "/posts", {
      method: "GET",
      body: JSON.stringify({url: urlHash})
    });
    if (!postsRequest.ok) {
      throw new Error(`Response status was: ${response.status}`);
    }

    const posts = await postsRequest.json();
    return posts;
  }
  
};




export default ApiManager;



