package com.sanri.app.jdbc;

import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.lang3.ObjectUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TypeListHandler<T> implements ResultSetHandler<List<T>> {
    @Override
    public List<T> handle(ResultSet resultSet) throws SQLException {
        List<T> list= new ArrayList<T>();
        while (resultSet.next()){
            Object object = resultSet.getObject(1);
            list.add((T) object);
        }
        return list;
    }
}
