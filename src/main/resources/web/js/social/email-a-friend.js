var newWindow;
var iMyWidth;
var iMyHeight;

function popup(url) {
  // Half the screen width minus half the new window width (plus 5 pixel borders).
  iMyWidth = (window.screen.width/2) - (165 + 10);

  // Half the screen height minus half the new window height (plus title and status bars).
  iMyHeight = (window.screen.height/2) - (235 + 50);

  // Open the window
  newWindow = window.open(url,"Window2","status=no,height=470,width=330,resizable=no"
                         + ",left=" + iMyWidth
                         + ",top=" + iMyHeight
                         + ",screenX=" + iMyWidth
                         + ",screenY=" + iMyHeight
                         + ",toolbar=no,menubar=no,scrollbars=no,location=no,directories=no");
  newWindow.focus();
}