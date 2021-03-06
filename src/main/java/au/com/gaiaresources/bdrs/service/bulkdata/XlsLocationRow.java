package au.com.gaiaresources.bdrs.service.bulkdata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import au.com.gaiaresources.bdrs.controller.insecure.taxa.ComparePersistentImplByWeight;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;

/**
 * Performs the reading and writing of XLS rows representing {@link Location}s.
 */
public class XlsLocationRow extends StyledRowImpl {
    
    private Logger log = Logger.getLogger(getClass());
    
    private BulkDataReadWriteService bulkDataReadWriteService;
    private Survey survey;
    
    private List<Attribute> locationScopeAttrList = null;

    private Cell currentReadCell;
    
    /**
     * Create a new instance.
     * @param bulkDataReadWriteService used for writing attribute values to cells.
     * @param survey the survey containing the locations to be stored. User locations
     * referenced by Records contained by this survey shall also be stored.
     */
    public XlsLocationRow(BulkDataReadWriteService bulkDataReadWriteService, Survey survey) {
        this.bulkDataReadWriteService = bulkDataReadWriteService;
        this.survey = survey;
    }
    
    public XlsLocationRow(BulkDataReadWriteService bulkDataReadWriteService) {
        this(bulkDataReadWriteService, null);
    }
    
    // ---------------------------
    // Writing Methods
    // ---------------------------
    public void writeLocationHeader(Workbook wb) {
        Sheet locSheet = getLocationSheet(wb);
        List<Attribute> locationScopeAttrs = getLocationScopeAttributes(survey);
        writeLocationHeader(locSheet, 0, locationScopeAttrs);
    }
    
    /**
     * Creates a row on the location sheet to represent a survey location.
     * @param wb the workbook containing the location sheet. If a location sheet
     * cannot be found, one shall be created.
     */
    public void writeSurveyLocations(Workbook wb) {
        Sheet locSheet = getLocationSheet(wb);
        if (survey == null) {
            // cannot write survey locations for null survey
            log.warn("Cannot write survey locations for null survey");
            return;
        }
        List<Attribute> locationScopeAttrs = getLocationScopeAttributes(survey);
        
        int rowIndex = locSheet.getLastRowNum()+1;
        
        for (Location loc : survey.getLocations()) {
            Row row = locSheet.createRow(rowIndex++);
            int colIndex = 0;

            row.createCell(colIndex++).setCellValue(loc.getId());
            row.createCell(colIndex++).setCellValue(AbstractBulkDataService.LOCATION_SHEET_SURVEY_LOCATION);
            row.createCell(colIndex++).setCellValue(loc.getName());
            row.createCell(colIndex++).setCellValue(loc.getLocation().getCentroid().getY());
            row.createCell(colIndex++).setCellValue(loc.getLocation().getCentroid().getX());
            row.createCell(colIndex++).setCellValue(formatCrs(loc));
            
            // Location Attribute Values
            Map<Attribute, AttributeValue> locAttrValMap = new HashMap<Attribute, AttributeValue>();
            for(AttributeValue attrVal : loc.getAttributes()) {
                locAttrValMap.put(attrVal.getAttribute(), attrVal);
            }
            
            for(Attribute attr : locationScopeAttrs) {
                AttributeType attrType = attr.getType();
                if (!AttributeType.FILE.equals(attrType)
                    && !AttributeType.IMAGE.equals(attrType)
                    && !AttributeType.AUDIO.equals(attrType)
                    && !AttributeType.VIDEO.equals(attrType)
                    && !AttributeType.isHTMLType(attrType)
                    && !AttributeType.isCensusMethodType(attrType)) {
                
                    AttributeValue attrVal = locAttrValMap.get(attr);
                    bulkDataReadWriteService.writeTypedAttributeValueCell(this,
                                                                          row.createCell(colIndex++), 
                                                                          attrVal);
                }
            }
        }
    }
    
    /**
     * Creates a row on the location sheet to represent a user location.
     * @param wb the workbook containing the location sheet. If a location sheet
     * cannot be found, one shall be created.
     * @param location the location to be added to the sheet.
     */
    public void writeUserLocation(Workbook wb, Location location) {
        Sheet locSheet = getLocationSheet(wb);
        
        // need to add one onto the last row number else we are overwriting an existing row
        Row row = locSheet.createRow(locSheet.getLastRowNum() + 1);
        
        int colIndex = 0;
        row.createCell(colIndex++).setCellValue(location.getId());
        row.createCell(colIndex++).setCellValue(AbstractBulkDataService.LOCATION_SHEET_USER_LOCATION);
        row.createCell(colIndex++).setCellValue(location.getName());
        row.createCell(colIndex++).setCellValue(location.getLocation().getCentroid().getY());
        row.createCell(colIndex++).setCellValue(location.getLocation().getCentroid().getX());
        row.createCell(colIndex++).setCellValue(formatCrs(location));
        
        // user locations do not have any location attribute values.
        // if that changes. add that stuff here.
    }
    
    private String formatCrs(Location loc) {
    	return BdrsCoordReferenceSystem.sridToEpsg(loc.getLocation().getSRID());
    }
    
    /**
     * Returns an existing location sheet or creates a location sheet if one
     * does not exist.
     * @param wb the workbook containing the location sheet or where it shall
     * be created.
     */
    private Sheet getLocationSheet(Workbook wb) {
        Sheet sheet = wb.getSheet(AbstractBulkDataService.LOCATION_SHEET_NAME);
        if(sheet == null) {
            super.createCellStyles(wb);
            sheet = wb.createSheet(AbstractBulkDataService.LOCATION_SHEET_NAME);
        } 
        
        return sheet;
    }
    
    private int writeLocationHeader(Sheet locSheet, int rowIndex, List<Attribute> locationScopeAttrs) {
        CellStyle headerStyle = getCellStyleByKey(STYLE_LOCATION_HEADER);
        int colIndex = 0;
        Cell cell;
        Row row = locSheet.createRow(rowIndex++);
        
        cell = row.createCell(colIndex++);
        cell.setCellValue(AbstractBulkDataService.LOCATION_SHEET_LOCATION_ID);
        cell.setCellStyle(headerStyle);

        cell = row.createCell(colIndex++);
        cell.setCellValue(AbstractBulkDataService.LOCATION_SHEET_LOCATION_TYPE);
        cell.setCellStyle(headerStyle);
        
        cell = row.createCell(colIndex++);
        cell.setCellValue(AbstractBulkDataService.LOCATION_SHEET_LOCATION_NAME);
        cell.setCellStyle(headerStyle);

        cell = row.createCell(colIndex++);
        cell.setCellValue("Latitude/Northings");
        cell.setCellStyle(headerStyle);

        cell = row.createCell(colIndex++);
        cell.setCellValue("Longitude/Eastings");
        cell.setCellStyle(headerStyle);
        
        cell = row.createCell(colIndex++);
        cell.setCellValue("EPSG Code");
        cell.setCellStyle(headerStyle);
        
        for(Attribute attr : locationScopeAttrs) {
            AttributeType attrType = attr.getType();
            if (!AttributeType.FILE.equals(attrType)
                && !AttributeType.IMAGE.equals(attrType)
                && !AttributeType.AUDIO.equals(attrType)
                && !AttributeType.VIDEO.equals(attrType)
                && !AttributeType.isHTMLType(attrType)
                && !AttributeType.isCensusMethodType(attrType)) {
                
                cell = row.createCell(colIndex++);
                cell.setCellValue(attr.getDescription());
                cell.setCellStyle(headerStyle);
            }
        }
        
        return rowIndex;
    }
    
    // ---------------------------
    // Reading Methods
    // ---------------------------
    /**
     * Parses a single location row returning a representation of the 
     * contained {@link Location} as a {@link LocationUpload}.
     * @param row the xls row containing the representation of the location.
     * @return a LocationUpload representing the location in the row.
     */
    public LocationUpload readRow(Row row) {
        currentReadCell = null;
        LocationUpload locationUpload = new LocationUpload();
        
        locationUpload.setRowNumber(row.getRowNum());
        locationUpload.setSheetName(AbstractBulkDataService.LOCATION_SHEET_LOCATION_NAME);
        
        int colIndex = 0;
        try {
            currentReadCell = row.getCell(colIndex, Row.CREATE_NULL_AS_BLANK);
            if(Cell.CELL_TYPE_BLANK != currentReadCell.getCellType()) {
                locationUpload.setPk((int)XlsCellUtil.cellToDouble(currentReadCell));
            }
            
            // Skip over the location type. We don't need it.
            ++colIndex;
            
            currentReadCell = row.getCell(++colIndex);
            locationUpload.setLocationName(XlsCellUtil.cellToString(currentReadCell));
            
            currentReadCell = row.getCell(++colIndex);
            locationUpload.setLatitude(XlsCellUtil.cellToDouble(currentReadCell));
            
            currentReadCell = row.getCell(++colIndex);
            locationUpload.setLongitude(XlsCellUtil.cellToDouble(currentReadCell));
            
            currentReadCell = row.getCell(++colIndex);
            locationUpload.setEpsg(XlsCellUtil.cellToString(currentReadCell));
            
            locationUpload.setSurveyName(survey.getName());
            
            colIndex = readLocationAttributeValue(locationUpload, row, ++colIndex);
                                                      
        } catch (Exception e) {
            String msg = e.getMessage() == null ? e.toString() : e.getMessage();
            
            String value = currentReadCell != null ? currentReadCell.toString() : null;
            String cellColRef = currentReadCell != null ? CellReference.convertNumToColString(currentReadCell.getColumnIndex()) : CellReference.convertNumToColString(colIndex);
            String sheetName = row.getSheet().getSheetName();
            
            String errMsg = String.format("Cell %s!%s%d[ value=\"%s\" ]: %s %s", sheetName, cellColRef, row.getRowNum()+1, value, e.getClass().getSimpleName(), msg);
            locationUpload.setErrorMessage(errMsg);
            
            log.warn(e.toString(), e);
        }
        
        return locationUpload;
    }
    
    private int readLocationAttributeValue(LocationUpload locationUpload, Row row, int colIndex) {
        for (Attribute attr : getLocationScopeAttributes(survey)) {
            String attrValue = null;
            currentReadCell = row.getCell(colIndex++, Row.CREATE_NULL_AS_BLANK);
            
            if(Cell.CELL_TYPE_BLANK == currentReadCell.getCellType()) {
                // Ideally we would delete the attribute value if there was one.
                attrValue = "";
            } else {
                switch (attr.getType()) {
                    case INTEGER:
                    case INTEGER_WITH_RANGE:
                    case DECIMAL:
                        attrValue = String.valueOf(XlsCellUtil.cellToDouble(currentReadCell));
                        break;
                    case DATE:
                        // Perhaps we shouldn't be using that particular date format.
                        // It should be referring to the 'canonical' way that we 
                        // convert dates to strings.
                        Date d = XlsCellUtil.cellToDate(currentReadCell);
                        attrValue = XlsCellUtil.getDateFormatter().format(d);
                        break;
                    case IMAGE:
                    case AUDIO:
                    case VIDEO:
                    case FILE:
                        throw new UnsupportedOperationException(
                                "Spreadsheet upload of file data is not supported.");
                    case CENSUS_METHOD_ROW:
                    case CENSUS_METHOD_COL:
                        throw new UnsupportedOperationException(
                                "Spreadsheet upload of census method attribute data is not supported.");
                    case TEXT:
                    case STRING_WITH_VALID_VALUES:
                    case STRING:
                    case HTML:
                    case HTML_NO_VALIDATION:
                    case HTML_COMMENT:
                    case HTML_HORIZONTAL_RULE:
                    case SPECIES:
                    default:
                        attrValue = XlsCellUtil.cellToString(currentReadCell);
                        break;
                }
            }
            
            locationUpload.setAttributeValue(attr, attrValue);
        }
        return colIndex;
    }
    
    // ---------------------------
    // Utility Methods
    // ---------------------------
    /** 
     * Returns true if the specified row is a header row, otherwise false.
     * @Row row the row to be interrogated.
     * @returns true if the specified row is a header row, false otherwise.
     */
    public boolean isHeader(Row row) {
        return row.getRowNum() == 0;
    }
    
    private List<Attribute> getLocationScopeAttributes(Survey survey) {
        
        if(locationScopeAttrList == null && survey != null) {
            List<Attribute> attrList = new ArrayList<Attribute>(survey.getAttributes());
            Collections.sort(attrList, new ComparePersistentImplByWeight());
            
            List<Attribute> locationScopeAttrs = new ArrayList<Attribute>();
            for(Attribute attr : attrList) {
                if(AttributeScope.LOCATION.equals(attr.getScope())) {
                    locationScopeAttrs.add(attr);
                }
            }
            
            locationScopeAttrList = Collections.unmodifiableList(locationScopeAttrs);
        } else if (survey == null) {
            locationScopeAttrList = Collections.EMPTY_LIST;
        }
        
        return locationScopeAttrList;
    }
}