package au.com.gaiaresources.bdrs.model.preference;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.ParamDef;

import au.com.gaiaresources.bdrs.db.impl.PortalPersistentImpl;

/*
 * A key value pair representing a configuration item.
 */
@Entity
@FilterDef(name=PortalPersistentImpl.PORTAL_FILTER_NAME, parameters=@ParamDef( name="portalId", type="integer" ) )
@Filter(name=PortalPersistentImpl.PORTAL_FILTER_NAME, condition=":portalId = PORTAL_ID")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "PREFERENCE")
@AttributeOverride(name = "id", column = @Column(name = "PREFERENCE_ID"))
public class Preference extends PortalPersistentImpl {
    
    public static final String TAXON_PROFILE_TEMPLATE = "taxon.profile.template";
    public static final String GOOGLE_MAP_KEY_PREFIX = "google.map.key.";
    /** Identifies the preference that configures how search results are displayed by default on the My Sightings page */
    public static final String MY_SIGHTINGS_DEFAULT_VIEW_KEY = "searchResults.mySightings.defaultToMapView";
    /** Identifies the preference that configures how search results are displayed by default on the Advanced Review page */
    public static final String ADVANCED_REVIEW_DEFAULT_VIEW_KEY = "searchResults.advancedReview.defaultToMapView";

    /** Identifies the Preference that controls whether species names are displayed using the scientific or common name */
    public static final String SHOW_SCIENTIFIC_NAME_KEY = "taxon.showScientificName";

    /** The default value for the taxon.showScientificName preference. */
    public static final boolean DEFAULT_SHOW_SCIENTIFIC_NAME = false;

    @SuppressWarnings("unused")
    private Logger log = Logger.getLogger(getClass());

    private String key;
    private String value;
    private boolean locked = false;
    private boolean isRequired = false;
    private String description;
    private PreferenceCategory preferenceCategory;

    @Column(name = "KEY", nullable = false)
    @Index(name = "PREFERENCE_KEY_INDEX")
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Column(name = "VALUE", nullable = false)
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Column(name = "LOCKED", nullable = false)
    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @Column(name = "DESCRIPTION", nullable = false)
    @Lob  // makes a 'text' type in the database
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    @Column(name ="ISREQUIRED", nullable = false)
    public boolean getIsRequired() {
        return isRequired;
    }
    
    public void setIsRequired(boolean value) {
        isRequired = value;
    }

    @ManyToOne
    @JoinColumn(name = "CATEGORY_ID", nullable = false)
    public PreferenceCategory getPreferenceCategory() {
        return preferenceCategory;
    }

    public void setPreferenceCategory(PreferenceCategory preferenceCategory) {
        this.preferenceCategory = preferenceCategory;
    }
}
