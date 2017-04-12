package io.soap2ra;

import com.smartbear.soapui.generatedschema.TestAssertion;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class AssertionBuilder {

    private final Map<String, BiConsumer<CodeBuilder, Element>> mappings = new HashMap<>();

    public AssertionBuilder() {
        mappings.put("Valid HTTP Status Codes", this::validHTTPStatusCode);
        mappings.put("Invalid HTTP Status Codes", this::invalidHTTPStatusCode);
        mappings.put("Simple Contains", this::simpleContains);
        mappings.put("Response SLA Assertion", this::responseSLA);
        mappings.put("JsonPath Match", this::jsonPathMatch);
        mappings.put("JsonPath Existence Match", this::jsonPathExistenceMatch);
        mappings.put("JsonPath Count", this::jsonPathCount);
    }

    public void build(CodeBuilder code, TestAssertion assertion) {
        if(mappings.containsKey(assertion.getType())) {
            Element element = (Element) assertion.getConfiguration();
            mappings.get(assertion.getType()).accept(code, element);
        } else {
            System.out.println(assertion.getType());
            code.ln("//TODO assertion %s of type %s ignored, not implemented", assertion.getName(), assertion.getType());
        }
    }

    private void simpleContains(CodeBuilder code, Element element) {
        String token = element.getFirstChild().getTextContent();
        code.ln(".body(contains(\"%s\"))", token);
    }

    private void responseSLA(CodeBuilder code, Element element) {
        String sla = element.getFirstChild().getTextContent();
        code.ln(".time(lessThan(%sL))", sla);
    }

    private void validHTTPStatusCode(CodeBuilder code, Element element) {
        String codes = element.getFirstChild().getTextContent().trim();
        if(codes.contains(",")) {
            code.ln(".statusCode(isOneOf(%s))", codes);
        } else {
            code.ln(".statusCode(%s)", codes);
        }
    }

    private void invalidHTTPStatusCode(CodeBuilder code, Element element) {
        String codes = element.getFirstChild().getTextContent().trim();
        code.ln(".statusCode(not(isOneOf(%s)))", codes);
    }

    private void jsonPathCount(CodeBuilder code, Element element) {
        String path = getJsonPath(element);
        String content = getJsonPathContent(element);
        code.ln(".body(\"%s\", hasSize(%s))", path, content);
    }

    private void jsonPathExistenceMatch(CodeBuilder code, Element element) {
        String path = getJsonPath(element);
        String content = getJsonPathContent(element);
        if("true".equalsIgnoreCase(content)) {
            code.ln(".body(\"%s\", notNullValue())", path);
        } else {
            code.ln(".body(\"%s\", nullValue())", path);
        }
    }

    private void jsonPathMatch(CodeBuilder code, Element element) {
        String path = getJsonPath(element);
        String content = getJsonPathContent(element);
        code.ln(".body(\"%s\", equalTo(\"%s\"))", path, content.replace("\"", "\\\"").replace("\n", "\\n"));
    }

    private String getJsonPathContent(Element element) {
        try {
            return element.getElementsByTagName("content").item(0).getTextContent();
        } catch (Exception e) {
            return "INVALID JSONPATH CONTENT";
        }
    }

    private String getJsonPath(Element element) {
        try {
            return convertJsonPath(element.getElementsByTagName("path").item(0).getTextContent()
                    .trim().replace("\n", ""));
        } catch (Exception e) {
            return "INVALID JSONPATH";
        }
    }

    private String convertJsonPath(String input) {
        //remove all double " "
        String ret = input;
        while(ret.contains("  ")) {
            ret = ret.replace("  ", " ");
        }

        //remove $
        if(ret.startsWith("$.")) {
            ret = ret.substring(2, ret.length());
        }
        return ret;
    }
}
