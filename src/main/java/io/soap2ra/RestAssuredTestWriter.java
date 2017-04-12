package io.soap2ra;

import com.smartbear.soapui.generatedschema.*;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;

public class RestAssuredTestWriter {

    private final InterfaceSimplifier interfaces;
    private final String baseClassName;
    private final List<Property> properties;

    public RestAssuredTestWriter(List<Interface> interfaces, PropertiesType properties) {
        this.interfaces = new InterfaceSimplifier(interfaces);
        this.properties = properties.getProperty();
        this.baseClassName = "AbstractRestTest";
    }


    public void baseClassFile(String pkg, String path) throws Exception {
        String code = baseClass(pkg);
        writeToFile(path, pkg, baseClassName + ".java", code);
    }

    public String baseClass(String pkg) {
        CodeBuilder code = new CodeBuilder();
        code
                .ln("package %s;", pkg)
                .ln()
                .ln("import org.junit.BeforeClass;")
                .ln("import io.restassured.RestAssured;")
                .ln()
                .ln("import java.util.HashMap;")
                .ln("import java.util.Map;")
                .ln()
                .ln("public abstract class %s {", baseClassName)
                .indent()
                .ln()
                .ln("private static Map<String, String> properties = new HashMap<>();")
                .ln()
                .ln("@BeforeClass")
                .ln("public static void setUp() {")
                .indent()
                .ln("RestAssured.baseURI = \"%s\";", interfaces.firstEndpoint());

        for (Property property : properties) {
            code.ln("properties.put(\"%s\", \"%s\");", property.getName(), property.getValue());
        }

        code
                .unindent()
                .ln("}")
                .ln()
                .ln("public static String getProperty(String key) {")
                .indent().ln("return properties.get(key);").unindent()
                .ln("}")
                .unindent()
                .ln("}");
        return code.toString();
    }

    public void suiteToClassFile(TestSuite suite, String pkg, String path) throws Exception {
        String code = suiteToClass(suite, pkg);
        writeToFile(path, pkg, CodeBuilder.classNameFrom(suite.getName()) + ".java", code);
    }

    public String suiteToClass(TestSuite suite, String pkg) {
        CodeBuilder code = new CodeBuilder();
        code
                .ln("package %s;", pkg)
                .ln()
                .ln("import org.junit.Test;")
                .ln("import io.restassured.RestAssured;")
                .ln()
                .ln("import static io.restassured.RestAssured.given;")
                .ln("import static org.hamcrest.Matchers.*;")
                .ln()
                .ln("/**")
                .ln(" * %s", suite.getDescription())
                .ln(" */")
                .ln("public class %s extends %s {", CodeBuilder.classNameFrom(suite.getName()), baseClassName)
                .indent();

        for (TestCase testCase : suite.getTestCase()) {
            code
                    .ln()
                    .ln("@Test")
                    .ln("public void %s() {", CodeBuilder.methodNameFrom(testCase.getName()))
                    .indent();

            for (TestStep step : testCase.getTestStep()) {
                code.ln("//step %s", step.getName());
                writeStep(code, step);
                code.ln();
            }

            code.unindent().ln("}");
        }

        code.unindent().ln("}").ln();

        return code.toString();
    }

    private void writeStep(CodeBuilder code, TestStep step) {
        if (!"restrequest".equals(step.getType())) {
            code.ln("//TODO %s ignored, type %s not implemented", step.getName(), step.getType());
        } else {
            RestRequestStep config = (RestRequestStep) step.getConfig();

            code.ln("given()").indent();
            writeRequestBody(code, config);
            writeRequestParameters(code, config);

            code.unindent().ln(".when()").indent();
            writeRequest(code, config);

            code.unindent().ln(".then()").indent();
            writeAssertions(code, config);

            code.ln(";").unindent(); //TODO ; on own line?
        }
    }

    private void writeAssertions(CodeBuilder code, RestRequestStep config) {
        code.ln(".log().all()");

        for (TestAssertion assertion : config.getRestRequest().getAssertion()) {

            code.ln("// %s", assertion.getName());
            new AssertionBuilder().build(code, assertion);
        }
    }

    private void writeRequest(CodeBuilder code, RestRequestStep config) {
        String method = interfaces.httpMethodFor(config.getResourcePath(), config.getMethodName());
        switch (method) {
            case "GET":
                code.ln(".get(\"%s\")", config.getResourcePath());
                break;
            case "POST":
                code.ln(".post(\"%s\")", config.getResourcePath());
                break;
            case "PUT":
                code.ln(".put(\"%s\")", config.getResourcePath());
                break;
            case "DELETE":
                code.ln(".delete(\"%s\")", config.getResourcePath());
                break;
            default:
                code.ln("//TODO method %s not implemented", method);
        }
    }

    private void writeRequestParameters(CodeBuilder code, RestRequestStep config) {
        Map<String, String> headerParams = interfaces.collectParamsFor(config.getResourcePath(), config.getMethodName(), "HEADER");
        Map<String, String> queryParams = interfaces.collectParamsFor(config.getResourcePath(), config.getMethodName(), "QUERY");
        Map<String, String> pathParams = interfaces.collectParamsFor(config.getResourcePath(), config.getMethodName(), "TEMPLATE");

        config.getRestRequest().getParameters().getEntry().forEach(e -> {
            if (headerParams.containsKey(e.getKey())) {
                headerParams.put(e.getKey(), e.getValue());
            } else if (queryParams.containsKey(e.getKey())) {
                queryParams.put(e.getKey(), e.getValue());
            } else if (pathParams.containsKey(e.getKey())) {
                pathParams.put(e.getKey(), e.getValue());
            }
        });

        for (Map.Entry<String, String> header : headerParams.entrySet()) {
            code.ln(".header(\"%s\", %s)", header.getKey(), injectVariables(header.getValue()));
        }
        for (Map.Entry<String, String> param : queryParams.entrySet()) {
            code.ln(".queryParam(\"%s\", %s)", param.getKey(), injectVariables(param.getValue()));
        }
        for (Map.Entry<String, String> param : pathParams.entrySet()) {
            code.ln(".pathParam(\"%s\", %s)", param.getKey(), injectVariables(param.getValue()));
        }
    }

    private String injectVariables(String value) {
        String ret = "";
        if(value != null) {
            ret = value.trim().replace("\"", "\\\"");
            for (Property property : properties) {
                String pp = "${#Project#" + property.getName() + "}";
                String rp = "getProperty(\"" + property.getName() + "\")";
                if (ret.equals(pp)) {
                    return rp;
                } else {
                    ret = ret.replace(pp,
                            "\" + " + rp + " + \"");
                }
            }
        }
        return "\"" + ret + "\"";
    }

    private void writeRequestBody(CodeBuilder code, RestRequestStep config) {
        if (!config.getRestRequest().getRequest().getValue().trim().isEmpty()) {
            code.ln(".contentType(\"%s\")", config.getRestRequest().getMediaType());
            String body = config.getRestRequest().getRequest().getValue();
            String[] lines = body.split("\n");
            code.ln(".body(").indent().indent();
            for (int i = 0; i < lines.length; i++) {
                code.ln("%s%s", injectVariables(lines[i]), i < lines.length - 1 ? " +" : "");
            }
            code.unindent().unindent().ln(")");
        }
    }

    private void writeToFile(String path, String pkg, String fileName, String content) throws Exception{
        String dir = path + "/" + pkg.replace(".", "/") + "/";
        File d = new File(dir);
        d.mkdirs();
        FileWriter file = new FileWriter(dir + fileName);
        file.write(content);
        file.close();
    }
}
