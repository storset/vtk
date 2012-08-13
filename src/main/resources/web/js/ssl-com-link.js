/*
 *  SSL communication - lightweight library
 *
 */

function SSLComLink(winObj) {

  // Class-like singleton pattern (p.145 JavaScript Patterns)
  var instance; // cached instance
  VrtxAdmin = function VrtxAdmin() { // rewrite constructor
    return instance;
  };
  VrtxAdmin.prototype = this; // carry over properties
  instance = new VrtxAdmin(); // instance
  instance.constructor = VrtxAdmin; // reset construction pointer
  //--

  // Cache jQuery instance internally
  this._$ = $;
  
  this.winObj = winObj;
  this.hasPostMessage = this.winObj['postMessage'] && (!($.browser.opera && $.browser.version < 9.65));
  this.vrtxAdminOrigin = "*"; // TODO: TEMP Need real origin of adm
  
  return instance;
};

var SSLComLink = new SSLComLink(window);

SSLComLink.prototype.postDataToParent = function postDataToParent(data) {
  if(this.winObj.parent && this.hasPostMessage) {
    this.winObj.parent.postMessage(data, this.vrtxAdminOrigin);
  }
};

SSLComLink.prototype.postDataToChild = function postDataToChild(iframeElm, data) {
  if(this.hasPostMessage) {
    iframeElm.contentWindow.postMessage(data, this.vrtxAdminOrigin);
  }
};

SSLComLink.prototype.setUpReceiveDataHandler = function setUpReceiveDataHandler(callback) {
  $(this.winObj).on("message", function(e) {
    if(e.originalEvent) e = e.originalEvent;
    var receivedData = e.data; 
    if(receivedData) {
      callback(receivedData);
    }
  });
};

/* ^ SSL communication - lightweight library */