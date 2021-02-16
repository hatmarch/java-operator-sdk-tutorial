package org.mhildenb.operatortutorial.demooperator;

import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class JsonTests {
    
    private AppOpsSpec myAppOps;

    @BeforeEach
    public void loadAppOpsSpec()
    {
        myAppOps = null;

        InputStream inJson = AppOpsSpec.class.getResourceAsStream("/testAppOps.json");
        try{
            myAppOps = new ObjectMapper().readValue(inJson, AppOpsSpec.class);
        }
        catch (Exception e)
        {
            assertTrue(false, String.format("Error loading test file: %s", e.getMessage()));
        }
    }
    @Test
    public void shouldFindBarPodLogSpec()
    {
        assertTrue( myAppOps != null, "No AppOpsSpec resource");
        
        Optional<PodLogSpec> spec = myAppOps.getPodLogSpec("bar");
        assertTrue( spec.isPresent() );
        assertTrue(spec.get().name.equals("bar"));
        assertTrue(!spec.get().elevatedLogging);
    }

    @Test
    public void shouldFindBarInAppOps()
    {
        assertTrue( myAppOps != null, "No AppOpsSpec resource");
        
        var appOps = new AppOps();
        appOps.setSpec(myAppOps);
        assertTrue( appOps.isInPodSpec("bar"), "Could not find bar");
    }

    @Test
    public void shouldNotFindBarNoneInPodLogSpec()
    {
        assertTrue( myAppOps != null, "No AppOpsSpec resource");
        
        Optional<PodLogSpec> spec = myAppOps.getPodLogSpec("barNone");
        assertTrue( !spec.isPresent() );
    }

    @Test
    public void shouldRemoveBarFromPodLogSpec()
    {
        assertTrue( myAppOps != null, "No AppOpsSpec resource");
        
        Optional<PodLogSpec> spec = myAppOps.removePodLogSpec("bar");
        assertTrue( spec.isPresent() );

        assertTrue( myAppOps.getPodLogSpecs().size() == 2, "There are not two elements in the list" );
        int count = 0;
        String[] names= {"foo", "baz"};
        for( PodLogSpec s : myAppOps.getPodLogSpecs() )
        {
            assertTrue(s.name.equals(names[count]), "Order or resulting list appears wrong"); 
            count++; 
        }
    }

    @Test
    public void shouldNotRemoveBarNoneFromPodLogSpec()
    {
        assertTrue( myAppOps != null, "No AppOpsSpec resource");
        
        Optional<PodLogSpec> spec = myAppOps.removePodLogSpec("barnone");
        assertTrue( !spec.isPresent() );

        assertTrue( myAppOps.getPodLogSpecs().size() == 3, "There are missing elements in the list" );
        int count = 0;
        String[] names= {"foo", "bar", "baz"};
        for( PodLogSpec s : myAppOps.getPodLogSpecs() )
        {
            assertTrue(s.name.equals(names[count]), "Order or resulting list appears wrong"); 
            count++; 
        }
    }
}
