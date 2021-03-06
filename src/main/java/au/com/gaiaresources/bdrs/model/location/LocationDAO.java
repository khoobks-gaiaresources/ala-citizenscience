package au.com.gaiaresources.bdrs.model.location;

import java.util.Collection;
import java.util.List;

import org.hibernate.Session;

import au.com.gaiaresources.bdrs.db.impl.PagedQueryResult;
import au.com.gaiaresources.bdrs.db.impl.PaginationFilter;
import au.com.gaiaresources.bdrs.model.facet.FacetDAO;
import au.com.gaiaresources.bdrs.model.region.Region;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.user.User;

import com.vividsolutions.jts.geom.Point;

public interface LocationDAO extends FacetDAO {
    /**
     * Get all of the <code>Location</code>s defined for a <code>User</code>.
     * @param user <code>User</code>.
     * @return <code>List</code> of <code>Location</code>s.
     */
    List<Location> getUserLocations(User user);

    /**
     * Get all of the <code>Location</code>s defined for a <code>User</code>
     * within a specific <code>Region</code>.
     * @param user <code>User</code> that owns the <code>Location</code>.
     * @param region The <code>Region</code>s that this location is part of.
     * @return <code>List</code> of <code>Location</code>s.
     */
    List<Location> getUserLocations(User user, Region region);

    /**
     * Count the number of locations that a user has defined.
     * @param user {@link User} the user to count for.
     * @return {@link Integer}
     */
    Integer countUserLocations(User user);

    /**
     * Get a <code>Location</code> by it's owning <code>User</code> and name.
     * @param user <code>User</code>.
     * @param locationName <code>String</code>.
     * @return <code>Location</code> or null if not found.
     */
    Location getUserLocation(User user, String locationName);

    /**
     * Get a location by id.
     * @param user <code>User</code>.
     * @param locationID <code>Integer</code>.
     * @return <code>Location</code>.
     */
    Location getUserLocation(User user, Integer locationID);

    /**
     * Create a new <code>Location</code> for a <code>User</code>. The <code>Point</code> is
     * expected to be in WGS84, i.e. regular latitude and longitude.
     * @param user <code>User</code> that owns the <code>Location</code>.
     * @param locationName <code>String</code> the name of the <code>Location</code>.
     * @param location JTS <code>Point</code> the coordinate of the <code>Location</code>.
     * @param regions The <code>Region</code>s that this location is part of.
     * @return <code>Location</code>.
     */
    Location createUserLocation(User user, String locationName, Point location, Collection<? extends Region> regions);

    /**
     * Update a pre-defined <code>Location</code>.
     * @param locationID The <code>Integer</code> id of the <code>Location</code>.
     * @param newLocationName <code>String</code> the new name of the <code>Location</code>.
     * @param newLocation JTS <code>Point</code> the new coordinate of the <code>Location</code>.
     * @return <code>Location</code>.
     */
    Location updateUserLocation(Integer locationID, String newLocationName, Point newLocation);

    /**
     * Update a pre-defined <code>Location</code>.
     * @param loc The <code>Location</code> to be updated.
     * @return <code>Location</code>.
     */
    Location updateLocation(Location loc);

    /**
     * Create a new location in the database.
     * @param loc the <code>Location</code> to be persisted.
     * @return <code>Location</code>.
     */
    Location createLocation(Location loc);

    /**
     * The save function differs from the normal create/update function by
     * performing a create or an update depending if the location contains
     * an id. If there is no id, a create is performed, otherwise an update
     * is performed.
     * @param loc the <code>Location</code> to be persisted.
     * @return <code>Location</code>.
     */
    Location save(Location loc);

    /**
     * Deletes the specified Location from the database.
     * @param delLoc the <code>Location</code> to be deleted.
     */
    void delete(Location delLoc);

    /**
     * @see #getLocationByName(Session, String, String)
     */
    Location getLocationByName(String surveyName, String locationName);

    /**
     * Retrieves a location with the specified parent survey and location name.
     * @param sesh the session to be used to retrieve the location or null if
     * the current session should be used.
     * @param surveyName the name of the parent survey.
     * @param locationName the name of the location.
     * @return a location with the specified location name in a survey with the
     * specified name.
     */
    Location getLocationByName(Session sesh, String surveyName,
            String locationName);

    /**
     * Retrieves a location with the specified primary key
     * @param pk primary key of the location
     * @return the location associated with the specified primary key.
     */
    Location getLocation(int pk);
    /**
    * Retrieves a location with the specified primary key using the given session
    * @param pk primary key of the location
    * @param sesh the session to use to retrieve the location.
    * @return the location associated with the specified primary key.
    */
    Location getLocation(Session sesh, int pk);

    /**
     * Retrieves a list of locations with the specified primary keys
     * @param pk primary key of the location
     * @return the location associated with the specified primary key.
     */
    public List<Location> getLocations(List<Integer> ids);

    /**
     * Retrieves the location associated with the specified user and survey.
     * @param survey the survey containing the user and location.
     * @param user the user associated with the location.
     * @return the locations associated with the specified user and survey.
     */
    List<Location> getLocation(Survey survey, User user);


    /**
     * Returns survey locations (user locations are not included), from all
     * the surveys that 'user' has access to.
     * 
     * @param filter contains the pagination details
     * @param user who is requesting the locations
     * @param surveyIdToExclude the id of the current survey we are attempting
     * to assign locations to. since we don't want to double assign, ignore this survey id
     * @return
     */
    PagedQueryResult<Location> getSurveylocations(PaginationFilter filter, User user, int surveyIdToExclude);

    /**
     * Retrieves the {@link Location} that was synchronized with the specified
     * client identifier.
     * 
     * @param clientID the identifier used by the client that originally 
     * synchronized this location. 
     * @return the {@link Location} that was synchronized with the specified
     * client identifier.
     */
    Location getLocationByClientID(String clientID);
    
    /**
     * Retrieves the {@link Location} that was synchronized with the specified
     * client identifier.
     * 
     * @param sesh the session to use when retrieving the {@link Location}
     * @param clientID the identifier used by the client that originally 
     * synchronized this location. 
     * @return the {@link Location} that was synchronized with the specified
     * client identifier.
     */
    Location getLocationByClientID(Session sesh, String clientID);

    /**
     * Gets all of the locations.
     * @return
     */
    public List<Location> getLocations();
}
