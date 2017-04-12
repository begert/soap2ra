package io.soap2ra;

import com.smartbear.soapui.generatedschema.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InterfaceSimplifier {

    private final List<Interface> interfaces;
    private final Map<String, RestResource> paths;

    public InterfaceSimplifier(List<Interface> interfaces) {
        this.interfaces = interfaces;
        interfaces.forEach(i -> System.out.println(((RestService) i).getBasePath()));

        RestService service = (RestService) interfaces.get(0);
        paths = buildPathMap(service.getResource());
    }

    public String httpMethodFor(String path, String methodName) {
        RestResource res = getResourceForPath(path);
        for (RestMethod restMethod : res.getMethod()) {
            if (restMethod.getName().equals(methodName)) {
                return restMethod.getMethod().toUpperCase();
            }
        }
        return "???";
    }

    public Map<String, String> collectParamsFor(String path, String methodName, String style) {
        RestResource res = getResourceForPath(path);

        List<RestParameter> collected = res.getParameters().getParameter();
        String strippedPath = path.substring(0, path.lastIndexOf("/"));
        RestResource parent = getResourceForPath(strippedPath);
        while (parent != null) {
            collected.addAll(parent.getParameters().getParameter());
            strippedPath = strippedPath.substring(0, strippedPath.lastIndexOf("/"));
            parent = getResourceForPath(strippedPath);
        }

        Map<String, String> map = new HashMap<>();
        for (RestMethod restMethod : res.getMethod()) {
            if (restMethod.getName().equals(methodName)) {
                collected.addAll(restMethod.getParameters().getParameter());
                for (RestParameter parameter : collected) {
                    if (parameter != null && parameter.getStyle().equals(style)) {
                        map.put(parameter.getName(), parameter.getValue());
                    }
                }
            }
        }
        return map;
    }

    private RestResource getResourceForPath(String path) {
        return paths.get(path.replace("//", "/"));
    }

    private Map<String, RestResource> buildPathMap(List<RestResource> resources) {
        String path = "";
        Map<String, RestResource> map = new HashMap<>();
        buildPathR(path, resources, map);
        return map;
    }

    private void buildPathR(String path, List<RestResource> resources, Map<String, RestResource> map) {
        for (RestResource resource : resources) {
            String newPath = path + "/" + resource.getPath();
            newPath = newPath.replace("//", "/");
            map.put(newPath, resource);
            if (!resource.getResource().isEmpty()) {
                buildPathR(newPath, resource.getResource(), map);
            }
        }
    }

    public String firstEndpoint() {
        return interfaces.get(0).getEndpoints().getEndpoint().get(0);
    }
}
