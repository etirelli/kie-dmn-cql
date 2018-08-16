package org.kie.dmn.cql;

import org.junit.Assert;
import org.junit.Test;
import org.kie.dmn.model.v1_1.Definitions;

import java.io.IOException;
import java.io.InputStream;

public class CQLtoDMNTest {

    @Test
    public void testChlamydia() throws IOException {
        InputStream is = CQLtoDMNTest.class.getResourceAsStream("/ChlamydiaScreening_CQM.cql");
        Assert.assertNotNull("NULL resource", is );

        Definitions def = CQLtoDMNTranslator.translate(is);

        System.out.println("namespace: " + def.getNamespace());
        System.out.println("name: " + def.getName());
        System.out.println("id: " + def.getId());
    }

}
