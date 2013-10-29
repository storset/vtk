package org.vortikal.web.referencedata.provider;

import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.web.referencedata.ReferenceDataProvider;

public class ServerNowTimeProvider implements ReferenceDataProvider {

    @Override
    public void referenceData(Map<String, Object> model, HttpServletRequest request) throws Exception {
        model.put("nowTime", new Date());
    }
}
