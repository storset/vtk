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

/* TODO: test editor.js instead
test("DateTime parsing", function () {
  // !DST

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
  
  // DST
  
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
*/