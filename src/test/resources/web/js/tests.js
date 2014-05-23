var utils = null;
var session = null;
module("Schedule.js", {
  setup: function() {
    utils = new scheduleUtils();
    session = {
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
      "vrtxTitle": "Åpningsforelesning",
      "vrtxStaff": [
        { "id": "rezam" },
        { "id": "oyvihatl" }
      ],
      "vrtxStaffExternal": [
        { "name": "Gunnar Flaksnes", "url": "http://www.nrk.no/" },
        { "name": "Roger Rabbit", "url": "http://www.aftenposten.no/" }
      ],
      "vrtxResources": [
        { "title": "Pensumlitteratur (PDF)", "url": "http://www.vg.no/" }
      ],
      "vrtxResourcesText": "<ul><li>listepunkt #1</li><li>listepunkt #2</li></ul>"
    };
  }, teardown: function() {
    utils = null;
    session = null;
  }
});

test("DateTime parsing", function () {
  var datetime = utils.getDateTime("2014-08-18T12:15:00.000+02:00", "2014-08-18T14:00:00.000+02:00");
  equal(utils.getDateFormatted(datetime.start, datetime.end), "18.08.14", "Date");
  equal(utils.getTimeFormatted(datetime.start, datetime.end), "12:15&ndash;14:00",  "Time");
  equal(utils.getDayFormatted(datetime.start, datetime.end, {
    "d0": "Søndag",
    "d1": "Mandag",
    "d2": "Tirsdag",
    "d3": "Onsdag",
    "d4": "Torsdag",
    "d5": "Fredag",
    "d6": "Lørdag"
  }), "Mandag",  "Day +0200 - 12:15=>14:00");
  var datetime2 = utils.getDateTime("2014-11-03T23:15:00.000+01:00", "2014-11-03T23:59:00.000+01:00");
  equal(utils.getDayFormatted(datetime2.start, datetime2.end, {
    "d0": "Søndag",
    "d1": "Mandag",
    "d2": "Tirsdag",
    "d3": "Onsdag",
    "d4": "Torsdag",
    "d5": "Fredag",
    "d6": "Lørdag"
  }), "Mandag",  "Day +0100 - 23:15=>23:59");
  var datetime3 = utils.getDateTime("2014-11-03T00:15:00.000+01:00", "2014-11-03T01:59:00.000+01:00");
  equal(utils.getDayFormatted(datetime3.start, datetime3.end, {
    "d0": "Søndag",
    "d1": "Mandag",
    "d2": "Tirsdag",
    "d3": "Onsdag",
    "d4": "Torsdag",
    "d5": "Fredag",
    "d6": "Lørdag"
  }), "Mandag",  "Day +0100 - 00:15=>01:59");
});
test("Generating HTML from JSON for Title", function () {
  equal(utils.getTitle(session), "Åpningsforelesning", "Title from Vortex");
});
test("Generating HTML from JSON for Place", function () {
  equal(utils.getPlace(session), "<abbr title='Georg Morgenstiernes hus'>GM</abbr> <a title='Seminarrom 205' href='http://www.med.uio.no/om/finn-fram/kart/vis/#bl1602,300,253'>205</a>", "Abbr with title + Link with title");
});
test("Generating HTML from JSON for Staff", function () {
  equal(utils.getStaff(session), "<ul><li>rezam</li><li>oyvihatl</li><li><a href='http://www.nrk.no/'>G. Flaksnes</a></li><li><a href='http://www.aftenposten.no/'>R. Rabbit</a></li></ul>", "List of: Just text or link");
});
test("Generating HTML from JSON for Resources", function () {
  equal(utils.getResources(session), "<a href='http://www.vg.no/'>Pensumlitteratur (PDF)</a><ul><li>listepunkt #1</li><li>listepunkt #2</li></ul>", "List of: Link + freetext");
});