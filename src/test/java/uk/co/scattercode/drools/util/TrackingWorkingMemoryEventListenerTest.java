package uk.co.scattercode.drools.util;

import static org.drools.builder.ResourceType.DRL;
import static org.junit.Assert.*;

import java.util.Map;

import org.drools.KnowledgeBase;
import org.drools.builder.ResourceType;
import org.drools.event.rule.ObjectInsertedEvent;
import org.drools.runtime.KnowledgeRuntime;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.co.scattercode.drools.util.ResourcePathType.CLASSPATH;

import uk.co.scattercode.drools.util.TrackingWorkingMemoryEventListener;
import uk.co.scattercode.drools.util.testfacts.Product;
import uk.co.scattercode.drools.util.testfacts.Customer;


/**
 * A very simple test to confirm that the
 * {@link TrackingWorkingMemoryEventListener} is responding to insertions and
 * retractions.
 * 
 * @author Stephen Masters
 */
public class TrackingWorkingMemoryEventListenerTest extends AbstractStatefulSessionRulesTest {

    private static Logger log = LoggerFactory.getLogger(TrackingWorkingMemoryEventListenerTest.class);

    @Override
    protected DroolsResource[] getResources() {
        return new DroolsResource[] { 
        		new DroolsResource("util/TrackingWorkingMemoryEventListenerTest.drl", 
                        ResourcePathType.CLASSPATH, 
                        ResourceType.DRL)
        };
    }
    
    @Mock private ObjectInsertedEvent objectInsertedEvent;
    @Mock private KnowledgeRuntime knowledgeRuntime;
    
    @Before
    public void setUp() {
        initMocks(this);

        when(this.objectInsertedEvent.getKnowledgeRuntime()).thenReturn(this.knowledgeRuntime);
        when(this.objectInsertedEvent.getObject()).thenReturn(new String("Mock object."));
    }
    
    @Test
    public void shouldTrackEvents() {
        TrackingWorkingMemoryEventListener listener = new TrackingWorkingMemoryEventListener();

        int insertionCountBeforeInsertion = listener.getInsertions().size();
        int retractionCountBeforeInsertion = listener.getRetractions().size();
        int updateCountBeforeInsertion = listener.getUpdates().size();

        listener.objectInserted(objectInsertedEvent);

        int insertionCountAfterInsertion = listener.getInsertions().size();
        int retractionCountAfterInsertion = listener.getRetractions().size();
        int updateCountAfterInsertion = listener.getUpdates().size();

        assertEquals(insertionCountBeforeInsertion + 1, insertionCountAfterInsertion);
        assertEquals(retractionCountBeforeInsertion, retractionCountAfterInsertion);
        assertEquals(updateCountBeforeInsertion, updateCountAfterInsertion);
    }
    
    @Test
    public void shouldTrackFilteredUpdates() {
    
        TrackingAgendaEventListener agendaListener = new TrackingAgendaEventListener();
        TrackingWorkingMemoryEventListener workingMemoryListener = new TrackingWorkingMemoryEventListener();
        
        knowledgeEnvironment.knowledgeSession.addEventListener(agendaListener);
        knowledgeEnvironment.knowledgeSession.addEventListener(workingMemoryListener);
        
        FactHandle productHandle = knowledgeEnvironment.knowledgeSession.insert(new Product("Book", 20));
        FactHandle customerHandle = knowledgeEnvironment.knowledgeSession.insert(new Customer("Jimbo"));

        TrackingWorkingMemoryEventListener productListener = new TrackingWorkingMemoryEventListener(productHandle);
        knowledgeEnvironment.knowledgeSession.addEventListener(productListener);
        TrackingWorkingMemoryEventListener customerListener = new TrackingWorkingMemoryEventListener(customerHandle);
        knowledgeEnvironment.knowledgeSession.addEventListener(customerListener);

        knowledgeEnvironment.knowledgeSession.fireAllRules();

        assertEquals("There should have been 10 updates, as the count was increments from 20 to 10.", 10, productListener.getUpdates().size());
        assertEquals("There should only be one customer update.", 1, customerListener.getUpdates().size());

        // Print the product updates...
        StringBuilder sb = new StringBuilder("The following changes to product were tracked:\n");
        for (Map<String, Object> objectProperties : productListener.getFactChanges()) {
            for (String k : objectProperties.keySet()) {
                sb.append(k + "=\"" + objectProperties.get(k) + "\" ");
            }
            sb.append("\n");
        }
        log.info(sb.toString());
    }

}
