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
      "d6": "Lørdag"
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
  var datetime = utils.getDateTime("2014-08-18T12:15:00.000+02:00", "2014-08-18T14:00:00.000+02:00");
  equal(utils.getDateFormatted(datetime.start, datetime.end), "18.08.14", "Date");
  equal(utils.getTimeFormatted(datetime.start, datetime.end), "12:15&ndash;14:00",  "Time");
  
  var endUTCDay = utils.getEndUTCDayFormatted(datetime.start, datetime.end, i18n);
  equal(endUTCDay.day, "Mandag",  "Day +0200 - 12:15=>14:00");
  equal(endUTCDay.endUTC, "Mon Aug 18 2014 12:00:00 GMT+0200 (CEST)", "UTC End date - 12:00");
  
  var datetime2 = utils.getDateTime("2014-11-03T23:15:00.000+01:00", "2014-11-03T23:59:00.000+01:00");
  var endUTCDay2 = utils.getEndUTCDayFormatted(datetime2.start, datetime2.end, i18n);
  equal(endUTCDay2.day, "Mandag",  "Day +0100 - 23:15=>23:59");
  equal(endUTCDay2.endUTC, "Mon Nov 03 2014 22:59:00 GMT+0100 (CET)", "UTC End date - 22:59");
  
  var datetime3 = utils.getDateTime("2014-11-03T00:15:00.000+01:00", "2014-11-03T01:59:00.000+01:00");
  var endUTCDay3 = utils.getEndUTCDayFormatted(datetime3.start, datetime3.end, i18n);
  equal(endUTCDay3.day, "Mandag",  "Day +0100 - 00:15=>01:59");
  equal(endUTCDay3.endUTC, "Mon Nov 03 2014 00:59:00 GMT+0100 (CET)", "UTC End date - 00:59");
});
test("Generating Table HTML", function () {
  equal(utils.getTitle(session), "Åpningsforelesning",
                                 "Title - from Vortex");
  equal(utils.getPlace(session), "<abbr title='Georg Morgenstiernes hus'>GM</abbr> <a title='Seminarrom 205' href='http://www.med.uio.no/om/finn-fram/kart/vis/#bl1602,300,253'>205</a>",
                                 "Place - Abbr with title + Link with title");
  equal(utils.getStaff(session), "<ul><li>rezam</li><li>oyvihatl</li><li><a href='http://www.nrk.no/'>G. Flaksnes</a></li><li><a href='http://www.aftenposten.no/'>R. Rabbit</a></li><li><a href='http://www.dagbladet.no/'>B. K. Funken-Hagen</a></li></ul>",
                                 "Staff - List of: Just text or link");
  equal(utils.getResources(session), "<a href='http://www.vg.no/'>Pensumlitteratur (PDF)</a><ul><li>listepunkt #1</li><li>listepunkt #2</li></ul>",
                                 "Resources - List of: Link + freetext");
});
test("Generating ToC HTML", function () {
  equal(utils.splitThirds(['<li><a href="#sem-2-11">Group 11</a> - thu 16:30–18:15</li>',
                           '<li><a href="#sem-2-12">Group 12</a> - thu 16:30–18:15</li>',
                           '<li><a href="#sem-2-13">Group 13</a> - thu 16:30–18:15</li>',
                           '<li><a href="#sem-2-14">Group 14</a> - thu 16:30–18:15</li>',
                           '<li><a href="#sem-2-15">Group 15</a> - thu 12:15–14:00</li>'], "Seminar"),
                           "<p>Seminar</p><div class='course-schedule-toc-thirds'><ul class='thirds-left'><li><a href=\"#sem-2-11\">Group 11</a> - thu 16:30–18:15</li><li><a href=\"#sem-2-12\">Group 12</a> - thu 16:30–18:15</li></ul><ul class='thirds-middle'><li><a href=\"#sem-2-13\">Group 13</a> - thu 16:30–18:15</li><li><a href=\"#sem-2-14\">Group 14</a> - thu 16:30–18:15</li></ul><ul class='thirds-right'><li><a href=\"#sem-2-15\">Group 15</a> - thu 12:15–14:00</li></ul></div>",
                           "Split in thirds - Seminar groups");
});