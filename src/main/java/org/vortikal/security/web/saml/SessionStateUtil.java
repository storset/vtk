package org.vortikal.security.web.saml;

import java.util.ArrayList;
import javax.servlet.http.HttpSession;

public class SessionStateUtil {
    public static final String ISSUED_IDS_ATTRIBUTE_NAME = "issuedIds";



    public static void addIssuedSamlRequestId(HttpSession session, String samlRequestId) {
    //    retrieveIdListFromSession(session).put(samlRequestId);
    }

    //public static

    private static ArrayList retrieveIdListFromSession(HttpSession session) {
        ArrayList idList = (ArrayList) session.getAttribute(ISSUED_IDS_ATTRIBUTE_NAME);
        if (idList == null) {
            idList = new ArrayList();
            session.setAttribute(ISSUED_IDS_ATTRIBUTE_NAME, idList);
        }

        return idList;
    }

}
