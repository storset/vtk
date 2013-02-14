/*
 * ToC (Table of Content)
 *
 */

var tocTargetId = ""; /* Possible to override with a target for toc defined typical in document.ready() on a site.
                       * XXX: not global
                       */
window.onload = function () {
  new tocGen('toc');
};

/*
 * This script was originally written By Brady Mulhollem - WebTech101.com
 * It was later modified by Tomm Eriksen and other humble USIT workers
 */
function tocGen(writeTo) {
  this.num = 1;
  this.opened = 0;
  this.writeOut = '';
  this.previous = 0;
  if (document.getElementById) {
    this.parentOb = document.getElementById(writeTo); // current requirements;
    var headers = null;
    if (document.querySelectorAll) { /* >= IE8 */
      if (tocTargetId !== "") {
        headers = document.getElementById(tocTargetId).querySelectorAll('h2,h3');
      } else {
        headers = document.querySelectorAll('h2,h3');
      }
    } else if (typeof (document.compareDocumentPosition) != 'undefined' || typeof (this.parentOb.sourceIndex) != 'undefined') {
      headers = getElementsByTagNames('h2,h3');
    } else {
      headers = getElementsByTagNames('h2');
    }

    if (headers !== null && headers.length > 0) {
      var headerNr = null;
      for (var i = 0, headersLength = headers.length; i < headersLength; i++) {
        headerNr = headers[i].nodeName.substr(1);
        if (headerNr > this.previous) {
          this.writeOut += '<ul>';
          this.opened++;
          this.addLink(headers[i]);
        } else if (headerNr < this.previous) {
          var headerChange = this.previous - headerNr;
          while (headerChange--) {
            this.writeOut += '<\/li><\/ul>';
            this.opened--;
          }
          this.addLink(headers[i]);
        } else {
          this.writeout += '<\/li>';
          this.addLink(headers[i]);
        }
        this.previous = headerNr;
      }
      while (this.opened--) { // close all opened
        this.writeOut += '<\/li><\/ul>';
      }
      document.getElementById(writeTo).innerHTML = this.writeOut;
    }
  }
}

tocGen.prototype.addLink = function (ob) {
  var id = this.getId(ob);
  var link = '<li><a href="#' + id + '">' + ob.innerHTML + '<\/a>';
  this.writeOut += link;
};

tocGen.prototype.getId = function (ob) {
  if (!ob.id) {
    ob.id = 'toc' + this.num;
    this.num++;
  }
  return ob.id;
};

// This function is stolen (legally) from quirksmode.org (and modified by USIT)
// For >= IE7 (can be removed soon)
function getElementsByTagNames(list, obj) {
  if (!obj) obj = document;
  var tagNames = list.split(',');
  var resultArray = [];
  for (var i = 0, tagNamesLength = tagNames.length; i < tagNamesLength; i++) {
	var tags = null;
    if (tocTargetId !== "") {
      tags = obj.getElementById(tocTargetId).getElementsByTagName(tagNames[i]);
    } else {
      tags = obj.getElementsByTagName(tagNames[i]);
    }
    for (var j = 0, tagsLength = tags.length; j < tagsLength; j++) {
      resultArray.push(tags[j]);
    }
  }
  var testNode = resultArray[0];
  if (!testNode) return [];
  if (testNode.sourceIndex) {
    resultArray.sort(function (a, b) {
      return a.sourceIndex - b.sourceIndex;
    });
  } else if (testNode.compareDocumentPosition) {
    resultArray.sort(function (a, b) {
      return 3 - (a.compareDocumentPosition(b) & 6);
    });
  }
  return resultArray;
}

/* ^ ToC (Table of Content) */