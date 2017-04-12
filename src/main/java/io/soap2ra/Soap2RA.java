package io.soap2ra;

import com.smartbear.soapui.generatedschema.Project;
import com.smartbear.soapui.generatedschema.TestSuite;

public class Soap2RA {

    public static void main(String... args) throws Exception {

        String input = null; //input is soapUI xml
        String output = null; //output is dir for generated sources
        String pkg = "io.soap2ra";

        if (args.length == 2) {
            input = args[0];
            output = args[1];
        } else if (args.length == 1) {
            input = args[0];
        } else {
            System.out.println("no input given");
            System.exit(1);
        }

        Project project = read(input);
        RestAssuredTestWriter writer = new RestAssuredTestWriter(project.getInterface(), project.getProperties());

        if(output == null) {
            System.out.println(writer.baseClass(pkg));
        } else {
            writer.baseClassFile(pkg, output);
        }

        for (TestSuite testSuite : project.getTestSuite()) {

            if (output == null) {
                System.out.println(writer.suiteToClass(testSuite, pkg));
            } else {
                writer.suiteToClassFile(testSuite, pkg, output);
            }
        }
    }

    private static Project read(String pathToFile) {
        ProjectReader reader = new ProjectReader();
        return reader.read(pathToFile);
    }
}
