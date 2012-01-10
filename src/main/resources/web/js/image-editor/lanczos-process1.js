// Credits: http://stackoverflow.com/questions/2303690/resizing-an-image-in-an-html5-canvas
self.addEventListener('message', function (e) {
  var data = e.data;
  if (data) {
    lanczos = lanczosCreate(data.lobes);
    var u = 0;
    while(u < data.dest.width) {
      data.center.x = (u + 0.5) * data.ratio;
      data.icenter.x = Math.floor(data.center.x);
      for (var v = 0; v < data.dest.height; v++) {
        data.center.y = (v + 0.5) * data.ratio;
        data.icenter.y = Math.floor(data.center.y);
        var a, r, g, b;
        a = r = g = b = 0;
        for (var i = data.icenter.x - data.range2; i <= data.icenter.x + data.range2; i++) {
          if (i < 0 || i >= data.src.width) continue;
          var f_x = Math.floor(1000 * Math.abs(i - data.center.x));
          if (!data.cacheLanc[f_x]) data.cacheLanc[f_x] = {};
          for (var j = data.icenter.y - data.range2; j <= data.icenter.y + data.range2; j++) {
            if (j < 0 || j >= data.src.height) continue;
            var f_y = Math.floor(1000 * Math.abs(j - data.center.y));
            if (data.cacheLanc[f_x][f_y] == undefined) {
              data.cacheLanc[f_x][f_y] = lanczos(Math.sqrt(Math.pow(f_x * data.rcp_ratio, 2) + Math.pow(f_y * data.rcp_ratio, 2)) / 1000);
            }
            weight = data.cacheLanc[f_x][f_y];
            if (weight > 0) {
              var idx = (j * data.src.width + i) * 4;
              a += weight;
              r += weight * data.src.data[idx];
              g += weight * data.src.data[idx + 1];
              b += weight * data.src.data[idx + 2];
            }
          }
        }
        var idx = (v * data.dest.width + u) * 3;
        data.dest.data[idx] = r / a;
        data.dest.data[idx + 1] = g / a;
        data.dest.data[idx + 2] = b / a;
      }
      u++;
    }
    self.postMessage(data);
  }
}, false);

// Returns a function that calculates lanczos weight
function lanczosCreate(lobes) {
  return function (x) {
    if (x > lobes) return 0;
    x *= Math.PI;
    if (Math.abs(x) < 1e-16) return 1
    var xx = x / lobes;
    return Math.sin(x) * Math.sin(xx) / x / xx;
  }
}
