bdrs.menu = {};

bdrs.menu.initHover = function() {
    sfHover = function() {
         var nav = document.getElementById("nav");
         if (nav) {
             var sfEls = nav.getElementsByTagName("LI");
             for (var i=0; i<sfEls.length; i++) {
                sfEls[i].onmouseover=function() {
                    this.className+=" sfhover";
                };
                sfEls[i].onmouseout=function() {
                    this.className=this.className.replace(new RegExp(" sfhover\\b"), "");
                };
             }
        }
    };
    if (window.attachEvent) { 
        window.attachEvent("onload", sfHover);
    }
};

bdrs.menu.populateMapMenu = function(mapMenuItemSelector) {
    // populate map menu...
    var popMapMenuParams = {};
    jQuery.getJSON(bdrs.portalContextPath + "/bdrs/public/webservice/getAvailableMaps.htm", popMapMenuParams, function(data) {
        var menu = jQuery(mapMenuItemSelector);
        // Append items since we want to keep the 'my sightings' map at the top
        for(var i=0; i<data.length; ++i) {
            var geoMap = data[i];
            var menuitem = jQuery("<li></li>").attr({"title":geoMap.description});
            var link =  jQuery("<a></a>").attr({
                'href': bdrs.portalContextPath + "/bdrs/map/view.htm?geoMapId="+geoMap.id
            }).text(geoMap.name);
            menuitem.append(link);
            menu.append(menuitem);
        }
    });
};

bdrs.menu.populateSurveyItems = function(registrationKey, contribMenuSelector, parentLink) {
    var params = {};
    params.ident = registrationKey;
    // javascript doing some weird closure stuff
    var getMenuRenderFcn = function(surveyMenuItem, surveyId, parentLink) {
        return function(data) {
            var censusMethodList = jQuery("<ul></ul>");
			
			// set the link on the parent item to the default census method...
			if (data && data.length > 0) {
				// the first item is the default census method.
				var defaultCensusMethod = data[0];
				parentLink.attr("href", bdrs.portalContextPath + "/bdrs/user/surveyRenderRedirect.htm?surveyId="+surveyId+"&censusMethodId="+defaultCensusMethod.id);
			}
			
            for(var j=0; j<data.length; ++j) {
                var censusMethod = data[j];
                var cmMenuItem = jQuery("<li "+(j === data.length-1 ? "class=\"menutop\"" : "")+"></li>");
                var cmElemId = "method_"+censusMethod.id;
                cmMenuItem.attr({id:cmElemId});
                var link =  jQuery("<a></a>").attr({
                    'href': bdrs.portalContextPath + "/bdrs/user/surveyRenderRedirect.htm?surveyId="+surveyId+"&censusMethodId="+censusMethod.id
                }).text(censusMethod.name);
                cmMenuItem.append(link);
                censusMethodList.append(cmMenuItem);
            }
            surveyMenuItem.append(censusMethodList);
        };
    };
    jQuery.getJSON(bdrs.portalContextPath + "/webservice/survey/surveysForUser.htm", params, function(data) {
        var menu = jQuery(contribMenuSelector);

        var survey;
        // bulk data item at the top. Other surveys appear underneth
        // in alphabetical order.
        for(var i=0; i<data.length; ++i) {
            survey = data[i];
            var menuitem = jQuery("<li></li>").attr({"title":survey.description});
            var link =  jQuery("<a></a>").attr({
                'href': bdrs.portalContextPath + "/bdrs/user/surveyRenderRedirect.htm?surveyId="+survey.id
            }).text(survey.name);
            menuitem.append(link);
            menu.append(menuitem);
            
            // populate census method items...
            jQuery.getJSON(bdrs.portalContextPath + "/bdrs/user/censusMethod/getSurveyCensusMethods.htm", {surveyId:survey.id}, getMenuRenderFcn(menuitem, survey.id, link));
        }
    });
};