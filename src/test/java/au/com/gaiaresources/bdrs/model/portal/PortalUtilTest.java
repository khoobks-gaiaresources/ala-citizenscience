package au.com.gaiaresources.bdrs.model.portal;

import java.io.IOException;
import java.util.List;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.db.FilterManager;
import au.com.gaiaresources.bdrs.file.FileService;
import au.com.gaiaresources.bdrs.model.portal.Portal;
import au.com.gaiaresources.bdrs.model.preference.Preference;
import au.com.gaiaresources.bdrs.model.preference.PreferenceCategory;
import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.bdrs.model.threshold.Threshold;
import au.com.gaiaresources.bdrs.model.threshold.ThresholdDAO;
import au.com.gaiaresources.bdrs.util.ModerationUtil;

public class PortalUtilTest extends AbstractControllerTest {

    @Autowired
    private PreferenceDAO prefDAO;
    
    @Autowired
    private ThresholdDAO thresholdDAO;
    
    private Portal p1;
    private Portal p2;

    private Logger log = Logger.getLogger(getClass());

    @Before
    public void setup() throws Exception {        
        
        FilterManager.disablePortalFilter(getSession());
        
        p1 = new Portal();
        p1.setName("portal 1");
        
        p2 = new Portal();
        p2.setName("portal 2");
                
        // to avoid the saving logic inside of portalDAO.
        getSession().save(p1);
        getSession().save(p2);
    }
    
    @After
    public void teardown() {
        FilterManager.setPortalFilter(getSession(), defaultPortal);
    }
    
    /*
     * IMPORTANT : this test relies on the settings inside preferences.json which is a production file. Of course this can cause problems
     * if the preferences.json file is edited (namely if existing items are removed, or additional items are added to the preference category
     * that I delete in this test. If stuff starts breaking unexpectedly - check for changes in preferences.json
     */
    @Test
    public void testPortalPreferenceLazyInit() throws IOException {
        
        Preference fileStorePref = prefDAO.getPreferenceByKey(getSession(), FileService.FILE_STORE_LOCATION_PREFERENCE_KEY, defaultPortal);
        
        PreferenceCategory prefCatToDelete = fileStorePref.getPreferenceCategory();
        
        Assert.assertNotNull("pref should exist", fileStorePref);
        prefDAO.delete(fileStorePref);
        Assert.assertNull("pref should now be deleted", prefDAO.getPreferenceByKey(getSession(), FileService.FILE_STORE_LOCATION_PREFERENCE_KEY, defaultPortal));
        
        String lsidAuthority = "lsidwoooo";
        Preference lsidPref = prefDAO.getPreferenceByKey(getSession(), "lsid.authority", defaultPortal);
        lsidPref.setValue(lsidAuthority);
        prefDAO.save(lsidPref);
        
        prefDAO.delete(getSession(), prefCatToDelete);
        Assert.assertNull("pref cat should be deleted", prefDAO.getPreferenceCategoryByName(getSession(), prefCatToDelete.getName(), defaultPortal));
        
        String prefCatDesc = "pref cat desc woooo";
        PreferenceCategory prefCatToEdit = prefDAO.getPreferenceCategoryByName(getSession(), "category.apikey", defaultPortal);
        prefCatToEdit.setDescription(prefCatDesc);
        prefDAO.save(getSession(), prefCatToEdit);
        
        Assert.assertNotNull("portal 1 should exist", p1.getId());
        Assert.assertNotNull("portal 2 should exist", p2.getId());
        
        Assert.assertNull("pref does not exist yet", prefDAO.getPreferenceByKey(getSession(), FileService.FILE_STORE_LOCATION_PREFERENCE_KEY, p1));
        Assert.assertNull("pref cat does not exist yet", prefDAO.getPreferenceCategoryByName(getSession(), "category.userManagement", p1));
        
        Assert.assertNull("pref does not exist yet", prefDAO.getPreferenceByKey(getSession(), FileService.FILE_STORE_LOCATION_PREFERENCE_KEY, p2));
        Assert.assertNull("pref cat does not exist yet", prefDAO.getPreferenceCategoryByName(getSession(), "category.userManagement", p2));
        
        PortalUtil.initPortalPreferences(getSession(), p1, true);
        PortalUtil.initPortalPreferences(getSession(), p2, false);
        PortalUtil.initPortalPreferences(getSession(), defaultPortal, true);
        
        Assert.assertNotNull("pref should be initialised", prefDAO.getPreferenceByKey(getSession(), FileService.FILE_STORE_LOCATION_PREFERENCE_KEY, p1));
        Assert.assertNotNull("pref cat should be initialised", prefDAO.getPreferenceCategoryByName(getSession(), "category.userManagement", p1));
        
        Assert.assertNotNull("pref should be initialised", prefDAO.getPreferenceByKey(getSession(), FileService.FILE_STORE_LOCATION_PREFERENCE_KEY, p2));
        Assert.assertNotNull("pref cat should be initialised", prefDAO.getPreferenceCategoryByName(getSession(), "category.userManagement", p2));
        
        // make sure lazy init has occured for the default portal
        Assert.assertNotNull("pref should be lazy initialised", prefDAO.getPreferenceByKey(getSession(), FileService.FILE_STORE_LOCATION_PREFERENCE_KEY, defaultPortal));
        
        Preference lsidPref2 = prefDAO.getPreferenceByKey(getSession(), "lsid.authority", defaultPortal);
        Assert.assertEquals("pref value should not be changed by lazy init", lsidAuthority, lsidPref2.getValue());
        
        Assert.assertFalse("pref cat desc should be changed by lazy init", prefCatDesc.equals(
                            prefDAO.getPreferenceCategoryByName(getSession(), prefCatToEdit.getName(), defaultPortal).getDescription()));
   
        Assert.assertNotNull("pref cat should be lazy init", prefDAO.getPreferenceCategoryByName(getSession(), prefCatToDelete.getName(), defaultPortal));                                                                                                          
    }
    
    /**
     * Tests that the default moderation threshold is created
     */
    @Test
    public void testCreateModerationThreshold() {
        List<Threshold> defaultTholds = thresholdDAO.getThresholdsByName(getSession(), ModerationUtil.MODERATION_THRESHOLD_NAME, p1);
        Assert.assertTrue("There shouldn't be any default thresholds!", defaultTholds.isEmpty());
        PortalUtil.initModerationThreshold(getSession(), p1);
        defaultTholds = thresholdDAO.getThresholdsByName(getSession(), ModerationUtil.MODERATION_THRESHOLD_NAME, p1);
        Assert.assertFalse("There should be a default threshold!", defaultTholds.isEmpty());
        Assert.assertEquals("There can be only 1 default threshold", 1, defaultTholds.size());
        Assert.assertEquals("The default threshold does not have the default name!", ModerationUtil.MODERATION_THRESHOLD_NAME, defaultTholds.get(0).getName());
    }
}
