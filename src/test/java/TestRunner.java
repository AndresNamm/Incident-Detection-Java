/**
 * Created by UDU on 7/21/2017.
 */

import bgt.RouteSegmentation.UtilityfunctionsTest;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class TestRunner {
    public static void main(String[] args) {
        Result result = JUnitCore.runClasses(UtilityfunctionsTest.class);

        for (Failure failure : result.getFailures()) {
            System.out.println(failure.toString());
        }

        System.out.println(result.wasSuccessful());
        System.out.println("Ran this many tests: "+ result.getRunCount());
    }

}
