package org.kie.dmn.cql;

import org.junit.Assert;
import org.junit.Test;
import org.kie.dmn.api.marshalling.v1_1.DMNMarshaller;
import org.kie.dmn.backend.marshalling.v1_1.DMNMarshallerFactory;
import org.kie.dmn.model.v1_1.Definitions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

public class CQLtoDMNTest {

    @Test
    public void testChlamydia() throws IOException {
        InputStream is = CQLtoDMNTest.class.getResourceAsStream("/ChlamydiaScreening_CQM.cql");
        Assert.assertNotNull("NULL resource", is );

        Definitions def = CQLtoDMNTranslator.translate(is);

        System.out.println("namespace: " + def.getNamespace());
        System.out.println("name: " + def.getName());
        System.out.println("id: " + def.getId());

        DMNMarshaller marshaller = DMNMarshallerFactory.newDefaultMarshaller();
        String dmn = marshaller.marshal(def);
        System.out.println(dmn);

        File out = new File("ChlamydiaScreening_CQM.dmn" );
        PrintWriter writer = new PrintWriter( out );
        writer.write( dmn );
        writer.close();
    }

}
