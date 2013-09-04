package org.vortikal.resourcemanagement.view.tl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import net.sf.json.JSONObject;

import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.text.tl.Context;
import org.vortikal.text.tl.Symbol;
import org.vortikal.text.tl.expr.Function;
import org.vortikal.web.RequestContext;

public class RetrieveJsonHandler extends Function {

    public RetrieveJsonHandler(Symbol symbol) {
        super(symbol, 1);
    }

    @Override
    public Object eval(Context ctx, Object... args) {

        Object arg = args[0];
        String ref = arg.toString();
        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();

        try {
            Path uri;
            if (!ref.startsWith("/")) {
                uri = requestContext.getResourceURI().getParent().expand(ref);
            } else {
                uri = Path.fromString(ref);
            }
            String token = requestContext.getSecurityToken();
            InputStream is = repository.getInputStream(token, uri, false);
            InputStreamReader isr = new InputStreamReader(is,"UTF-8");
            BufferedReader br = new BufferedReader(isr);

            String line;
            String result = "";
            while ((line = br.readLine()) != null) {
                result += line;
            }
            is.close();

            return JSONObject.fromObject(result);
        } catch (Throwable t) {
        }
        
        return null;
    }
}
