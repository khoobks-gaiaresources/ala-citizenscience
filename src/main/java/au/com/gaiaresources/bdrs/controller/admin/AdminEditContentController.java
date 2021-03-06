package au.com.gaiaresources.bdrs.controller.admin;

import java.util.List;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import edu.emory.mathcs.backport.java.util.TreeSet;

import au.com.gaiaresources.bdrs.controller.AbstractController;
import au.com.gaiaresources.bdrs.model.content.ContentDAO;
import au.com.gaiaresources.bdrs.model.portal.Portal;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.content.ContentService;

@RolesAllowed({Role.ADMIN})
@Controller
public class AdminEditContentController extends AbstractController {
    Logger log = Logger.getLogger(AdminEditContentController.class);
    @Autowired
    private ContentService contentService;
    @Autowired
    private ContentDAO contentDAO;
    
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value = "/admin/editContent.htm", method = RequestMethod.GET)
    public ModelAndView renderPage(HttpServletRequest request,
            HttpServletResponse response) {
        ModelAndView mav = new ModelAndView("adminEditContent");
        List<String> keys = contentDAO.getAllKeys();
        // add the default portal initializer keys as well if not present
        Set<String> uniqueKeys = new TreeSet(keys);
        uniqueKeys.addAll(ContentService.CONTENT.keySet());
        mav.addObject("keys", uniqueKeys);
        return mav;
    }
    
    // There is no protection when using this URL directly. You will reset
    // all of your content!
    @RolesAllowed({Role.ADMIN, Role.ROOT, Role.POWERUSER, Role.SUPERVISOR})
    @RequestMapping(value="/admin/resetContentToDefault.htm", method = RequestMethod.GET)
    public String reset(HttpServletRequest request, HttpServletResponse response, 
            @RequestParam(value = "key", required = false) String key) throws Exception {   
        Portal currentPortal = getRequestContext().getPortal();
        if (currentPortal == null) {
            // something has gone seriously wrong for this to happen...
            throw new Exception("The portal cannot be null");
        }
        Session sesh = getRequestContext().getHibernate();
        if (key == null) {
            contentService.initContent(sesh, currentPortal);
        } else {
            contentService.initContent(sesh, currentPortal, key, null);
        }
        return "redirect:/admin/editContent.htm";
    }
}
