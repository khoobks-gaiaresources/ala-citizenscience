package au.com.gaiaresources.bdrs.controller.theme;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.file.FileService;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.file.ManagedFile;
import au.com.gaiaresources.bdrs.model.file.ManagedFileDAO;
import au.com.gaiaresources.bdrs.model.portal.Portal;
import au.com.gaiaresources.bdrs.model.theme.*;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.theme.ThemeService;
import au.com.gaiaresources.bdrs.util.ZipUtils;
import edu.emory.mathcs.backport.java.util.Collections;
import junit.framework.Assert;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.web.servlet.ModelAndView;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ThemeControllerTest extends AbstractControllerTest {
    private static final int IMAGE_WIDTH = 128;
    private static final int IMAGE_HEIGHT = 128;
    
    private static final String TEST_CSS_FILE_PATH= "css/style.css";
    private static final String TEST_CSS_RAW_CONTENT = "#horiz-menu {background-image: url(${asset}../images/blockdefault.gif);background-repeat: ${style.background.repeat};}";
    private static final String TEST_CSS_DEFAULT_CONTENT_TMPL = "#horiz-menu {background-image: url("+REQUEST_CONTEXT_PATH+"/files/download.htm?className=au.com.gaiaresources.bdrs.model.theme.Theme&id=%d&fileName=processed/../images/blockdefault.gif);background-repeat: repeat-x;}";
    private static final String TEST_CSS_CUSTOM_CONTENT_TMPL = "#horiz-menu {background-image: url("+REQUEST_CONTEXT_PATH+"/files/download.htm?className=au.com.gaiaresources.bdrs.model.theme.Theme&id=%d&fileName=processed/../images/blockdefault.gif);background-repeat: no-repeat;}";
    
    private static final String TEST_CSS_MODIFIED_RAW_CONTENT = ".horizontal-menu {background-image: url(${asset}../images/default.gif);background-repeat: ${style.background.repeat};}";
    private static final String TEST_CSS_MODIFIED_DEFAULT_CONTENT_TMPL = ".horizontal-menu {background-image: url("+REQUEST_CONTEXT_PATH+"/files/download.htm?className=au.com.gaiaresources.bdrs.model.theme.Theme&id=%d&fileName=processed/../images/default.gif);background-repeat: repeat-x;}";
    
    private static final String TEST_TEMPLATE_FILE_PATH = "templates/template.vm"; 
    private static final String TEST_TEMPLATE_RAW_CONTENT = "<a href=\"${portalContextPath}/authenticated/redirect.htm\" class=\"nounder\"><img src=\"${asset}images/wild_backyards.jpg\" id=\"logo\"/></a>";
    // we now expect $asset to be overwritten at the unzipping/processing stage so the 2 strings below becomes...
    // note there is no context path. I assume this is because there is no context path when running these tests
    private static final String TEST_TEMPLATE_DEFAULT_CONTENT = "<a href=\"${portalContextPath}/authenticated/redirect.htm\" class=\"nounder\"><img src=\""+REQUEST_CONTEXT_PATH+"/files/download.htm?className=au.com.gaiaresources.bdrs.model.theme.Theme&id=%d&fileName=processed/images/wild_backyards.jpg\" id=\"logo\"/></a>";
    private static final String TEST_TEMPLATE_CUSTOM_CONTENT = "<a href=\"${portalContextPath}/authenticated/redirect.htm\" class=\"nounder\"><img src=\""+REQUEST_CONTEXT_PATH+"/files/download.htm?className=au.com.gaiaresources.bdrs.model.theme.Theme&id=%d&fileName=processed/images/wild_backyards.jpg\" id=\"logo\"/></a>";
    
    private static final String TEST_JS_FILE_PATH = "js/javascript.js";
    private static final String TEST_JS_RAW_CONTENT = "function foobar(a){ var b=${js.foobar.b};var c=a+b;return c;}";
    private static final String TEST_JS_DEFAULT_CONTENT = "function foobar(a){ var b=3;var c=a+b;return c;}";
    private static final String TEST_JS_CUSTOM_CONTENT = "function foobar(a){ var b=8;var c=a+b;return c;}";
    
    private static final Map<String, String> TEST_THEME_PAGE_1;
    private static final Map<String, String> TEST_THEME_PAGE_2;
    
    private static final String TEST_IMAGE_FILE_PATH = "images/testImage.png";
    
    private static final Map<String, String> DEFAULT_CONFIG_VALUES;
    private static final Map<String, String> CUSTOM_CONFIG_VALUES;
    
    static {
        Map<String, String> defaultTemp = new HashMap<String, String>();
        defaultTemp.put("style.background.repeat", "repeat-x");
        defaultTemp.put("js.foobar.b", "3");
        
        DEFAULT_CONFIG_VALUES = Collections.unmodifiableMap(defaultTemp);
        
        Map<String, String> customTemp = new HashMap<String, String>();
        customTemp.put("style.background.repeat", "no-repeat");
        customTemp.put("js.foobar.b", "8");
        
        CUSTOM_CONFIG_VALUES = Collections.unmodifiableMap(customTemp);
        
        Map<String, String> page1 = new HashMap<String, String>();
        page1.put("key", "mySightings");
        page1.put("title", "Simple Review");
        page1.put("description", "simple review description");
        TEST_THEME_PAGE_1 = Collections.unmodifiableMap(page1);
        
        Map<String, String> page2 = new HashMap<String, String>();
        page2.put("key", "userLocations");
        page2.put("title", "My Locations");
        page2.put("description", "my locations description");
        TEST_THEME_PAGE_2 = Collections.unmodifiableMap(page2);
    }
    
    
    @Autowired
    private ManagedFileDAO managedFileDAO;
    @Autowired
    private ThemeDAO themeDAO;
    @Autowired
    private FileService fileService;

    @Test
    public void testListing() throws Exception {
        login("root", "password", new String[] { Role.ROOT });
        Portal portal = getRequestContext().getPortal();

        request.setMethod("GET");
        request.setRequestURI("/bdrs/root/theme/listing.htm");
        request.setParameter("portalId", portal.getId().toString());

        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "themeListing");
        ModelAndViewAssert.assertModelAttributeValue(mv, "portalId", portal.getId());
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "themeList");
    }
    
    @Test
    public void testEdit() throws Exception {
        login("root", "password", new String[] { Role.ROOT });
        Portal portal = getRequestContext().getPortal();

        request.setMethod("GET");
        request.setRequestURI("/bdrs/root/theme/edit.htm");
        request.setParameter("portalId", portal.getId().toString());
        
        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "themeEdit");
        ModelAndViewAssert.assertModelAttributeValue(mv, "portalId", portal.getId());
        Assert.assertEquals(0, ((List<ThemeFile>)mv.getModel().get("themeFileList")).size());
        Assert.assertNull(((Theme)mv.getModel().get("editTheme")).getId());
        Assert.assertTrue((Boolean)mv.getModel().get("editAsRoot"));
    }
    
    @Test
    public void testEditSubmit() throws Exception {
        ManagedFile mf = new ManagedFile();
        mf.setContentType("application/zip");
        mf.setFilename("testTheme.zip");
        mf.setCredit("");
        mf.setLicense("");
        mf.setDescription("");
        managedFileDAO.save(mf);
        fileService.createFile(mf.getClass(), mf.getId(), mf.getFilename(), createTestTheme(DEFAULT_CONFIG_VALUES));
        
        login("root", "password", new String[] { Role.ROOT });
        Portal portal = getRequestContext().getPortal();
        
        request.setMethod("POST");
        request.setRequestURI("/bdrs/root/theme/edit.htm");
        request.setParameter("portalPk", portal.getId().toString());
        request.setParameter("name", "Test Theme");
        request.setParameter("themeFileUUID", mf.getUuid());
        request.setParameter("active", "true");
        
        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertModelAttributeValue(mv, "portalId", portal.getId());
        assertRedirect(mv, "/bdrs/root/theme/edit.htm");
        
        Theme theme = themeDAO.getTheme((Integer)mv.getModel().get("themeId"));
        Assert.assertEquals(request.getParameter("name"), theme.getName());
        Assert.assertEquals(request.getParameter("themeFileUUID"), theme.getThemeFileUUID());
        Assert.assertEquals(Boolean.parseBoolean(request.getParameter("active")), theme.isActive());
        
        Assert.assertEquals(DEFAULT_CONFIG_VALUES.size(), theme.getThemeElements().size());
        for(ThemeElement elem : theme.getThemeElements()) {
            String expectedValue = DEFAULT_CONFIG_VALUES.get(elem.getKey());
            Assert.assertEquals(expectedValue, elem.getDefaultValue());
            Assert.assertEquals(expectedValue, elem.getCustomValue());
        }
        
        // Check that the save theme files have been decompressed and processed correctly.
        // Check the raw file directory.
        File rawDir = fileService.getTargetDirectory(theme, Theme.THEME_DIR_RAW, false);
        Assert.assertEquals(TEST_CSS_RAW_CONTENT, FileUtils.fileRead(new File(rawDir, TEST_CSS_FILE_PATH)));
        Assert.assertEquals(TEST_TEMPLATE_RAW_CONTENT, FileUtils.fileRead(new File(rawDir, TEST_TEMPLATE_FILE_PATH)));
        Assert.assertEquals(TEST_JS_RAW_CONTENT, FileUtils.fileRead(new File(rawDir, TEST_JS_FILE_PATH)));
        BufferedImage rawImg = ImageIO.read(new File(rawDir, TEST_IMAGE_FILE_PATH));
        Assert.assertEquals(IMAGE_WIDTH, rawImg.getWidth());
        Assert.assertEquals(IMAGE_HEIGHT, rawImg.getHeight());
        
        // Check the processed file directory
        File processedDir = fileService.getTargetDirectory(theme, Theme.THEME_DIR_PROCESSED, false);
        Assert.assertEquals(String.format(TEST_CSS_DEFAULT_CONTENT_TMPL, theme.getId()).trim(), FileUtils.fileRead(new File(processedDir, TEST_CSS_FILE_PATH)).trim());
        Assert.assertEquals(String.format(TEST_TEMPLATE_DEFAULT_CONTENT, theme.getId()).trim(), FileUtils.fileRead(new File(processedDir, TEST_TEMPLATE_FILE_PATH)).trim());
        Assert.assertEquals(TEST_JS_DEFAULT_CONTENT, FileUtils.fileRead(new File(processedDir, TEST_JS_FILE_PATH)).trim());
        BufferedImage processedImg = ImageIO.read(new File(processedDir, TEST_IMAGE_FILE_PATH));
        Assert.assertEquals(IMAGE_WIDTH, processedImg.getWidth());
        Assert.assertEquals(IMAGE_HEIGHT, processedImg.getHeight());
        
        assertThemePage(theme.getId().intValue());
    }
    
    @Test
    public void testEditCustomSubmit() throws Exception {
        ManagedFile mf = new ManagedFile();
        mf.setContentType("application/zip");
        mf.setFilename("testTheme.zip");
        mf.setCredit("");
        mf.setLicense("");
        mf.setDescription("");
        managedFileDAO.save(mf);
        fileService.createFile(mf.getClass(), mf.getId(), mf.getFilename(), createTestTheme(DEFAULT_CONFIG_VALUES));
        
        Portal portal = getRequestContext().getPortal();
        
        Map<String, ThemeElement> themeElementMap = new HashMap<String, ThemeElement>();
        List<ThemeElement> themeElements = new ArrayList<ThemeElement>(DEFAULT_CONFIG_VALUES.size());
        for(Map.Entry<String, String> entry : DEFAULT_CONFIG_VALUES.entrySet()) {
            ThemeElement te = new ThemeElement();
            te.setKey(entry.getKey());
            te.setDefaultValue(entry.getValue());
            te.setCustomValue(entry.getValue());
            te.setType(ThemeElementType.TEXT);
            te = themeDAO.save(te);
            themeElements.add(te);
            themeElementMap.put(te.getKey(), te);
        }
        
        Theme theme = new Theme();
        theme.setActive(true);
        theme.setName("Test Theme");
        theme.setThemeFileUUID(mf.getUuid());
        theme.setCssFiles(Arrays.asList(new String[]{TEST_CSS_FILE_PATH}));
        theme.setJsFiles(Arrays.asList(new String[]{TEST_JS_FILE_PATH}));
        theme.setPortal(portal);
        theme.setThemeElements(themeElements);
        theme = themeDAO.save(theme);
        
        ZipUtils.decompressToDir(new ZipFile(fileService.getFile(mf, mf.getFilename()).getFile()), 
                                 fileService.getTargetDirectory(theme, Theme.THEME_DIR_RAW, true));
        
        login("root", "password", new String[] { Role.ROOT });
                
        request.setMethod("POST");
        request.setRequestURI("/bdrs/root/theme/edit.htm");
        request.setParameter("portalPk", portal.getId().toString());
        request.setParameter("themePk", theme.getId().toString());
        request.setParameter("name", theme.getName());
        request.setParameter("themeFileUUID", theme.getThemeFileUUID());
        request.setParameter("active", String.valueOf(theme.isActive()));
        for(Map.Entry<String, String> customEntry : CUSTOM_CONFIG_VALUES.entrySet()) {
            ThemeElement te = themeElementMap.get(customEntry.getKey());
            request.setParameter(String.format(ThemeController.THEME_ELEMENT_CUSTOM_VALUE_TEMPLATE, te.getId()),
                                 customEntry.getValue());
        }
        
        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertModelAttributeValue(mv, "portalId", portal.getId());
        assertRedirect(mv, "/bdrs/root/theme/edit.htm");
        
        Theme actualTheme = themeDAO.getTheme((Integer)mv.getModel().get("themeId"));
        Assert.assertEquals(theme.getName(), actualTheme.getName());
        Assert.assertEquals(theme.getThemeFileUUID(), actualTheme.getThemeFileUUID());
        Assert.assertEquals(theme.isActive(), actualTheme.isActive());
        
        Assert.assertEquals(DEFAULT_CONFIG_VALUES.size(), actualTheme.getThemeElements().size());
        for(ThemeElement elem : actualTheme.getThemeElements()) {
            String expectedDefaultValue = DEFAULT_CONFIG_VALUES.get(elem.getKey());
            Assert.assertEquals(expectedDefaultValue, elem.getDefaultValue());
            
            String expectedCustomValue = CUSTOM_CONFIG_VALUES.get(elem.getKey());
            Assert.assertEquals(expectedCustomValue, elem.getCustomValue());
        }
        
        // Check that the save theme files have been decompressed and processed correctly.
        // Check the raw file directory.
        File rawDir = fileService.getTargetDirectory(actualTheme, Theme.THEME_DIR_RAW, false);
        Assert.assertEquals(TEST_CSS_RAW_CONTENT, FileUtils.fileRead(new File(rawDir, TEST_CSS_FILE_PATH)));
        Assert.assertEquals(TEST_TEMPLATE_RAW_CONTENT, FileUtils.fileRead(new File(rawDir, TEST_TEMPLATE_FILE_PATH)));
        Assert.assertEquals(TEST_JS_RAW_CONTENT, FileUtils.fileRead(new File(rawDir, TEST_JS_FILE_PATH)));
        BufferedImage rawImg = ImageIO.read(new File(rawDir, TEST_IMAGE_FILE_PATH));
        Assert.assertEquals(IMAGE_WIDTH, rawImg.getWidth());
        Assert.assertEquals(IMAGE_HEIGHT, rawImg.getHeight());
        
        // Check the processed file directory
        File processedDir = fileService.getTargetDirectory(actualTheme, Theme.THEME_DIR_PROCESSED, false);
        Assert.assertEquals(String.format(TEST_CSS_CUSTOM_CONTENT_TMPL, actualTheme.getId()).trim(), FileUtils.fileRead(new File(processedDir, TEST_CSS_FILE_PATH)).trim());
        Assert.assertEquals(String.format(TEST_TEMPLATE_CUSTOM_CONTENT, actualTheme.getId()).trim(), FileUtils.fileRead(new File(processedDir, TEST_TEMPLATE_FILE_PATH)).trim());
        Assert.assertEquals(TEST_JS_CUSTOM_CONTENT, FileUtils.fileRead(new File(processedDir, TEST_JS_FILE_PATH)).trim());
        BufferedImage processedImg = ImageIO.read(new File(processedDir, TEST_IMAGE_FILE_PATH));
        Assert.assertEquals(IMAGE_WIDTH, processedImg.getWidth());
        Assert.assertEquals(IMAGE_HEIGHT, processedImg.getHeight());
    }
    
    @Test
    public void testEditThemeFile() throws Exception {
        ManagedFile mf = new ManagedFile();
        mf.setContentType("application/zip");
        mf.setFilename("testTheme.zip");
        mf.setCredit("");
        mf.setLicense("");
        mf.setDescription("");
        managedFileDAO.save(mf);
        fileService.createFile(mf.getClass(), mf.getId(), mf.getFilename(), createTestTheme(DEFAULT_CONFIG_VALUES));
        
        Portal portal = getRequestContext().getPortal();
        
        Map<String, ThemeElement> themeElementMap = new HashMap<String, ThemeElement>();
        List<ThemeElement> themeElements = new ArrayList<ThemeElement>(DEFAULT_CONFIG_VALUES.size());
        for(Map.Entry<String, String> entry : DEFAULT_CONFIG_VALUES.entrySet()) {
            ThemeElement te = new ThemeElement();
            te.setKey(entry.getKey());
            te.setDefaultValue(entry.getValue());
            te.setCustomValue(entry.getValue());
            te.setType(ThemeElementType.TEXT);
            te = themeDAO.save(te);
            themeElements.add(te);
            themeElementMap.put(te.getKey(), te);
        }
        
        Theme theme = new Theme();
        theme.setActive(true);
        theme.setName("Test Theme");
        theme.setThemeFileUUID(mf.getUuid());
        theme.setCssFiles(Arrays.asList(new String[]{TEST_CSS_FILE_PATH}));
        theme.setJsFiles(Arrays.asList(new String[]{TEST_JS_FILE_PATH}));
        theme.setPortal(portal);
        theme.setThemeElements(themeElements);
        theme = themeDAO.save(theme);
        
        ZipUtils.decompressToDir(new ZipFile(fileService.getFile(mf, mf.getFilename()).getFile()), 
                                 fileService.getTargetDirectory(theme, Theme.THEME_DIR_RAW, true));
        
        login("root", "password", new String[] { Role.ROOT });
        
        request.setMethod("GET");
        request.setRequestURI("/bdrs/root/theme/editThemeFile.htm");
        request.setParameter("themeId", theme.getId().toString());
        request.setParameter("themeFileName", TEST_CSS_FILE_PATH);
        
        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "themeFileEdit");
        Assert.assertEquals(theme.getId(), ((Theme)(mv.getModel().get("editTheme"))).getId());
        Assert.assertEquals(request.getParameter("themeFileName"), mv.getModel().get("themeFileName"));
        Assert.assertEquals(TEST_CSS_RAW_CONTENT.trim(), mv.getModel().get("content").toString().trim());
    }
    
    @Test
    public void testEditThemeFileSubmit() throws Exception {
        ManagedFile mf = new ManagedFile();
        mf.setContentType("application/zip");
        mf.setFilename("testTheme.zip");
        mf.setCredit("");
        mf.setLicense("");
        mf.setDescription("");
        managedFileDAO.save(mf);
        fileService.createFile(mf.getClass(), mf.getId(), mf.getFilename(), createTestTheme(DEFAULT_CONFIG_VALUES));
        
        Portal portal = getRequestContext().getPortal();
        
        Map<String, ThemeElement> themeElementMap = new HashMap<String, ThemeElement>();
        List<ThemeElement> themeElements = new ArrayList<ThemeElement>(DEFAULT_CONFIG_VALUES.size());
        for(Map.Entry<String, String> entry : DEFAULT_CONFIG_VALUES.entrySet()) {
            ThemeElement te = new ThemeElement();
            te.setKey(entry.getKey());
            te.setDefaultValue(entry.getValue());
            te.setCustomValue(entry.getValue());
            te.setType(ThemeElementType.TEXT);
            te = themeDAO.save(te);
            themeElements.add(te);
            themeElementMap.put(te.getKey(), te);
        }
        
        Theme theme = new Theme();
        theme.setActive(true);
        theme.setName("Test Theme");
        theme.setThemeFileUUID(mf.getUuid());
        theme.setCssFiles(Arrays.asList(new String[]{TEST_CSS_FILE_PATH}));
        theme.setJsFiles(Arrays.asList(new String[]{TEST_JS_FILE_PATH}));
        theme.setPortal(portal);
        theme.setThemeElements(themeElements);
        theme = themeDAO.save(theme);
        
        ZipUtils.decompressToDir(new ZipFile(fileService.getFile(mf, mf.getFilename()).getFile()), 
                                 fileService.getTargetDirectory(theme, Theme.THEME_DIR_RAW, true));
        
        login("root", "password", new String[] { Role.ROOT });
        
        request.setMethod("POST");
        request.setRequestURI("/bdrs/root/theme/editThemeFile.htm");
        request.setParameter("themePk", theme.getId().toString());
        request.setParameter("themeFileName", TEST_CSS_FILE_PATH);
        request.setParameter("themeFileContent", TEST_CSS_MODIFIED_RAW_CONTENT);
        
        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertModelAttributeValue(mv, "portalId", portal.getId());
        assertRedirect(mv, "/bdrs/root/theme/edit.htm");
        
        // Check that the save theme files have been decompressed and processed correctly.
        // Check the raw file directory.
        File rawDir = fileService.getTargetDirectory(theme, Theme.THEME_DIR_RAW, false);
        Assert.assertEquals(TEST_CSS_MODIFIED_RAW_CONTENT, FileUtils.fileRead(new File(rawDir, TEST_CSS_FILE_PATH)));
        Assert.assertEquals(TEST_TEMPLATE_RAW_CONTENT, FileUtils.fileRead(new File(rawDir, TEST_TEMPLATE_FILE_PATH)));
        Assert.assertEquals(TEST_JS_RAW_CONTENT, FileUtils.fileRead(new File(rawDir, TEST_JS_FILE_PATH)));
        BufferedImage rawImg = ImageIO.read(new File(rawDir, TEST_IMAGE_FILE_PATH));
        Assert.assertEquals(IMAGE_WIDTH, rawImg.getWidth());
        Assert.assertEquals(IMAGE_HEIGHT, rawImg.getHeight());
        
        // Check the processed file directory
        File processedDir = fileService.getTargetDirectory(theme, Theme.THEME_DIR_PROCESSED, false);
        
        Assert.assertEquals(String.format(TEST_CSS_MODIFIED_DEFAULT_CONTENT_TMPL, theme.getId()).trim(), FileUtils.fileRead(new File(processedDir, TEST_CSS_FILE_PATH)).trim());
        Assert.assertEquals(String.format(TEST_TEMPLATE_DEFAULT_CONTENT, theme.getId()).trim(), FileUtils.fileRead(new File(processedDir, TEST_TEMPLATE_FILE_PATH)).trim());
        Assert.assertEquals(TEST_JS_DEFAULT_CONTENT, FileUtils.fileRead(new File(processedDir, TEST_JS_FILE_PATH)).trim());
        BufferedImage processedImg = ImageIO.read(new File(processedDir, TEST_IMAGE_FILE_PATH));
        Assert.assertEquals(IMAGE_WIDTH, processedImg.getWidth());
        Assert.assertEquals(IMAGE_HEIGHT, processedImg.getHeight());
    }
    
    @Test
    public void testEditThemeFileRevertSubmit() throws Exception {
        ManagedFile mf = new ManagedFile();
        mf.setContentType("application/zip");
        mf.setFilename("testTheme.zip");
        mf.setCredit("");
        mf.setLicense("");
        mf.setDescription("");
        managedFileDAO.save(mf);
        fileService.createFile(mf.getClass(), mf.getId(), mf.getFilename(), createTestTheme(DEFAULT_CONFIG_VALUES));
        
        Portal portal = getRequestContext().getPortal();
        
        Map<String, ThemeElement> themeElementMap = new HashMap<String, ThemeElement>();
        List<ThemeElement> themeElements = new ArrayList<ThemeElement>(DEFAULT_CONFIG_VALUES.size());
        for(Map.Entry<String, String> entry : DEFAULT_CONFIG_VALUES.entrySet()) {
            ThemeElement te = new ThemeElement();
            te.setKey(entry.getKey());
            te.setDefaultValue(entry.getValue());
            te.setCustomValue(entry.getValue());
            te.setType(ThemeElementType.TEXT);
            te = themeDAO.save(te);
            themeElements.add(te);
            themeElementMap.put(te.getKey(), te);
        }
        
        Theme theme = new Theme();
        theme.setActive(true);
        theme.setName("Test Theme");
        theme.setThemeFileUUID(mf.getUuid());
        theme.setCssFiles(Arrays.asList(new String[]{TEST_CSS_FILE_PATH}));
        theme.setJsFiles(Arrays.asList(new String[]{TEST_JS_FILE_PATH}));
        theme.setPortal(portal);
        theme.setThemeElements(themeElements);
        theme = themeDAO.save(theme);
        
        ZipUtils.decompressToDir(new ZipFile(fileService.getFile(mf, mf.getFilename()).getFile()), 
                                 fileService.getTargetDirectory(theme, Theme.THEME_DIR_RAW, true));
        
        File rawDir = fileService.getTargetDirectory(theme, Theme.THEME_DIR_RAW, false);
        File cssFile = new File(rawDir, TEST_CSS_FILE_PATH);
        FileUtils.fileDelete(cssFile.getAbsolutePath());
        FileUtils.fileWrite(cssFile.getAbsolutePath(), "Mary had a little lamb");
        
        login("root", "password", new String[] { Role.ROOT });
        
        request.setMethod("POST");
        request.setRequestURI("/bdrs/root/theme/editThemeFile.htm");
        request.setParameter("themePk", theme.getId().toString());
        request.setParameter("themeFileName", TEST_CSS_FILE_PATH);
        request.setParameter("themeFileContent", TEST_CSS_MODIFIED_RAW_CONTENT);
        request.setParameter("revert", "Revert File");
        
        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertModelAttributeValue(mv, "portalId", portal.getId());
        assertRedirect(mv, "/bdrs/root/theme/edit.htm");
        
        // Check that the save theme files have been decompressed and processed correctly.
        // Check the raw file directory.
        Assert.assertEquals(TEST_CSS_RAW_CONTENT, FileUtils.fileRead(new File(rawDir, TEST_CSS_FILE_PATH)));
        Assert.assertEquals(TEST_TEMPLATE_RAW_CONTENT, FileUtils.fileRead(new File(rawDir, TEST_TEMPLATE_FILE_PATH)));
        Assert.assertEquals(TEST_JS_RAW_CONTENT, FileUtils.fileRead(new File(rawDir, TEST_JS_FILE_PATH)));
        BufferedImage rawImg = ImageIO.read(new File(rawDir, TEST_IMAGE_FILE_PATH));
        Assert.assertEquals(IMAGE_WIDTH, rawImg.getWidth());
        Assert.assertEquals(IMAGE_HEIGHT, rawImg.getHeight());
        
        // Check the processed file directory
        File processedDir = fileService.getTargetDirectory(theme, Theme.THEME_DIR_PROCESSED, false);
        
        Assert.assertEquals(String.format(TEST_CSS_DEFAULT_CONTENT_TMPL, theme.getId()).trim(), FileUtils.fileRead(new File(processedDir, TEST_CSS_FILE_PATH)).trim());
        Assert.assertEquals(String.format(TEST_TEMPLATE_DEFAULT_CONTENT, theme.getId()).trim(), FileUtils.fileRead(new File(processedDir, TEST_TEMPLATE_FILE_PATH)).trim());
        Assert.assertEquals(TEST_JS_DEFAULT_CONTENT, FileUtils.fileRead(new File(processedDir, TEST_JS_FILE_PATH)).trim());
        BufferedImage processedImg = ImageIO.read(new File(processedDir, TEST_IMAGE_FILE_PATH));
        Assert.assertEquals(IMAGE_WIDTH, processedImg.getWidth());
        Assert.assertEquals(IMAGE_HEIGHT, processedImg.getHeight());
    }
    
    @Test
    public void testEditRevertSubmit() throws Exception {
        ManagedFile mf = new ManagedFile();
        mf.setContentType("application/zip");
        mf.setFilename("testTheme.zip");
        mf.setCredit("");
        mf.setLicense("");
        mf.setDescription("");
        managedFileDAO.save(mf);
        fileService.createFile(mf.getClass(), mf.getId(), mf.getFilename(), createTestTheme(DEFAULT_CONFIG_VALUES));
        
        Portal portal = getRequestContext().getPortal();
        
        Map<String, ThemeElement> themeElementMap = new HashMap<String, ThemeElement>();
        List<ThemeElement> themeElements = new ArrayList<ThemeElement>(DEFAULT_CONFIG_VALUES.size());
        for(Map.Entry<String, String> entry : DEFAULT_CONFIG_VALUES.entrySet()) {
            ThemeElement te = new ThemeElement();
            te.setKey(entry.getKey());
            te.setDefaultValue(entry.getValue());
            te.setCustomValue(CUSTOM_CONFIG_VALUES.get(entry.getKey()));
            te.setType(ThemeElementType.TEXT);
            te = themeDAO.save(te);
            themeElements.add(te);
            themeElementMap.put(te.getKey(), te);
        }
        
        Theme theme = new Theme();
        theme.setActive(true);
        theme.setName("Test Theme");
        theme.setThemeFileUUID(mf.getUuid());
        theme.setCssFiles(Arrays.asList(new String[]{TEST_CSS_FILE_PATH}));
        theme.setJsFiles(Arrays.asList(new String[]{TEST_JS_FILE_PATH}));
        theme.setPortal(portal);
        theme.setThemeElements(themeElements);
        theme = themeDAO.save(theme);
        
        ZipUtils.decompressToDir(new ZipFile(fileService.getFile(mf, mf.getFilename()).getFile()), 
                                 fileService.getTargetDirectory(theme, Theme.THEME_DIR_RAW, true));
        
        login("root", "password", new String[] { Role.ROOT });
                
        request.setMethod("POST");
        request.setRequestURI("/bdrs/root/theme/edit.htm");
        request.setParameter("portalPk", portal.getId().toString());
        request.setParameter("themePk", theme.getId().toString());
        request.setParameter("name", theme.getName());
        request.setParameter("themeFileUUID", theme.getThemeFileUUID());
        request.setParameter("active", String.valueOf(theme.isActive()));
        request.setParameter("revert", "Revert Theme");
        for(Map.Entry<String, String> customEntry : CUSTOM_CONFIG_VALUES.entrySet()) {
            ThemeElement te = themeElementMap.get(customEntry.getKey());
            request.setParameter(String.format(ThemeController.THEME_ELEMENT_CUSTOM_VALUE_TEMPLATE, te.getId()),
                                 customEntry.getValue());
        }
        
        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertModelAttributeValue(mv, "portalId", portal.getId());
        assertRedirect(mv, "/bdrs/root/theme/edit.htm");
        
        Theme actualTheme = themeDAO.getTheme((Integer)mv.getModel().get("themeId"));
        Assert.assertEquals(theme.getName(), actualTheme.getName());
        Assert.assertEquals(theme.getThemeFileUUID(), actualTheme.getThemeFileUUID());
        Assert.assertEquals(theme.isActive(), actualTheme.isActive());
        
        Assert.assertEquals(DEFAULT_CONFIG_VALUES.size(), actualTheme.getThemeElements().size());
        for(ThemeElement elem : actualTheme.getThemeElements()) {
            String expectedDefaultValue = DEFAULT_CONFIG_VALUES.get(elem.getKey());
            Assert.assertEquals(expectedDefaultValue, elem.getDefaultValue());
            Assert.assertEquals(expectedDefaultValue, elem.getCustomValue());
        }
        
        // Check that the save theme files have been decompressed and processed correctly.
        // Check the raw file directory.
        File rawDir = fileService.getTargetDirectory(actualTheme, Theme.THEME_DIR_RAW, false);
        Assert.assertEquals(TEST_CSS_RAW_CONTENT, FileUtils.fileRead(new File(rawDir, TEST_CSS_FILE_PATH)));
        Assert.assertEquals(TEST_TEMPLATE_RAW_CONTENT, FileUtils.fileRead(new File(rawDir, TEST_TEMPLATE_FILE_PATH)));
        Assert.assertEquals(TEST_JS_RAW_CONTENT, FileUtils.fileRead(new File(rawDir, TEST_JS_FILE_PATH)));
        BufferedImage rawImg = ImageIO.read(new File(rawDir, TEST_IMAGE_FILE_PATH));
        Assert.assertEquals(IMAGE_WIDTH, rawImg.getWidth());
        Assert.assertEquals(IMAGE_HEIGHT, rawImg.getHeight());
        
        // Check the processed file directory
        File processedDir = fileService.getTargetDirectory(actualTheme, Theme.THEME_DIR_PROCESSED, false);
        Assert.assertEquals(String.format(TEST_CSS_DEFAULT_CONTENT_TMPL, theme.getId()).trim(), FileUtils.fileRead(new File(processedDir, TEST_CSS_FILE_PATH)).trim());
        Assert.assertEquals(String.format(TEST_TEMPLATE_DEFAULT_CONTENT, theme.getId()).trim(), FileUtils.fileRead(new File(processedDir, TEST_TEMPLATE_FILE_PATH)).trim());
        Assert.assertEquals(TEST_JS_DEFAULT_CONTENT, FileUtils.fileRead(new File(processedDir, TEST_JS_FILE_PATH)).trim());
        BufferedImage processedImg = ImageIO.read(new File(processedDir, TEST_IMAGE_FILE_PATH));
        Assert.assertEquals(IMAGE_WIDTH, processedImg.getWidth());
        Assert.assertEquals(IMAGE_HEIGHT, processedImg.getHeight());
        
        assertThemePage(theme.getId().intValue());
    }
    
    /**
     * This only gets used in tests where we expect the theme to be processed from the
     * zipped theme file
     * @param themeId
     */
    private void assertThemePage(int themeId) {
        List<ThemePage> pageList = themeDAO.getThemePages(themeId);
        Assert.assertEquals(2, pageList.size());
        
        ThemePage p1 = getByKey(pageList, TEST_THEME_PAGE_1.get("key"));
        Assert.assertNotNull("expect test page 1", p1);
        Assert.assertEquals("check titles", TEST_THEME_PAGE_1.get("title"), p1.getTitle());
        Assert.assertEquals("check descriptions", TEST_THEME_PAGE_1.get("description"), p1.getDescription());
        
        ThemePage p2 = getByKey(pageList, TEST_THEME_PAGE_2.get("key"));
        Assert.assertNotNull("expect teset page 2", p2);
        Assert.assertEquals("check titles", TEST_THEME_PAGE_2.get("title"), p2.getTitle());
        Assert.assertEquals("check descriptions", TEST_THEME_PAGE_2.get("description"), p2.getDescription());
    }
    
    private ThemePage getByKey(List<ThemePage> list, String key) {
        for (ThemePage p : list) {
            if (key.equals(p.getKey())) {
                return p;
            }
        }
        return null;
    }
    
    private byte[] createTestTheme(Map<String, String> configValues) throws Exception {
        Map<String, byte[]> files = new HashMap<String, byte[]>();
        files.put(TEST_CSS_FILE_PATH, createMemoryFile(TEST_CSS_RAW_CONTENT));
        files.put(TEST_TEMPLATE_FILE_PATH, createMemoryFile(TEST_TEMPLATE_RAW_CONTENT));
        files.put(TEST_JS_FILE_PATH, createMemoryFile(TEST_JS_RAW_CONTENT));
        files.put(TEST_IMAGE_FILE_PATH, createTestImage());
        
        List<Map<String,String>> themePageList = new ArrayList<Map<String,String>>();
        themePageList.add(TEST_THEME_PAGE_1);
        themePageList.add(TEST_THEME_PAGE_2);
        
        files.put(ThemeService.THEME_CONFIG_FILENAME, createConfigFile(
                                                                          new String[]{TEST_CSS_FILE_PATH}, 
                                                                          new String[]{TEST_JS_FILE_PATH}, 
                                                                          configValues,
                                                                          themePageList));
        return createZip(files);
    }
    
    private byte[] createConfigFile(String[] cssFilePathArray, String[] jsFilePathArray, Map<String,String> configValues,
            List<Map<String,String>> themePageList) throws IOException {
        
        JSONArray cssFiles = new JSONArray();
        for(String css : cssFilePathArray) {
            cssFiles.add(css);
        }
        
        JSONArray jsFiles = new JSONArray();
        for(String js : jsFilePathArray) {
            jsFiles.add(js);
        }
        
        JSONArray themeElements = new JSONArray();
        JSONObject elem;
        for(Map.Entry<String, String> entry : configValues.entrySet()) {
            elem = new JSONObject();
            elem.accumulate("type", ThemeElementType.TEXT);
            elem.accumulate("key", entry.getKey());
            elem.accumulate("value", entry.getValue());
            
            themeElements.add(elem);
        }
        
        JSONArray themePages = new JSONArray();
        for (Map<String, String> page : themePageList) {
            JSONObject pageElem = new JSONObject();
            for(Map.Entry<String, String> entry : page.entrySet()) {
                pageElem.accumulate(entry.getKey(), entry.getValue());
            }
            themePages.add(pageElem);
        }

        JSONObject config = new JSONObject();
        config.accumulate("css_files", cssFiles);
        config.accumulate("js_files", jsFiles);
        config.accumulate("theme_elements", themeElements);
        config.accumulate("theme_pages", themePages);
        
        return createMemoryFile(config.toString());
    }
    
    private byte[] createTestImage() throws IOException {
        BufferedImage img = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D g2 = (Graphics2D)img.getGraphics();
        Random rand = new Random();
        g2.setColor(new Color(rand.nextInt(255),rand.nextInt(255),rand.nextInt(255)));
        g2.fillRect(0,0,img.getWidth(),img.getHeight());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream(img.getWidth() * img.getHeight());
        ImageIO.write(img, "png", baos);
        baos.flush();
        byte[] rawBytes = baos.toByteArray();
        baos.close();
        
        return rawBytes;
    }
    
    private byte[] createMemoryFile(String content) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(bos);
        writer.write(content);
        writer.flush();
        writer.close();
        
        byte[] data = bos.toByteArray();
        bos.close();
        return data;
    }
    
    private byte[] createZip(Map<String, byte[]> files) throws IOException {  
        ByteArrayOutputStream bos = new ByteArrayOutputStream();  
        ZipOutputStream zipfile = new ZipOutputStream(bos);
        
        ZipEntry zipentry;
        for(Map.Entry<String, byte[]> entry : files.entrySet()) {
            zipentry = new ZipEntry(entry.getKey());  
            zipfile.putNextEntry(zipentry);  
            zipfile.write(entry.getValue());
        }

        zipfile.flush();
        zipfile.close();
        
        byte[] data = bos.toByteArray();
        bos.close();
        return data;
    }
    
    
    @Test
    public void testDownloadZip() throws Exception {
        ManagedFile mf = new ManagedFile();
        mf.setContentType("application/zip");
        mf.setFilename("testTheme.zip");
        mf.setCredit("");
        mf.setLicense("");
        mf.setDescription("");
        mf = managedFileDAO.save(mf);
        byte[] testTheme = createTestTheme(DEFAULT_CONFIG_VALUES);
        fileService.createFile(mf.getClass(), mf.getId(), mf.getFilename(), testTheme);
        
        login("root", "password", new String[] { Role.ROOT,Role.ADMIN });
        Portal portal = getRequestContext().getPortal();
        
        Map<String, ThemeElement> themeElementMap = new HashMap<String, ThemeElement>();
        List<ThemeElement> themeElements = new ArrayList<ThemeElement>(DEFAULT_CONFIG_VALUES.size());
        for(Map.Entry<String, String> entry : DEFAULT_CONFIG_VALUES.entrySet()) {
            ThemeElement te = new ThemeElement();
            te.setKey(entry.getKey());
            te.setDefaultValue(entry.getValue());
            te.setCustomValue(CUSTOM_CONFIG_VALUES.get(entry.getKey()));
            te.setType(ThemeElementType.TEXT);
            te = themeDAO.save(te);
            themeElements.add(te);
            themeElementMap.put(te.getKey(), te);
        }
        
        Theme expectedTheme = new Theme();
        expectedTheme.setActive(true);
        expectedTheme.setName("Test Theme");
        expectedTheme.setThemeFileUUID(mf.getUuid());
        expectedTheme.setCssFiles(Arrays.asList(new String[]{TEST_CSS_FILE_PATH}));
        expectedTheme.setJsFiles(Arrays.asList(new String[]{TEST_JS_FILE_PATH}));
        expectedTheme.setPortal(portal);
        expectedTheme.setThemeElements(themeElements);
        expectedTheme = themeDAO.save(expectedTheme);
        
        ZipUtils.decompressToDir(new ZipFile(fileService.getFile(mf, mf.getFilename()).getFile()), 
                                 fileService.getTargetDirectory(expectedTheme, Theme.THEME_DIR_RAW, true));
        
        request.setMethod("GET");
        request.setRequestURI("/bdrs/admin/theme/downloadTheme.htm");
        request.setParameter("themeId", expectedTheme.getId().toString());
        
        ModelAndView mv = handle(request, response);
        Assert.assertNull(mv);
        Assert.assertEquals(testTheme.length, response.getContentLength());
        byte[] responseBytes = response.getContentAsByteArray();
        Assert.assertEquals(testTheme.length, responseBytes.length);
        /*for (int i = 0; i < testTheme.length && i < responseBytes.length; i++) {
            Assert.assertEquals("byte " + i + " of files is different", testTheme[i], responseBytes[i]);
        }*/
    }
    
    @Test
    public void testRefreshTheme() throws Exception {
        login("root", "password", new String[] { Role.ROOT });
        Portal portal = getRequestContext().getPortal();
        Theme activeTheme = getRequestContext().getTheme();

        // get a theme besides the active one
        List<Theme> themes = themeDAO.getThemes(portal);
        if (activeTheme == null) {
            activeTheme = themes.get(0);
        } else {
            for (Theme theme : themes) {
                if (theme.equals(activeTheme)) {
                    activeTheme = theme;
                }
            }
        }
        request.setMethod("GET");
        request.setRequestURI("/bdrs/admin/theme/refreshTheme.htm");
        request.setParameter("portalId", portal.getId().toString());
        request.setParameter("themeId", activeTheme.getId().toString());

        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "themeListing");
        ModelAndViewAssert.assertModelAttributeValue(mv, "portalId", portal.getId());
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "themeList");
        
        // assert that the active theme and the selected one are the same
        Assert.assertEquals(activeTheme, themeDAO.getActiveTheme(portal));
    }
}
