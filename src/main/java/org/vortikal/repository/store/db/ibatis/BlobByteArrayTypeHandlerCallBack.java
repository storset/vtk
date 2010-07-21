package org.vortikal.repository.store.db.ibatis;

import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.orm.ibatis.support.BlobByteArrayTypeHandler;
import org.vortikal.repository.ContentStream;
import org.vortikal.util.io.StreamUtil;

public class BlobByteArrayTypeHandlerCallBack extends BlobByteArrayTypeHandler {

    @Override
    protected void setParameterInternal(PreparedStatement ps, int index, Object value,
            String jdbcType, LobCreator lobCreator) throws SQLException {

        ContentStream cs = (ContentStream) value;
        InputStream in = cs.getStream();
        try {
            byte[] data = StreamUtil.readInputStream(in);
            super.setParameterInternal(ps, index, data, jdbcType, lobCreator);
        } catch (IOException io) {
            throw new SQLException(io);
        }
    }

}
