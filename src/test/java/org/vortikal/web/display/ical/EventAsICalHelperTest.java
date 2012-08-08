package org.vortikal.web.display.ical;

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Calendar;

import org.junit.BeforeClass;
import org.junit.Test;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.PropertySetImpl;
import org.vortikal.repository.RepositoryResourceSetUpHelper;
import org.vortikal.repository.resourcetype.DateValueFormatter;
import org.vortikal.repository.resourcetype.HtmlValueFormatter;
import org.vortikal.repository.resourcetype.PropertyType.Type;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.StringValueFormatter;

public class EventAsICalHelperTest {

    private static EventAsICalHelper helper;
    private static PropertyTypeDefinition startDatePropDef;
    private static PropertyTypeDefinition endDatePropDef;
    private static PropertyTypeDefinition locationPropDef;
    private static PropertyTypeDefinition introductionPropDef;
    private static PropertyTypeDefinition titlePropDef;

    @BeforeClass
    public static void init() throws Exception {
        helper = new EventAsICalHelper();

        startDatePropDef = RepositoryResourceSetUpHelper.getPropertyTypeDefinition(Namespace.DEFAULT_NAMESPACE,
                "start-date", Type.DATE, new DateValueFormatter());
        endDatePropDef = RepositoryResourceSetUpHelper.getPropertyTypeDefinition(Namespace.DEFAULT_NAMESPACE,
                "end-date", Type.DATE, new DateValueFormatter());
        locationPropDef = RepositoryResourceSetUpHelper.getPropertyTypeDefinition(Namespace.DEFAULT_NAMESPACE,
                "location", Type.STRING, new StringValueFormatter());
        introductionPropDef = RepositoryResourceSetUpHelper.getPropertyTypeDefinition(Namespace.DEFAULT_NAMESPACE,
                "introduction", Type.HTML, new HtmlValueFormatter());
        titlePropDef = RepositoryResourceSetUpHelper.getPropertyTypeDefinition(Namespace.DEFAULT_NAMESPACE, "title",
                Type.STRING, new StringValueFormatter());

        helper.setStartDatePropDef(startDatePropDef);
        helper.setEndDatePropDef(endDatePropDef);
        helper.setLocationPropDef(locationPropDef);
        helper.setIntroductionPropDef(introductionPropDef);
        helper.setTitlePropDef(titlePropDef);
    }

    @Test
    public void getAsICal() throws Exception {
        String iCal = helper.getAsICal(Arrays.asList(this.getEvent()));
        assertNotNull(iCal);
    }

    private PropertySet getEvent() {
        PropertySetImpl event = new PropertySetImpl();

        Calendar cal = Calendar.getInstance();
        Property startDateProp = startDatePropDef.createProperty(cal.getTime());
        event.addProperty(startDateProp);

        cal.add(Calendar.HOUR, 2);
        Property endDateProp = endDatePropDef.createProperty(cal.getTime());
        event.addProperty(endDateProp);

        // XXX Needs proper setup of HtmlValueMapper
        // Property introductionProp = introductionPropDef.createProperty("Introduction");
        // event.addProperty(introductionProp);

        Property locationProp = locationPropDef.createProperty("UiO");
        event.addProperty(locationProp);
        
        Property titleProp = titlePropDef.createProperty("ICal for an Event");
        event.addProperty(titleProp);

        return event;
    }

}
