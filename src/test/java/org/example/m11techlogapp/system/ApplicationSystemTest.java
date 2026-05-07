package org.example.m11techlogapp.system;

import javafx.fxml.FXML;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationSystemTest {

    private static final String FXML_RESOURCE_DIR = "/org/example/m11techlogapp/";
    private static final String FX_NAMESPACE = "http://javafx.com/fxml/1";

    @Test
    void allApplicationViewsHaveResolvableControllersInjectedFieldsHandlersAndStylesheets() throws Exception {
        List<String> fxmlFiles = List.of(
                "mainPage.fxml",
                "generateLogs.fxml",
                "outputLogs.fxml",
                "search.fxml",
                "updateRecords.fxml");

        List<String> failures = new ArrayList<>();
        for (String fxmlFile : fxmlFiles) {
            validateFxmlContract(fxmlFile, failures);
        }

        assertTrue(failures.isEmpty(), () -> String.join(System.lineSeparator(), failures));
    }

    private static void validateFxmlContract(String fxmlFile, List<String> failures) throws Exception {
        Document document = parseFxml(fxmlFile);
        Element root = document.getDocumentElement();
        String controllerClassName = root.getAttributeNS(FX_NAMESPACE, "controller");

        if (controllerClassName == null || controllerClassName.isBlank()) {
            failures.add(fxmlFile + ": missing fx:controller");
            return;
        }

        Class<?> controllerClass = loadControllerClass(fxmlFile, controllerClassName, failures);
        if (controllerClass == null) {
            return;
        }

        validateStylesheet(fxmlFile, root, failures);
        Set<String> fxIds = new HashSet<>();
        validateElement(root, controllerClass, fxmlFile, failures, fxIds);
        validateControllerFields(fxmlFile, controllerClass, fxIds, failures);
    }

    private static Document parseFxml(String fxmlFile) throws Exception {
        URL resource = ApplicationSystemTest.class.getResource(FXML_RESOURCE_DIR + fxmlFile);
        assertNotNull(resource, () -> fxmlFile + " resource is missing");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

        try (InputStream inputStream = resource.openStream()) {
            return factory.newDocumentBuilder().parse(inputStream);
        }
    }

    private static Class<?> loadControllerClass(String fxmlFile, String controllerClassName, List<String> failures) {
        try {
            return Class.forName(controllerClassName);
        } catch (ClassNotFoundException e) {
            failures.add(fxmlFile + ": controller class does not exist: " + controllerClassName);
            return null;
        }
    }

    private static void validateStylesheet(String fxmlFile, Element root, List<String> failures) {
        String stylesheets = root.getAttribute("stylesheets");
        if (stylesheets == null || stylesheets.isBlank()) {
            return;
        }

        for (String stylesheet : stylesheets.split(",")) {
            String resourceName = stylesheet.trim();
            if (resourceName.startsWith("@")) {
                resourceName = resourceName.substring(1);
            }

            URL stylesheetResource = ApplicationSystemTest.class.getResource(FXML_RESOURCE_DIR + resourceName);
            if (stylesheetResource == null) {
                failures.add(fxmlFile + ": stylesheet resource does not exist: " + stylesheet);
            }
        }
    }

    private static void validateElement(
            Element element,
            Class<?> controllerClass,
            String fxmlFile,
            List<String> failures,
            Set<String> fxIds) {

        collectFxId(element, fxIds);
        validateEventHandlers(element, controllerClass, fxmlFile, failures);

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element childElement) {
                validateElement(childElement, controllerClass, fxmlFile, failures, fxIds);
            }
        }
    }

    private static void collectFxId(Element element, Set<String> fxIds) {
        String fxId = element.getAttributeNS(FX_NAMESPACE, "id");
        if (fxId != null && !fxId.isBlank()) {
            fxIds.add(fxId);
        }
    }

    private static void validateControllerFields(
            String fxmlFile,
            Class<?> controllerClass,
            Set<String> fxIds,
            List<String> failures) {

        for (Field field : fxmlFields(controllerClass)) {
            if (!fxIds.contains(field.getName())) {
                failures.add(fxmlFile + ": @FXML field '" + field.getName() + "' has no matching fx:id");
            }
        }
    }

    private static void validateEventHandlers(Element element, Class<?> controllerClass, String fxmlFile, List<String> failures) {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            String value = attribute.getNodeValue();
            if (attribute.getNodeName().startsWith("on") && value != null && value.startsWith("#")) {
                String methodName = value.substring(1);
                if (!hasEventHandlerMethod(controllerClass, methodName)) {
                    failures.add(fxmlFile + ": event handler '" + value + "' has no matching method on " + controllerClass.getSimpleName());
                }
            }
        }
    }

    private static List<Field> fxmlFields(Class<?> controllerClass) {
        List<Field> fields = new ArrayList<>();
        Class<?> currentClass = controllerClass;
        while (currentClass != null) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(FXML.class)) {
                    fields.add(field);
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        return fields;
    }

    private static boolean hasEventHandlerMethod(Class<?> controllerClass, String methodName) {
        Class<?> currentClass = controllerClass;
        while (currentClass != null) {
            for (Method method : currentClass.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() <= 1) {
                    return true;
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        return false;
    }
}
