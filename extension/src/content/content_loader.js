(async () => {
  browser ??= chrome;
  const url = browser.runtime.getURL("src/content/content.js");
  const content = await import(url);
  content.onHookLoad();
})();
