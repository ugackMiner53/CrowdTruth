import { hashUrl } from "../api.js";
import { webext } from "../lib/browser.js";


function onHookLoad() {
  console.log("Hook loaded")
  webext.runtime.onMessage.addListener(onServerData);
}

function onServerData(posts, _sender, _sendResponse) {
  console.log("Got server data")

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', createElements);
  } else {
    createElements();
  }
}


let crowdtruthHeader = null;


function createElements() {
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
        height: 5lvh;
        background: red;
        display: flex;
        justify-content: center;
        align-items: center;
      }
    </style>
    <div id="crowdtruth-banner">
      <span>CrowdTruth detector alert</span>
    </div>
  `;

  crowdtruthHeader = document.importNode(crowdtruthTemplate.content, true);
  document.body.appendChild(crowdtruthHeader)

}









function getBadgeColor(reputation) {
  if (reputation >= 4.0) return '#28a745';
  if (reputation >= 3.0) return '#ffc107';
  if (reputation >= 2.0) return '#fd7e14';
  if (reputation >= 1.0) return '#dc3545';
  return '#6c757d';
}


export { onHookLoad };

