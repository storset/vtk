package org.vortikal.repository.store.db.ibatis;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.orm.ibatis.support.BlobByteArrayTypeHandler;
import org.vortikal.repository.ContentStream;

public class BlobByteArrayTypeHandlerCallBack extends BlobByteArrayTypeHandler {

    @Override
    protected void setParameterInternal(PreparedStatement ps, int index, Object value,
            String jdbcType, LobCreator lobCreator) throws SQLException {

        ContentStream cs = (ContentStream) value;
        InputStream in = cs.getStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int i;
        try {
            while ((i = in.read()) != -1) {
                out.write(i);
            }
        } catch (IOException e) {
        }
        super.setParameterInternal(ps, index, out.toByteArray(), jdbcType, lobCreator);
    }

}
