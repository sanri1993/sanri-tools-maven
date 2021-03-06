package com.sanri.tools.modules.database.service;

import com.sanri.tools.modules.core.service.file.FileManager;
import com.sanri.tools.modules.database.dtos.CodeGeneratorConfig;
import com.sanri.tools.modules.database.dtos.meta.Column;
import com.sanri.tools.modules.database.dtos.meta.PrimaryKey;
import com.sanri.tools.modules.database.dtos.meta.Table;
import com.sanri.tools.modules.database.dtos.meta.TableMetaData;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExcelDocService {

    @Autowired
    private JdbcService jdbcService;

    @Autowired
    private FileManager fileManager;

    public Path generate(CodeGeneratorConfig.DataSourceConfig dataSourceConfig) throws IOException, SQLException {
        List<TableMetaData> tableMetaDataList = jdbcService.filterChoseTables(dataSourceConfig.getConnName(), dataSourceConfig.getCatalog(), dataSourceConfig.getSchema(), dataSourceConfig.getTableNames());
        Workbook workbook = new XSSFWorkbook();

        for (TableMetaData tableMetaData : tableMetaDataList) {
            Table table = tableMetaData.getTable();
            List<Column> columns = tableMetaData.getColumns();
            String remark = table.getRemark();
            if (StringUtils.isBlank(remark)){
                remark = table.getActualTableName().getTableName();
            }
            Sheet sheet = workbook.createSheet(remark);
            Row head = sheet.createRow(0);
            fillValues(head,"?????????????????????","?????????????????????","????????????","?????????????????????","????????????","?????????","?????????","??????","????????????","????????????");

            List<PrimaryKey> primaryKeys = tableMetaData.getPrimaryKeys();
            List<String> primaryKeyColumns = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(primaryKeys)){
                primaryKeyColumns = primaryKeys.stream().map(PrimaryKey::getColumnName).collect(Collectors.toList());
            }
            for (int i = 0; i < columns.size(); i++) {
                Column column = columns.get(i);
                Row row = sheet.createRow(i + 1);
                String typeName = column.getTypeName();
                int columnSize = column.getColumnSize();
                int decimalDigits = column.getDecimalDigits();
                String mergeTypeName = "";
                if (decimalDigits != 0){
                    mergeTypeName = typeName+"("+columnSize+","+decimalDigits+")";
                }else{
                    mergeTypeName = typeName+"("+columnSize+")";
                }
                boolean primaryKey = false;
                if (primaryKeyColumns.contains(column.getColumnName())){
                    primaryKey = true;
                }
                fillValues(row,column.getColumnName(),column.getRemark(),mergeTypeName,primaryKey ? "???":"???",column.isNullable() ? "YES":"NO","","",column.getRemark(),"");
            }
        }
        File dir = fileManager.mkTmpDir("db/meta/generate");
        File file = new File(dir, System.currentTimeMillis() + ".xlsx");

        FileOutputStream fileOutputStream = new FileOutputStream(file);
        workbook.write(fileOutputStream);
        fileOutputStream.close();
        Path path = fileManager.relativePath(file.toPath());
        return path;
    }

    private void fillValues(Row row,String... values){
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellType(Cell.CELL_TYPE_STRING);
            cell.setCellValue(values[i]);
        }
    }
}
