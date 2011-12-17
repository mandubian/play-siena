package functional;

import org.junit.Test;
import play.mvc.Http;
import play.test.FunctionalTest;

import java.util.HashMap;
import java.util.Map;

/**
 * User: Wursteisen David
 * Date: 17/12/11
 * Time: 17:25
 */
public class BindingTest extends FunctionalTest {
    
    @Test
    public void testBindingObjectNotIncludedIntoTheRequest() {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("fakeParameters", "fakeValue");
        Http.Response response = POST("/Binding/bindMe", parameters);
        assertIsOk(response);
    }
}
