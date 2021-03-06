package au.com.gaiaresources.bdrs.controller.map;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javax.activation.FileDataSource;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;

import au.com.gaiaresources.bdrs.kml.BDRSKMLWriter;
import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.bdrs.service.content.ContentService;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.controller.webservice.JqGridDataBuilder;
import au.com.gaiaresources.bdrs.controller.webservice.JqGridDataHelper;
import au.com.gaiaresources.bdrs.controller.webservice.JqGridDataRow;
import au.com.gaiaresources.bdrs.db.SessionFactory;
import au.com.gaiaresources.bdrs.db.impl.PagedQueryResult;
import au.com.gaiaresources.bdrs.db.impl.PaginationFilter;
import au.com.gaiaresources.bdrs.db.impl.SortOrder;
import au.com.gaiaresources.bdrs.file.FileService;
import au.com.gaiaresources.bdrs.geometry.GeometryBuilder;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.file.ManagedFile;
import au.com.gaiaresources.bdrs.model.file.ManagedFileDAO;
import au.com.gaiaresources.bdrs.model.map.AssignedGeoMapLayer;
import au.com.gaiaresources.bdrs.model.map.GeoMapFeature;
import au.com.gaiaresources.bdrs.model.map.GeoMapFeatureDAO;
import au.com.gaiaresources.bdrs.model.map.GeoMapLayer;
import au.com.gaiaresources.bdrs.model.map.GeoMapLayerDAO;
import au.com.gaiaresources.bdrs.model.map.GeoMapLayerSource;
import au.com.gaiaresources.bdrs.model.record.AccessControlledRecordAdapter;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.TypedAttributeValue;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.web.JsonService;
import au.com.gaiaresources.bdrs.servlet.view.PortalRedirectView;
import au.com.gaiaresources.bdrs.spatial.ShapeFileReader;
import au.com.gaiaresources.bdrs.util.SpatialUtilFactory;
import au.com.gaiaresources.bdrs.util.TransactionHelper;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

@Controller
public class GeoMapLayerController extends AbstractController {

    public static final String BASE_ADMIN_URL = "/bdrs/admin/mapLayer/";
    public static final String LISTING_URL = BASE_ADMIN_URL + "listing.htm";
    public static final String EDIT_URL = BASE_ADMIN_URL + "edit.htm";
    public static final String DELETE_LAYER_URL = BASE_ADMIN_URL + "delete.htm";
    
    public static final String LIST_SERVICE_URL = BASE_ADMIN_URL + "listService.htm";
    public static final String GET_LAYER_URL = "/bdrs/map/getLayer.htm";
    public static final String GET_RECORD_URL = "/bdrs/map/getRecord.htm";
    
    public static final String DOWNLOAD_RECORDS_URL = "bdrs/map/downloadRecords.htm";
    
    public static final String GET_FEATURE_SERVICE_URL = "/bdrs/map/getFeatureInfo.htm";
    public static final String CHECK_SHAPEFILE_SERVICE_URL = "/bdrs/map/checkShapefile.htm";
    public static final String GET_FEATURE_INFO_BY_ID_SERVICE_URL = "/bdrs/map/getFeatureInfoById.htm";
    
    public static final String GEO_MAP_LAYER_PK_VIEW = "geoMapLayerId";
    public static final String GEO_MAP_LAYER_PK_SAVE = "geoMapLayerPk";
    
    public static final String FILTER_NAME = "name";
    public static final String FILTER_DESCRIPTION = "description";
    
    public static final String PARAM_SURVEY_ID = "surveyPk";
    public static final String PARAM_PUBLISH = "publish";
    public static final String PARAM_NAME = "name";
    public static final String PARAM_DESCRIPTION = "desc";
    public static final String PARAM_HIDE_PRIVATE_DETAILS = "hidePrivateDetails";
    public static final String PARAM_MANAGED_FILE_UUID = "mfuuid";
    
    public static final String PARAM_MAP_LAYER_SRC = "layerSrc";
    public static final String PARAM_SHAPE_TO_DB = "shpToDatabase";
    
    public static final String PARAM_LATITUDE_Y = "latitude";
    public static final String PARAM_LONGITUDE_X = "longitude";
    public static final String PARAM_BUFFER_KM = "buffer";
    public static final String PARAM_MAP_LAYER_ID = "mapLayerId";
    
    public static final String PARAM_DOWNLOAD_FORMAT = "downloadFormat";
    
    public static final String PARAM_LAYER_ID = "layerPk";
    public static final String PARAM_FEATURE_ID = "featureId";
    
    public static final String PARAM_STROKE_COLOR = "strokeColor";
    public static final String PARAM_FILL_COLOR = "fillColor";
    public static final String PARAM_SYMBOL_SIZE = "symbolSize";
    public static final String PARAM_STROKE_WIDTH = "strokeWidth";
    
    public static final String PARAM_SERVER_URL = "serverUrl";
    
    public static final String PARAM_RECORD_ID = "recordPk";
    
    public static final String JSON_KEY_ITEMS = "items";
    
    public static final String KML_RECORD_FOLDER = "Record";
    public static final String KML_POINT_ICON_ID = "pointIcon";
    
    public static final String FORMAT_KML = "kml";
    public static final String FORMAT_SHAPEFILE = "shapefile";
    
    // var hexColorRegex = new RegExp('#[0-9A-F]{6}', 'i');
    Pattern colorPattern = Pattern.compile("#[0-9A-F]{6}", Pattern.CASE_INSENSITIVE);
    
    @Autowired
    private GeoMapLayerDAO layerDAO;
    @Autowired
    private SurveyDAO surveyDAO;
    @Autowired
    private ManagedFileDAO mfDAO;
    @Autowired
    private FileService fileService;
    @Autowired
    private RecordDAO recDAO;
    @Autowired
    private AttributeDAO attrDAO;
    @Autowired
    private GeoMapFeatureDAO featureDAO;
    @Autowired
    private PreferenceDAO preferenceDAO;
    @Autowired
    private SessionFactory sessionFactory;
    
    GeometryBuilder geomBuilder = new GeometryBuilder();
    
    private Logger log = Logger.getLogger(getClass());
    
    @RolesAllowed( {Role.ROOT, Role.ADMIN} )
    @RequestMapping(value = LISTING_URL, method = RequestMethod.GET)
    public ModelAndView list(HttpServletRequest request, HttpServletResponse response) throws Exception { 
        ModelAndView mv = new ModelAndView("geoMapLayerListing");
        return mv;
    }
    
    @RolesAllowed( {Role.ROOT, Role.ADMIN} )
    @RequestMapping(value = EDIT_URL, method = RequestMethod.GET)
    public ModelAndView view(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value = GEO_MAP_LAYER_PK_VIEW, defaultValue="0", required=false) int mapLayerPk) {
        GeoMapLayer gml = mapLayerPk == 0 ? new GeoMapLayer() : layerDAO.get(mapLayerPk);
        if (gml == null) {
            throw new IllegalArgumentException("Invalid pk for geo map layer. pk = " + mapLayerPk);
        }
        ModelAndView mv = new ModelAndView("geoMapLayerEdit");
        mv.addObject("geoMapLayer", gml);
        mv.addObject("surveyList", surveyDAO.search(null).getList());
        return mv;
    }
    
    private static final int BATCH_SIZE = 20;
    
    @RolesAllowed( {Role.ROOT, Role.ADMIN} )
    @SuppressWarnings("unchecked")
    @RequestMapping(value = EDIT_URL, method = RequestMethod.POST)
    public ModelAndView save(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value = GEO_MAP_LAYER_PK_SAVE, defaultValue="0", required=false) int mapLayerPk,
            @RequestParam(value = PARAM_SURVEY_ID, defaultValue="0", required=false) int surveyPk,
            @RequestParam(value = PARAM_NAME, required=true) String name,
            @RequestParam(value = PARAM_DESCRIPTION, required=true) String desc,
            @RequestParam(value = PARAM_PUBLISH, defaultValue="false") boolean publish,
            @RequestParam(value = PARAM_HIDE_PRIVATE_DETAILS, defaultValue="false") boolean hidePrivateDetails,
            @RequestParam(value = PARAM_MANAGED_FILE_UUID, defaultValue="") String mfuuid,
            @RequestParam(value = PARAM_MAP_LAYER_SRC, required=true) String mapLayerSrc,
            @RequestParam(value = PARAM_SHAPE_TO_DB, defaultValue="false") boolean shapeToDatabase,
            @RequestParam(value = PARAM_STROKE_COLOR, defaultValue="") String strokeColor,
            @RequestParam(value = PARAM_FILL_COLOR, defaultValue="") String fillColor,
            @RequestParam(value = PARAM_SYMBOL_SIZE, defaultValue="0") int symbolSize,
            @RequestParam(value = PARAM_STROKE_WIDTH, defaultValue="0") int strokeWidth,
            @RequestParam(value = PARAM_SERVER_URL, defaultValue="") String serverUrl) throws IOException {
        
        Session sesh = null;
        try {
            sesh = sessionFactory.openSession();

            sesh.beginTransaction();
            
            GeoMapLayer gml = mapLayerPk == 0 ? new GeoMapLayer() : layerDAO.get(sesh, mapLayerPk);
            if (gml == null) {
                throw new IllegalArgumentException("Invalid pk for geo map layer. pk = " + mapLayerPk);
            }
            
            strokeColor = strokeColor.trim();
            fillColor = fillColor.trim();
            boolean validStrokeColor = colorPattern.matcher(strokeColor).matches();
            boolean validFillColor = colorPattern.matcher(fillColor).matches();
            
            gml.setName(name.trim());
            gml.setDescription(desc.trim());
            gml.setSurvey(surveyDAO.getSurvey(sesh, surveyPk));
            gml.setPublish(publish);
            gml.setHidePrivateDetails(hidePrivateDetails);
            gml.setManagedFileUUID(mfuuid.trim());
            gml.setLayerSource(GeoMapLayerSource.fromString(mapLayerSrc));
            gml.setStrokeColor(validStrokeColor ? strokeColor : GeoMapLayer.DEFAULT_STROKE_COLOR);
            gml.setStrokeWidth(strokeWidth > 0 ? strokeWidth : 0);
            gml.setSymbolSize(symbolSize > 0 ? symbolSize : 0);
            gml.setFillColor(validFillColor ? fillColor : GeoMapLayer.DEFAULT_FILL_COLOR);
            gml.setServerUrl(StringUtils.hasLength(serverUrl) ? serverUrl.trim() : "");
            
            if (mapLayerPk == 0) {
                layerDAO.save(sesh, gml);
            } else {
                layerDAO.update(sesh, gml);
            }
            
            if (gml.getLayerSource() == GeoMapLayerSource.SHAPEFILE && shapeToDatabase) {
                // delete all existing records
                List<GeoMapFeature> featuresToDelete = featureDAO.find(sesh, gml.getId());
                
                List<Attribute> attrToDelete = gml.getAttributes();

                gml.setAttributes(Collections.EMPTY_LIST);
                layerDAO.update(sesh, gml);
                
                for (GeoMapFeature f : featuresToDelete) {
                    featureDAO.deleteCascade(sesh, f);
                }
                
                for (Attribute a : attrToDelete) {
                    // sometimes because of errors during writes of large shapefiles to the
                    // database we can get AttributeValues that aren't assigned to geo map features.
                    // so....
                    for (TypedAttributeValue av : attrDAO.getAttributeValueObjects(sesh, a)) {
                        attrDAO.delete(sesh, av);
                    }
                    attrDAO.delete(sesh, a);
                }
                
                sesh.flush();
                
                sesh.setFlushMode(FlushMode.MANUAL);
                // now insert the new stuff...
                ManagedFile mf = mfDAO.getManagedFile(sesh, gml.getManagedFileUUID());
                File file = fileService.getFile(mf, mf.getFilename()).getFile();
                
                ShapeFileReader reader = new ShapeFileReader(file);
    
                List<Attribute> attributeList = reader.readAttributes();
                List<GeoMapFeature> featuresToAdd = reader.readAsMapFeatures(attributeList);
                
                int weight = 1;
                gml.setAttributes(attributeList);
                for (Attribute a : gml.getAttributes()) {
                    a.setWeight(++weight);
                    a.setRunThreshold(false);
                    attrDAO.save(sesh, a);
                }
                sesh.flush();
                
                int insertCount = 0;

                while (!featuresToAdd.isEmpty()) {
                    // so garbage collection runs on big lists...
                    GeoMapFeature f = featuresToAdd.remove(0);

                    for (AttributeValue av : f.getAttributes()) {
                        av.setRunThreshold(false);
                        attrDAO.save(sesh, av);
                        insertCount = checkBatch(sesh, insertCount);
                    }
                    f.setLayer(gml);
                    
                    f.setRunThreshold(false);
                    featureDAO.save(sesh, f);
                    insertCount = checkBatch(sesh, insertCount);
                }   
                layerDAO.update(sesh, gml);
                
                getRequestContext().addMessage("bdrs.geoMapLayer.save.successWithDatabaseWrite", new Object[] { gml.getName() });
            } else if (gml.getLayerSource() == GeoMapLayerSource.SHAPEFILE && !shapeToDatabase) {
                getRequestContext().addMessage("bdrs.geoMapLayer.save.successNoDatabaseWrite", new Object[] { gml.getName() });
            } else {
                getRequestContext().addMessage("bdrs.geoMapLayer.save.success", new Object[] { gml.getName() });
            }
            
            if (StringUtils.hasLength(strokeColor) || StringUtils.hasLength(fillColor)) {
            	if (!validStrokeColor || !validFillColor) {
                    getRequestContext().addMessage("bdrs.geoMapLayer.save.invalidColor", new Object[] { gml.getName() });
                }
            }
        } finally {
            if (sesh != null) {
                if (sesh.isOpen()) {
                    sesh.flush();
                    sesh.clear();
                    TransactionHelper.commit(sesh);
                    
                    sesh.close();
                }
            }
        }
        ModelAndView mv = new ModelAndView(new PortalRedirectView(LISTING_URL, true));
        
        return mv;
    }
    
    private int checkBatch(Session sesh, int count) {
        if (count % BATCH_SIZE == 0) {
            sesh.flush();
            sesh.clear();
            
            TransactionHelper.commit(sesh);
            sesh.beginTransaction();
        }
        return count+1;
    }
    
    @RolesAllowed( {Role.ROOT, Role.ADMIN} )
    @RequestMapping(value = LIST_SERVICE_URL, method = RequestMethod.GET)
    public void listService(
            @RequestParam(value = "name", defaultValue = "") String name,
            @RequestParam(value = "description", defaultValue = "") String description,
            @RequestParam(value = "mapPk", required=false) Integer mapPk,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        JqGridDataHelper jqGridHelper = new JqGridDataHelper(request);       
        PaginationFilter filter = jqGridHelper.createFilter(request);
        
        PagedQueryResult<GeoMapLayer> queryResult = layerDAO.search(filter, name, description);
        
        JqGridDataBuilder builder = new JqGridDataBuilder(jqGridHelper.getMaxPerPage(), queryResult.getCount(), jqGridHelper.getRequestedPage());
        
        if (queryResult.getCount() > 0) {
            for (GeoMapLayer layer : queryResult.getList()) {
                JqGridDataRow row = new JqGridDataRow(layer.getId());
                row
                .addValue("name", layer.getName())
                .addValue("description", layer.getDescription());
                builder.addRow(row);
            }
        }
        response.setContentType("application/json");
        response.getWriter().write(builder.toJson());
    }

    // public
    @RequestMapping(value = GET_LAYER_URL, method = RequestMethod.GET)
    public void getLayer(
            @RequestParam(value = PARAM_LAYER_ID, required=true) int layerPk,
            HttpServletRequest request, HttpServletResponse response) throws Exception  {
        GeoMapLayer gml = layerDAO.get(layerPk);
        
        if (gml == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            log.error("layer id not valid : " + layerPk);
            return;
        }
        
        response.setContentType("application/vnd.google-earth.kml+xml");
        response.setHeader("Content-Disposition", "attachment;filename=layer_"+System.currentTimeMillis()+".kml");
        
        if (gml.getLayerSource() == GeoMapLayerSource.KML) {
            if (!StringUtils.hasLength(gml.getManagedFileUUID())) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                log.error("Layer configured to read a KML managed file but no file UUID is assigned to the layer");
                return;
            }
            ManagedFile mf = mfDAO.getManagedFile(gml.getManagedFileUUID());
            if (mf == null) {
                log.error("Can't find managed file with uuid : " + gml.getManagedFileUUID());
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            FileDataSource fsrc = fileService.getFile(mf, mf.getFilename());
            InputStream fileIn = fsrc.getInputStream();
            try {
                IOUtils.copy(fileIn, response.getOutputStream());
            } finally {
                fileIn.close();
            }
        } else if (gml.getLayerSource() == GeoMapLayerSource.SURVEY_KML) {
            if (gml.getSurvey() == null) {
                log.error("Layer configured to use a survey to produce KML but there is no survey assigned to the layer");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            BDRSKMLWriter writer = new BDRSKMLWriter(preferenceDAO,
                    getRequestContext().getServerURL(), request.getParameter("placemark_color"));

            Integer[] mapLayerIds = new Integer[] { gml.getId() };
            User accessingUser = getRequestContext().getUser();                             
            List<Record> recList = getRecordsToDisplay(mapLayerIds, accessingUser, null);
            
            try {
                writer.writeRecordsToKML(accessingUser, recList, response.getOutputStream(), false);
            } catch (JAXBException e) {
                log.error(e);
                throw e;
            } catch (IOException e) {
                log.error(e);
                throw e;
            }

        } else {
            // We are displaying the records using MapServer
        }
    }
    
    /**
     * Get the KML for a single record
     * 
     * @param recordPk
     * @param request
     * @param response
     * @throws Exception
     */
    // public
    @RequestMapping(value = GET_RECORD_URL, method = RequestMethod.GET)
    public void getRecordKml(
            @RequestParam(value = PARAM_RECORD_ID, required=true) int recordPk,
            HttpServletRequest request, HttpServletResponse response) throws Exception  {
        
        response.setContentType("application/vnd.google-earth.kml+xml");
        response.setHeader("Content-Disposition", "attachment;filename=layer_"+System.currentTimeMillis()+".kml");

        BDRSKMLWriter writer = new BDRSKMLWriter(preferenceDAO, getRequestContext().getServerURL(),
                request.getParameter("placemark_color"));
        
        try {
            Record rec = recDAO.getRecord(recordPk);
            List<Record> recordList = new LinkedList<Record>();
            recordList.add(rec);
            writer.writeRecordsToKML(getRequestContext().getUser(), recordList,
                    response.getOutputStream(), true);

        } catch (JAXBException e) {
            log.error(e);
            throw e;
        } catch (IOException e) {
            log.error(e);
            throw e;
        }
    }
    
    // public
    /**
     * Simple web service for retreving feature info in json format
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param featureId Primary key of geo map feature object
     * @throws IOException error writing to stream
     */
    @RequestMapping(value=GET_FEATURE_INFO_BY_ID_SERVICE_URL, method=RequestMethod.GET)
    public void getSingleFeatureInfo(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value=PARAM_FEATURE_ID, required = true) int featureId) throws IOException {
        
        GeoMapFeature gmf = featureDAO.get(featureId);
        if (gmf == null) {
            // return empty json object
            this.writeJson(response, "{}");
            return;
        }
        JsonService jsonService = new JsonService(preferenceDAO,
                getRequestContext().getServerURL());
        this.writeJson(response, jsonService.toJson(gmf, true).toString());
    }
    
    // public
    @RequestMapping(value=GET_FEATURE_SERVICE_URL, method=RequestMethod.GET) 
    public void getFeatureInfo(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value=PARAM_LATITUDE_Y, required = true) double latitude_y,
            @RequestParam(value=PARAM_LONGITUDE_X, required = true) double longitude_x,
            @RequestParam(value=PARAM_BUFFER_KM, required=true) double bufferKm,
            @RequestParam(value=PARAM_MAP_LAYER_ID, required=false) Integer[] mapLayedIds) throws IOException {
        
        if (mapLayedIds == null) {
            mapLayedIds = new Integer[]{};
        }
        
        Point point = geomBuilder.createPoint(longitude_x, latitude_y);
        Geometry spatialFilter = bufferKm > 0d ? geomBuilder.bufferInKm(point, bufferKm) : point;
        
        // 0th page, 10 results per page. Order by geo map feature id.
        PaginationFilter filter = new PaginationFilter(0, 10);
        filter.addSortingCriteria("id", SortOrder.ASCENDING);
        
        List<GeoMapFeature> gmfList = featureDAO.find(mapLayedIds, spatialFilter, filter).getList();
        
        User accessingUser = getRequestContext().getUser();                             
        List<Record> recList = getRecordsToDisplay(mapLayedIds, accessingUser, spatialFilter);

        JsonService jsonService = new JsonService(preferenceDAO, getRequestContext().getServerURL());

        SpatialUtilFactory spatialUtilFactory = new SpatialUtilFactory();
        JSONArray itemArray = new JSONArray();
        for (Record record : recList) {
            AccessControlledRecordAdapter recordAdapter = new AccessControlledRecordAdapter(record, accessingUser);
            itemArray.add(jsonService.toJson(recordAdapter, spatialUtilFactory));
        }
        for (GeoMapFeature f : gmfList) {
            itemArray.add(jsonService.toJson(f));
        }
	
        JSONObject parentObj = new JSONObject();
        parentObj.put(JSON_KEY_ITEMS, itemArray);
        
        writeJson(request, response, parentObj.toString());
    }
    
    private List<Record> getRecordsToDisplay(Integer[] mapLayerIds, User accessingUser, Geometry spatialFilter) {
        List<Record> recList;
        // if logged in
        if (accessingUser != null) {
            // if admin
            if (accessingUser.isAdmin()) {
                // as the admin we don't care about the privacy level or the owner of the record
                recList = recDAO.find(mapLayerIds, spatialFilter, null, null);  
            } else {
                // if standard user
                
                // the user id shouldn't be null but if it is, set it to 0. This will make the
                // find method return all of the non private records for the map layer / spatial filter
                Integer userId = accessingUser.getId() != null ? accessingUser.getId() : 0;
                recList = recDAO.find(mapLayerIds, spatialFilter, false, userId);  
            }    
        } else {
            // if not logged in
            recList = recDAO.find(mapLayerIds, spatialFilter, false, null);  
        }
        return recList;
    }
    
    // time limit of 300 secs / 5 minutes
    public static final int TIME_LIMIT_SECS = 300;
    
    // rough estimate from profiling
    private static final double SEC_PER_SHAPEFILE_ITEM = 0.0019d;  
    private static final double SEC_PER_MIN = 60d;
    
    public static final String JSON_KEY_MESSAGE = "message";
    public static final String JSON_KEY_STATUS = "status";
    public static final String JSON_STATUS_ERROR = "error";
    public static final String JSON_STATUS_WARN = "warn";
    public static final String JSON_STATUS_OK = "ok";
    
    @RolesAllowed( {Role.ROOT, Role.ADMIN} )
    @RequestMapping(value=CHECK_SHAPEFILE_SERVICE_URL, method=RequestMethod.GET) 
    public void checkShapefile(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value=PARAM_MANAGED_FILE_UUID, required=true) String uuid) throws IOException {
        
        JSONObject parentObj = new JSONObject();
        JSONArray messageArray = new JSONArray();
        
        if (!StringUtils.hasLength(uuid)) {
            messageArray.add("Cannot have a blank uuid");
            parentObj.put(JSON_KEY_MESSAGE, messageArray);
            parentObj.put(JSON_KEY_STATUS, JSON_STATUS_ERROR);
            writeJson(request, response, parentObj.toString());
            return;
        }
        ManagedFile mf = mfDAO.getManagedFile(uuid);
        
        if (mf == null) {
            messageArray.add("This file UUID does not exist");
            parentObj.put(JSON_KEY_MESSAGE, messageArray);
            parentObj.put(JSON_KEY_STATUS, JSON_STATUS_ERROR);
            writeJson(request, response, parentObj.toString());
            return;
        }
        
        File file = null;
        try {
            file = fileService.getFile(mf, mf.getFilename()).getFile();
        } catch (IllegalArgumentException e) {
            log.warn("Could not find file", e);
        }
        
        if (file == null) {
            messageArray.add("Could not retrieve the requested file for this UUID");
            parentObj.put(JSON_KEY_MESSAGE, messageArray);
            parentObj.put(JSON_KEY_STATUS, JSON_STATUS_ERROR);
            writeJson(request, response, parentObj.toString());
            return;
        }
        
        ShapeFileReader reader = new ShapeFileReader(file);
        List<Attribute> attributeList = reader.readAttributes();
        List<GeoMapFeature> featuresToAdd = reader.readAsMapFeatures(attributeList);
        
        boolean warn = false;
        
        int numAttr = attributeList.size();
        int numFeatures = featuresToAdd.size();
        double estimatedSec = (numAttr*numFeatures*SEC_PER_SHAPEFILE_ITEM);
        
        if (estimatedSec > TIME_LIMIT_SECS) {
            warn = true;
            Integer estimatedMinutes = ((Double)Math.ceil(estimatedSec / SEC_PER_MIN)).intValue();
            messageArray.add("The file you have selected has been detected to be large, we estimate it will take more than " 
                             + estimatedMinutes.toString() + 
                             " minutes to save.\nAttempting to store this shapefile to the database will take a long time, " 
                             + "and there is no guarantee it will work.\n"
                             + "You may want to reduce the area of interest or detail of your file to make it more manageable.");
        }
        
        if (!reader.isCrsSupported()) {
            warn = true;
            messageArray.add("The file you have selected has an unsupported coordinate reference system (CRS): " 
                             + reader.getCrsCode() 
                             + "\n\nThe supported CRS are " + org.apache.commons.lang.StringUtils.join(reader.getSupportedCrsCodes().toArray(), ", "));
        }
        
        if (!reader.isGeometryValid()) {
            warn = true;
            messageArray.add("The file you have selected has one or more invalid geometry objects. This will cause errors during spatial queries. It is recommended that you clean the data.");
        }
        
        if (warn) {
            parentObj.put(JSON_KEY_MESSAGE, messageArray);
            parentObj.put(JSON_KEY_STATUS, JSON_STATUS_WARN);
            writeJson(request, response, parentObj.toString());
            return;
        }
        
        // we've got through all of that so...
        parentObj.put(JSON_KEY_STATUS, JSON_STATUS_OK);
        writeJson(request, response, parentObj.toString());
    }
    
    /**
     * Will delete the geo map layer plus any assigned layers the geo map layer was 
     * a part of.
     * 
     * @param request
     * @param response
     * @param layerPk
     * @return
     */
    @RolesAllowed( {Role.ROOT, Role.ADMIN} )
    @RequestMapping(value=DELETE_LAYER_URL, method=RequestMethod.POST)
    public ModelAndView deleteLayer(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value=GEO_MAP_LAYER_PK_SAVE, required=true) int layerPk) {
        
        List<AssignedGeoMapLayer> assignedLayerList = layerDAO.getAssignedLayerByLayerId(layerPk);
        layerDAO.delete(assignedLayerList);
        List<GeoMapFeature> featuresToDelete = featureDAO.find(layerPk);
        for (GeoMapFeature f : featuresToDelete) {
            featureDAO.deleteCascade(f);
        }
        layerDAO.delete(layerPk);
        
        ModelAndView mv = new ModelAndView(new PortalRedirectView(LISTING_URL, true, true, false));
        getRequestContext().addMessage("bdrs.geoMapLayer.delete.success");
        return mv;
    }
}
