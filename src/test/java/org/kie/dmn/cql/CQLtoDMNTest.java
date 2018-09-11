package org.kie.dmn.cql;

import org.junit.Assert;
import org.junit.Test;
import org.kie.dmn.api.core.DMNMessage;
import org.kie.dmn.api.marshalling.v1_1.DMNMarshaller;
import org.kie.dmn.backend.marshalling.v1_1.DMNMarshallerFactory;
import org.kie.dmn.model.v1_1.Definitions;
import org.kie.dmn.validation.DMNValidator;
import org.kie.dmn.validation.DMNValidatorFactory;

import java.io.*;
import java.util.List;

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

        DMNValidator validator = DMNValidatorFactory.newValidator();
        List<DMNMessage> validate = validator.validate(new StringReader(dmn), DMNValidator.Validation.VALIDATE_COMPILATION, DMNValidator.Validation.VALIDATE_MODEL, DMNValidator.Validation.VALIDATE_SCHEMA);

        for( DMNMessage m : validate ) {
            System.out.println( m );
        }

        File out = new File("output/ChlamydiaScreening_CQM.dmn" );
        PrintWriter writer = new PrintWriter( out );
        writer.write( dmn );
        writer.close();
    }

}
