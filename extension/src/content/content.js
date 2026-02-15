import { hashUrl } from "../api.js";
import { webext } from "../lib/browser.js";


function onHookLoad() {
  console.log("Hook loaded")
  webext.runtime.onMessage.addListener(onServerData);
}

function onServerData(data, _sender, _sendResponse) {
  console.log("Got server data")

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', createElements.bind(data.posts));
  } else {
    createElements(data.posts);
  }
}


let crowdtruthHeader = null;


function createElements(posts) {
  console.log(posts)
 
  const highestReputationPost = posts.reduce((last, current) => last && last.reputation > current.reputation ? last : current);
  
  const crowdtruthTemplate = document.createElement("template");
  crowdtruthTemplate.id = "crowdtruth-template"
  crowdtruthTemplate.innerHTML = `
    <style>
      #crowdtruth-banner {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        min-width: 100lvw;
        // height: 5lvh;
        background: ${getBadgeColor(highestReputationPost.reputation)};
        display: flex;
        // justify-content: center;
        align-items: center;
        flex-direction: column;
        z-index: 9999999 !important;

        .crowdtruth-post {
          border: 2px solid black;
          border-radius: 12px;
          padding: 5rem;
          // display: none;
        }
      }
    </style>
    <div id="crowdtruth-banner">
      <div class="crowdtruth-message">
        <span>${getWarningString(highestReputationPost.reputation)}</span>
        ${highestReputationPost.reputation <= 1 ? `
            <button id="crowdtruth-showpopup">Click here to view.</button>
          ` : ""}
      </div>
      <div class="crowdtruth-post">
        <p>${highestReputationPost.text}</p>
      </div>
    </div>
  `;

  crowdtruthHeader = document.importNode(crowdtruthTemplate.content, true);

  if (highestReputationPost.reputation <= 1) {
    crowdtruthHeader.getElementById("crowdtruth-showpopup").onclick = () => {webext.runtime.sendMessage({action: "openPopup"})}
  }
  
  document.body.appendChild(crowdtruthHeader)

}









function getBadgeColor(reputation) {
  if (reputation >= 4.0) return '#28a745';
  if (reputation >= 3.0) return '#ffc107';
  if (reputation >= 2.0) return '#fd7e14';
  if (reputation >= 1.0) return '#dc3545';
  return '#6c757d';
}

function getWarningString(reputation) {
  if (reputation >= 4.0) return "CrowdTruth has found serious misinformation on this page."
  if (reputation >= 3.0) return "CrowdTruth has found misinformation on this page."
  if (reputation >= 2.0) return "CrowdTruth may have found misinformation on this page."
  if (reputation >= 1.0) return "CrowdTruth believes there might be misinformation on this page."
  return "Users have left fact checks on CrowdTruth."
}


export { onHookLoad };

