package au.com.gaiaresources.bdrs.model.metadata;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import au.com.gaiaresources.bdrs.annotation.CompactAttribute;
import org.apache.commons.lang.StringEscapeUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.ParamDef;
import org.hibernate.search.annotations.Field;

import au.com.gaiaresources.bdrs.controller.attribute.RecordPropertyAttributeFormField;
import au.com.gaiaresources.bdrs.db.impl.PortalPersistentImpl;

@Entity
@FilterDef(name=PortalPersistentImpl.PORTAL_FILTER_NAME, parameters=@ParamDef( name="portalId", type="integer" ) )
@Filter(name=PortalPersistentImpl.PORTAL_FILTER_NAME, condition=":portalId = PORTAL_ID")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "METADATA")
@AttributeOverride(name = "id", column = @Column(name = "ID"))
public class Metadata extends PortalPersistentImpl {
    public static final String FILE_URL_TMPL = "className=%s&id=%d&fileName=%s";

    // User Keys
    public static final String SCHOOL_NAME_KEY = "schoolname";
    public static final String SCHOOL_SUBURB_KEY = "school suburb";
    
    public static final String AGE = "age";
    public static final String HEAR_ABOUT = "hear about";
    public static final String TELEPHONE = "telephone";
    public static final String CLIMATEWATCH_USERNAME = "climatewatch username";
    
    public static final String TITLE = "title";
    public static final String GROUP_NAME = "group_name";
    public static final String ADDRESS_LINE_1 = "address_line_1";
    public static final String ADDRESS_LINE_2 = "address_line_2";
    public static final String ADDRESS_LINE_3 = "address_line_3";
    public static final String SUBURB = "suburb";
    public static final String STATE = "state";
    public static final String COUNTRY = "country";
    public static final String POSTCODE = "postcode";
    public static final String HOME_PHONE = "home_phone";
    public static final String MOBILE_PHONE = "mobile_phone";
    public static final String WORK_PHONE = "work_phone";
    public static final String PROMO_CODE = "promo_code";
    public static final String BA_MEMBER_NO = "ba_member_no";
    public static final String BIRTH_YEAR = "birth_year";
    public static final String SUBSCRIBE_NEWS = "subscribe_news";
    public static final String REQUEST_INFO = "request_info";
    
    // Record Keys
    // The original id when the record was uploaded from a spreadsheet.
    public static final String RECORD_UPLOAD_ORIG_ID = "record_upload_orig_id";
    public static final String RECORD_IS_MASTER = "record_is_master";
    public static final String RECORD_NOT_DUPLICATE = "record_is_not_duplicate";
    // Survey Keys
    /**
     * Form renderer type for the survey
     */
    public static final String FORM_RENDERER_TYPE = "FormRendererType";
    /**
     * Logo for the survey
     */
    public static final String SURVEY_LOGO = "SurveyLogo";
    /**
     * CUSTOM css file for the survey form(s)
     */
    public static final String SURVEY_CSS = "SurveyCss";
    /**
     * CUSTOM js file for the survey form
     */
    public static final String SURVEY_JS = "SurveyJs";
    /**
     * Can the survey only have pre defined locations
     */
    public static final String PREDEFINED_LOCATIONS_ONLY = "PredefinedLocationsOnly";
    /**
     * Default record visibility for the survey
     */
    public static final String DEFAULT_RECORD_VIS = "defaultRecordVisbility";
    /**
     * Is the record visibility modifiable by users for the survey
     */
    public static final String RECORD_VIS_MODIFIABLE = "recordVisibilityModifiable";
    /**
     * Is a default census method provided for the survey
     */
    public static final String DEFAULT_CENSUS_METHOD_FOR_SURVEY = "defaultCensusMethodProvidedForSurvey";
    /**
     * The post submit form action for the survey
     */
    public static final String FORM_SUBMIT_ACTION = "FormSubmitAction";
    
    // Moderation Email keys
    public static final String MODERATION_REQUIRED_EMAIL = "ModerationRequiredEmail";
    public static final String MODERATION_PERFORMED_EMAIL = "ModerationPerformedEmail";
    
    // SpeciesProfile Keys
    public static final String SCIENTIFIC_NAME_SOURCE_DATA_ID = "ScientificNameSourceDataId";
    public static final String COMMON_NAME_SOURCE_DATA_ID = "CommonNameSourceDataId";
    public static final String PUBLICATION_SOURCE_DATA_ID = "PublicationSourceDataId";
    
    // IndicatorSpecies Keys
    public static final String TAXON_SOURCE = "TaxonSource";
    public static final String TAXON_FAMILY = "TaxonFamily";
    public static final String TAXON_KINGDOM = "TaxonKingdom";
    
    // Census Method Keys
    /**
     * Can we define a point on the map for this census method
     */
    public static final String CENSUS_METHOD_POINT = "censusMethodPoint";
    /**
     * Can we define a line on the map for this census method
     */
    public static final String CENSUS_METHOD_LINE = "censusMethodLine";
    /**
     * Can we define a polygon on the map for this census method
     */
    public static final String CENSUS_METHOD_POLYGON = "censusMethodPolygon";
    
    // Record Property
    /**
     * Template that is used to generate Metadata keys. Metadata are keyed
     * against the property name which results in keys such as
     * <i>Record.species</i> or <i>Record.location</i>.
     */
    public static final String RECORD_PROPERTY_FIELD_METADATA_KEY_TEMPLATE = "Record.%s";
    
    public static final String RECORD_CLIENT_ID_KEY = "Record.ClientID";
    public static final String RECORD_GROUP_CLIENT_ID_KEY = "RecordGroup.ClientID";
    
    /**
     * The unique key for the metadata item storing the client ID for an 
     * uploaded location.
     */
    public static final String LOCATION_CLIENT_ID_KEY = "Location.ClientID";
    
    /**
     * The primary key of a user's default user location.
     */
    public static final String DEFAULT_LOCATION_ID = "DefaultLocationId";

    /** 
     * Whether users can comment on Records created against a particular Survey 
     */
    public static final String COMMENTS_ENABLED_FOR_SURVEY = "commentsEnabledForSurvey";

    /**
     * The default zoom level of the map.
     */
    public static final String MAP_DEFAULT_ZOOM = "Survey.MapZoom";
    /**
     * The default center of the map.
     */
    public static final String MAP_DEFAULT_CENTER = "Survey.MapCenter";
    
    private String key;
    private String value;

    public Metadata() {
    	super();
    }
    
    public Metadata(String key, String value) {
    	super();
    	this.key = key;
    	this.value = value;
    }

    @CompactAttribute
    @Column(name="KEY", nullable=false)
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @CompactAttribute
    @Column(name="VALUE", nullable=false)
    @Index(name="metadata_value_index")
    @Field
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
    @Transient
    public String getFileURL() {
        try {
            return String.format(FILE_URL_TMPL, URLEncoder.encode(getClass()
                    .getCanonicalName(), "UTF-8"), getId(), URLEncoder.encode(
                    getValue(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            return String.format(FILE_URL_TMPL, StringEscapeUtils
                    .escapeHtml(getClass().getCanonicalName()), getId(),
                    StringEscapeUtils.escapeHtml(getValue()));
        }
    }
}
