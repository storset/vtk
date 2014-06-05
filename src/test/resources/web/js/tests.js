var utils = null;
var i18n = null;
var session = null;

module("Schedule.js", {
  setup: function() {
    utils = new scheduleUtils();
    i18n = {
      "d0": "Søndag",
      "d1": "Mandag",
      "d2": "Tirsdag",
      "d3": "Onsdag",
      "d4": "Torsdag",
      "d5": "Fredag",
      "d6": "Lørdag",
      
      "m1": "Jan",
      "m2": "Feb",
      "m3": "Mar",
      "m4": "Apr",
      "m5": "Mai",
      "m6": "Jun",
      "m7": "Jul",
      "m8": "Aug",
      "m9": "Sep",
      "m10": "Okt",
      "m11": "Nov",
      "m12": "Des"
    };
    session = {
      /* TP */
      "id": "14H-EXPHIL03-1-1-2-12-1/34",
      "dtStart": "2014-08-21T16:30:00.000+02:00",
      "dtEnd": "2014-08-21T18:15:00.000+02:00",
      "weekNr": 34,
      "status": "active",
      "title": "Seminargruppe 12 (MED)",
      "rooms": [{
        "buildingId": "BL16",
        "buildingName": "Georg Morgenstiernes hus",
        "buildingAcronym": "GM",
        "roomId": "205",
        "roomName": "Seminarrom 205",
        "roomUrl": "http://www.med.uio.no/om/finn-fram/kart/vis/#bl1602,300,253"
      }],
      /* Enriched */
      "vrtxTitle": "Åpningsforelesning",
      "vrtxStaff": [
        { "id": "rezam" },
        { "id": "oyvihatl" }
      ],
      "vrtxStaffExternal": [
        { "name": "Gunnar Flaksnes", "url": "http://www.nrk.no/" },
        { "name": "Roger  Rabbit", "url": "http://www.aftenposten.no/" },
        { "name": "Bob Kåre Funken-Hagen", "url": "http://www.dagbladet.no/" }
      ],
      "vrtxResources": [
        { "title": "Pensumlitteratur (PDF)", "url": "http://www.vg.no/" }
      ],
      "vrtxResourcesText": "<ul><li>listepunkt #1</li><li>listepunkt #2</li></ul>"
    };
  }, teardown: function() {
    utils = null;
    i18n = null;
    session = null;
  }
});

test("DateTime parsing", function () {
  /* !DST */

  var datetime = utils.getDateTime("2014-08-18T23:15:00.000+02:00", "2014-08-18T23:59:00.000+02:00");
  var endDay = utils.getEndDateDayFormatted(datetime.start, datetime.end, i18n);
  equal(endDay.day, "Mandag",  "Mandag +0200 - 23:15=>23:59");
  equal(utils.getDateFormatted(datetime.start, datetime.end, i18n), "18. aug.", "Date");
  equal(utils.getTimeFormatted(datetime.start, datetime.end), "23:15&ndash;23:59",  "Time");
  
  var datetime = utils.getDateTime("2014-08-22T23:15:00.000+02:00", "2014-08-22T23:59:00.000+02:00");
  var endDay = utils.getEndDateDayFormatted(datetime.start, datetime.end, i18n);
  equal(endDay.day, "Fredag",  "Fredag +0200 - 23:15=>23:59");
  
  var datetime = utils.getDateTime("2014-08-24T23:15:00.000+02:00", "2014-08-24T23:59:00.000+02:00");
  var endDay = utils.getEndDateDayFormatted(datetime.start, datetime.end, i18n);
  equal(endDay.day, "Søndag",  "Søndag +0200 - 23:15=>23:59");

  var datetime = utils.getDateTime("2014-08-18T00:15:00.000+02:00", "2014-08-18T01:59:00.000+02:00");
  var endDay = utils.getEndDateDayFormatted(datetime.start, datetime.end, i18n);
  equal(endDay.day, "Mandag",  "Mandag +0200 - 00:15=>01:59");
  
  var datetime = utils.getDateTime("2014-08-22T00:15:00.000+02:00", "2014-08-22T01:59:00.000+02:00");
  var endDay = utils.getEndDateDayFormatted(datetime.start, datetime.end, i18n);
  equal(endDay.day, "Fredag",  "Fredag +0200 - 00:15=>01:59");
  
  var datetime = utils.getDateTime("2014-08-24T00:15:00.000+02:00", "2014-08-24T01:59:00.000+02:00");
  var endDay = utils.getEndDateDayFormatted(datetime.start, datetime.end, i18n);
  equal(endDay.day, "Søndag",  "Søndag +0200 - 00:15=>01:59");
  
  /* DST */
  
  var datetime = utils.getDateTime("2014-11-03T23:15:00.000+01:00", "2014-11-03T23:59:00.000+01:00");
  var endDay = utils.getEndDateDayFormatted(datetime.start, datetime.end, i18n);
  equal(endDay.day, "Mandag",  "Mandag +0100 - 23:15=>23:59");
  
  var datetime = utils.getDateTime("2014-11-05T23:15:00.000+01:00", "2014-11-05T23:59:00.000+01:00");
  var endDay = utils.getEndDateDayFormatted(datetime.start, datetime.end, i18n);
  equal(endDay.day, "Onsdag",  "Onsdag +0100 - 23:15=>23:59");
  
  var datetime = utils.getDateTime("2014-11-08T23:15:00.000+01:00", "2014-11-08T23:59:00.000+01:00");
  var endDay = utils.getEndDateDayFormatted(datetime.start, datetime.end, i18n);
  equal(endDay.day, "Lørdag",  "Lørdag +0100 - 23:15=>23:59");

  var datetime = utils.getDateTime("2014-11-03T00:15:00.000+01:00", "2014-11-03T01:59:00.000+01:00");
  var endDay = utils.getEndDateDayFormatted(datetime.start, datetime.end, i18n);
  equal(endDay.day, "Mandag",  "Mandag +0100 - 00:15=>01:59");
  
  var datetime = utils.getDateTime("2014-11-05T00:15:00.000+01:00", "2014-11-05T01:59:00.000+01:00");
  var endDay = utils.getEndDateDayFormatted(datetime.start, datetime.end, i18n);
  equal(endDay.day, "Onsdag",  "Onsdag +0100 - 00:15=>01:59");
  
  var datetime = utils.getDateTime("2014-11-08T00:15:00.000+01:00", "2014-11-08T01:59:00.000+01:00");
  var endDay = utils.getEndDateDayFormatted(datetime.start, datetime.end, i18n);
  equal(endDay.day, "Lørdag",  "Lørdag +0100 - 00:15=>01:59");
});
test("Generating Table HTML", function () {
  equal(utils.getTitle(session), "Åpningsforelesning",
                                 "Title - from Vortex");
  equal(utils.getPlace(session), "<abbr class='place-short' title='Georg Morgenstiernes hus'>GM</abbr><span class='place-long'>Georg Morgenstiernes hus</span> <a class='place-short' title='Seminarrom 205' href='http://www.med.uio.no/om/finn-fram/kart/vis/#bl1602,300,253'>205</a><a class='place-long' href='http://www.med.uio.no/om/finn-fram/kart/vis/#bl1602,300,253'>Seminarrom 205</a>",
                                 "Place - Abbr with title + Link with title");
  equal(utils.getStaff(session), "<ul><li>rezam</li><li>oyvihatl</li><li><a href='http://www.nrk.no/'>G. Flaksnes</a></li><li><a href='http://www.aftenposten.no/'>R. Rabbit</a></li><li><a href='http://www.dagbladet.no/'>B. K. Funken-Hagen</a></li></ul>",
                                 "Staff - List of: Just text or link");
  equal(utils.getResources(session), "<a href='http://www.vg.no/'>Pensumlitteratur (PDF)</a><ul><li>listepunkt #1</li><li>listepunkt #2</li></ul>",
                                 "Resources - List of: Link + freetext");
});
test("Generating ToC HTML", function () {
  var testData = [];
  for(var i = 0, len = 5; i < len; i++) {
    testData.push({ tocHtml: '<li><a href="#sem-1-"' + i + '">Group ' + i + '</a> - thu 12:30–14:15</li>' });
  }
  equal(utils.splitThirds(testData, "Seminar", testData.length > 30),
                          "<span class='display-as-h3'>Seminar</span><div class='course-schedule-toc-thirds'><ul class='thirds-left'><li><a href=\"#sem-1-\"0\">Group 0</a> - thu 12:30–14:15</li><li><a href=\"#sem-1-\"1\">Group 1</a> - thu 12:30–14:15</li></ul><ul class='thirds-middle'><li><a href=\"#sem-1-\"2\">Group 2</a> - thu 12:30–14:15</li><li><a href=\"#sem-1-\"3\">Group 3</a> - thu 12:30–14:15</li></ul><ul class='thirds-right'><li><a href=\"#sem-1-\"4\">Group 4</a> - thu 12:30–14:15</li></ul></div>",
                          "Split in thirds - Seminar groups");
  var testData = [];
  for(var i = 0, len = 30; i < len; i++) {
    testData.push({ tocHtml: '<li><a href="#sem-1-"' + i + '">Group ' + i + '</a> - thu 12:30–14:15</li>' });
  }
  equal(utils.splitThirds(testData, "Seminar", testData.length > 30),
                          "<span class='display-as-h3'>Seminar</span><div class='course-schedule-toc-thirds'><ul class='thirds-left'><li><a href=\"#sem-1-\"0\">Group 0</a> - thu 12:30–14:15</li><li><a href=\"#sem-1-\"1\">Group 1</a> - thu 12:30–14:15</li><li><a href=\"#sem-1-\"2\">Group 2</a> - thu 12:30–14:15</li><li><a href=\"#sem-1-\"3\">Group 3</a> - thu 12:30–14:15</li><li><a href=\"#sem-1-\"4\">Group 4</a> - thu 12:30–14:15</li><li><a href=\"#sem-1-\"5\">Group 5</a> - thu 12:30–14:15</li><li><a href=\"#sem-1-\"6\">Group 6</a> - thu 12:30–14:15</li><li><a href=\"#sem-1-\"7\">Group 7</a> - thu 12:30–14:15</li><li><a href=\"#sem-1-\"8\">Group 8</a> - thu 12:30–14:15</li><li><a href=\"#sem-1-\"9\">Group 9</a> - thu 12:30–14:15</li></ul><ul class='thirds-middle'><li><a href=\"#sem-1-\"10\">Group 10</a> - thu 12:30–14:15</li><li><a href=\"#sem-1-\"11\">Group 11</a> - thu 12:30–14:15</li><li><a href=\"#sem-1-\"12\">Group 12</a> - thu 12:30–14:15</li><li><a href=\"#sem-1-\"13\">Group 13</a> - thu 12:30–14:15</li><li><a href=\"#sem-1-\"14\">Group 14</a> - thu 12:30–14:15</li><li><a href=\"#sem-1-\"15\">Group 15</a> - thu 12:30–14:15</li><li><a href=\"#sem-1-\"16\">Group 16</a> - thu 12:30–14:15</li><li><a href=\"#sem-1-\"17\">Group 17</a> - thu 12:30–14:15</li><li><a href=\"#sem-1-\"18\">Group 18</a> - thu 12:30–14:15</li><li><a href=\"#sem-1-\"19\">Group 19</a> - thu 12:30–14:15</li></ul><ul class='thirds-right'><li><a href=\"#sem-1-\"20\">Group 20</a> - thu 12:30–14:15</li><li><a href=\"#sem-1-\"21\">Group 21</a> - thu 12:30–14:15</li><li><a href=\"#sem-1-\"22\">Group 22</a> - thu 12:30–14:15</li><li><a href=\"#sem-1-\"23\">Group 23</a> - thu 12:30–14:15</li><li><a href=\"#sem-1-\"24\">Group 24</a> - thu 12:30–14:15</li><li><a href=\"#sem-1-\"25\">Group 25</a> - thu 12:30–14:15</li><li><a href=\"#sem-1-\"26\">Group 26</a> - thu 12:30–14:15</li><li><a href=\"#sem-1-\"27\">Group 27</a> - thu 12:30–14:15</li><li><a href=\"#sem-1-\"28\">Group 28</a> - thu 12:30–14:15</li><li><a href=\"#sem-1-\"29\">Group 29</a> - thu 12:30–14:15</li></ul></div>",
                          "Split in thirds (30) - Seminar groups");
  var testData = [];
  for(var i = 0, len = 31; i < len; i++) {
    testData.push({ tocHtml: '<li><a href="#sem-1-"' + i + '">Group ' + i + '</a> - thu 12:30–14:15</li>' });
  }                    
  equal(utils.splitThirds(testData, "Seminar", testData.length > 30),
                          "<span class='display-as-h3'>Seminar</span><div class='course-schedule-toc-thirds'><ul class='thirds-left'><li><a href=\"#sem-1-\"0\">Group 0</a></li><li><a href=\"#sem-1-\"1\">Group 1</a></li><li><a href=\"#sem-1-\"2\">Group 2</a></li><li><a href=\"#sem-1-\"3\">Group 3</a></li><li><a href=\"#sem-1-\"4\">Group 4</a></li><li><a href=\"#sem-1-\"5\">Group 5</a></li><li><a href=\"#sem-1-\"6\">Group 6</a></li><li><a href=\"#sem-1-\"7\">Group 7</a></li><li><a href=\"#sem-1-\"8\">Group 8</a></li><li><a href=\"#sem-1-\"9\">Group 9</a></li><li><a href=\"#sem-1-\"10\">Group 10</a></li></ul><ul class='thirds-middle'><li><a href=\"#sem-1-\"11\">Group 11</a></li><li><a href=\"#sem-1-\"12\">Group 12</a></li><li><a href=\"#sem-1-\"13\">Group 13</a></li><li><a href=\"#sem-1-\"14\">Group 14</a></li><li><a href=\"#sem-1-\"15\">Group 15</a></li><li><a href=\"#sem-1-\"16\">Group 16</a></li><li><a href=\"#sem-1-\"17\">Group 17</a></li><li><a href=\"#sem-1-\"18\">Group 18</a></li><li><a href=\"#sem-1-\"19\">Group 19</a></li><li><a href=\"#sem-1-\"20\">Group 20</a></li></ul><ul class='thirds-right'><li><a href=\"#sem-1-\"21\">Group 21</a></li><li><a href=\"#sem-1-\"22\">Group 22</a></li><li><a href=\"#sem-1-\"23\">Group 23</a></li><li><a href=\"#sem-1-\"24\">Group 24</a></li><li><a href=\"#sem-1-\"25\">Group 25</a></li><li><a href=\"#sem-1-\"26\">Group 26</a></li><li><a href=\"#sem-1-\"27\">Group 27</a></li><li><a href=\"#sem-1-\"28\">Group 28</a></li><li><a href=\"#sem-1-\"29\">Group 29</a></li><li><a href=\"#sem-1-\"30\">Group 30</a></li></ul></div>",
                          "Split in thirds (above 30) - Seminar groups");
});