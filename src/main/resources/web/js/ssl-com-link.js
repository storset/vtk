/*
 *  SSL communication - lightweight library
 *  by USIT/2012 - Licenced under GPL v3.0 
 */

function SSLComLink() {
  var instance; // cached instance
  VrtxAdmin = function VrtxAdmin() { // rewrite constructor
    return instance;
  };
  VrtxAdmin.prototype = this; // carry over properties
  instance = new VrtxAdmin(); // instance
  instance.constructor = VrtxAdmin; // reset construction pointer

  this._$ = $; // Cache jQuery instance internally
  this.currWin = window;
  this.hasPostMessage = this.currWin['postMessage'] && (!($.browser.opera && $.browser.version < 9.65));
  this.origin = "*";
  this.predefinedCommands = {};
  
  return instance;
};

/* POST BACK */
SSLComLink.prototype.postCmd = function postCmd(c, source) {
  this.postData({cmd: c}, source);
};
SSLComLink.prototype.postCmdAndNum = function postCmdAndNum(c, n, source) {
  this.postData({cmd: c, num: n}, source);
};
SSLComLink.prototype.postData = function postData(data, source) {
  if(this.hasPostMessage) {
    source.postMessage(data, this.origin);
  }
};

/* POST TO PARENT */
SSLComLink.prototype.postCmdToParent = function postCmdToParent(c) {
  this.postDataToParent({cmd: c});
};
SSLComLink.prototype.postCmdAndNumToParent = function postCmdAndNumToParent(c, n) {
  this.postDataToParent({cmd: c, num: n});
};
SSLComLink.prototype.postDataToParent = function postDataToParent(data) {
  if(this.currWin.parent && this.hasPostMessage) {
    this.currWin.parent.postMessage(data, this.origin);
  }
};

/* POST TO IFRAME */
SSLComLink.prototype.postCmdToIframe = function postCmdToParent(iframeElm, c) {
  this.postDataToIframe(iframeElm, {cmd: c});
};
SSLComLink.prototype.postCmdAndNumToIframe = function postCmdAndNumToParent(iframeElm, c, n) {
  this.postDataToIframe(iframeElm, {cmd: c, num: n});
};
SSLComLink.prototype.postDataToIframe = function postDataToIframe(iframeElm, data) {
  if(iframeElm && iframeElm.contentWindow && this.hasPostMessage) {
    iframeElm.contentWindow.postMessage(data, this.origin);
  }
};

SSLComLink.prototype.setUpReceiveDataHandler = function setUpReceiveDataHandler(cmds) {
  var sslCL = this;
  sslCL.predefinedCommands = cmds;
  this._$(this.currWin).on("message", function(e) {
    if(e.originalEvent) e = e.originalEvent;
    var receivedData = e.data;
    var source = e.source;
    if(typeof source === "undefined") source = "";
    if(receivedData && typeof receivedData === "object" && receivedData.cmd && typeof receivedData.cmd === "string") {
      if(receivedData.num) { // Run command on number
        if(!isNaN(receivedData.num)) {
          sslCL.predefinedCommands.cmdNum(receivedData.cmd, receivedData.num, sslCL, source);
        } else if(typeof receivedData.num === "object") {
          sslCL.predefinedCommands.cmdNums(receivedData.cmd, receivedData.num, sslCL, source);
        }
      } else { // Run command
        sslCL.predefinedCommands.cmd(receivedData.cmd, sslCL, source);
      }
    }
  });
};

/* ^ SSL communication - lightweight library */