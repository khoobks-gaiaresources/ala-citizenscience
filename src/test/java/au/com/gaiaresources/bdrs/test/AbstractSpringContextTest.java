package au.com.gaiaresources.bdrs.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import au.com.gaiaresources.bdrs.email.EmailService;
import au.com.gaiaresources.bdrs.email.impl.MockEmailService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "file:src/main/webapp/WEB-INF/climatewatch.xml",
        "file:src/main/webapp/WEB-INF/climatewatch-hibernate.xml",
        "file:src/main/webapp/WEB-INF/climatewatch-hibernate-datasource-test.xml",
        "file:src/main/webapp/WEB-INF/climatewatch-security.xml",
        "file:src/main/webapp/WEB-INF/climatewatch-daos.xml",
        "file:src/main/webapp/WEB-INF/climatewatch-email-test.xml",
        "file:src/main/webapp/WEB-INF/climatewatch-servlet.xml",
        "file:src/main/webapp/WEB-INF/climatewatch-profileConfig-test.xml"})
@Transactional
public abstract class AbstractSpringContextTest extends
        AbstractTransactionalJUnit4SpringContextTests {
    
    Logger log = Logger.getLogger(getClass());
    
    @Autowired
    private EmailService emailService;
    
    @Before
    public void abstractSpringContextTestSetup() {
        // as the EmailService bean is a singleton we need to clear our 
        // mock implementation - see climatewatch-email-test.xml - before
        // each test
        MockEmailService mockEmailService = (MockEmailService)emailService;
        mockEmailService.clearEmails();
    }
    
    @After
    public final void cleanupStreams() {
        for (InputStream in : inStream) {
            try {
                in.close();
            } catch (IOException e) {   
            }
        }
        for (OutputStream out : outStream) {
            try {
                out.close();
            } catch (IOException e) {   
            }
        }
        // clear the streams
        inStream = new ArrayList<InputStream>();
        outStream = new ArrayList<OutputStream>();
    }
    
    private List<InputStream> inStream = new ArrayList<InputStream>();
    private List<OutputStream> outStream = new ArrayList<OutputStream>();
    
    protected final void registerStream(InputStream stream) {
        inStream.add(stream);
    }
    
    protected final void registerStream(OutputStream stream) {
        outStream.add(stream);
    }
}
