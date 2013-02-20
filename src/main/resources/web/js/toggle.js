/*
 * Plain JS toggle 
 * XXX: Should probably use jQuery as need document.ready for hiding list and showing toggle link (making the JS unobtrusive)
 */

function toggleCompleted(name, hideLinkText, showLinkText) {
  var elm = document.getElementById("vrtx-" + name);
  var toggleLink = document.getElementById("vrtx-" + name + "-toggle");
  if(elm.style.display == "none") {
    elm.style.display = "block";
    toggleLink.innerHTML = hideLinkText;
  } else {
    elm.style.display = "none";
    toggleLink.innerHTML = showLinkText;
  }
}