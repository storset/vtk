package org.vortikal.resourcemanagement.view.tl;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.support.RequestContextUtils;
import org.vortikal.text.tl.Context;
import org.vortikal.text.tl.Symbol;
import org.vortikal.text.tl.expr.Function;
import org.vortikal.web.RequestContext;

public class ResourceLocaleFunction extends Function {

    public ResourceLocaleFunction(Symbol symbol) {
        super(symbol,0);
    }

    @Override
    public Object eval(Context ctx, Object... args) {
        RequestContext requestContext = RequestContext.getRequestContext();
        HttpServletRequest request = requestContext.getServletRequest();
        return RequestContextUtils.getLocale(request).toString();
    }


}
