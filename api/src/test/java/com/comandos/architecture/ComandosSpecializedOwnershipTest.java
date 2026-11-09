package com.comandos.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * COMANDOS owns specialized domain behavior and consumes
 * generic FARIA MIGUEL capabilities.
 */
class ComandosSpecializedOwnershipTest {

    private static final Path JAVA =
        Path.of("src", "main", "java", "com", "comandos");

    @Test
    void specializedInventoryRulesMustRemainInComandos() throws IOException {
        Path rules = JAVA.resolve(
            Path.of("inventory", "service", "ComandosInventoryRules.java")
        );

        assertTrue(Files.isRegularFile(rules));

        String source = Files.readString(rules, StandardCharsets.UTF_8);

        assertTrue(
            source.contains("ArmamentParameter"),
            "COMANDOS must retain armament-specific validation"
        );

        assertTrue(
            source.contains("FirearmSpecification"),
            "COMANDOS must retain firearm-specific validation"
        );

        assertTrue(
            source.contains("AmmunitionSpecification"),
            "COMANDOS must retain ammunition-specific validation"
        );
    }

    @Test
    void specializedInventoryCatalogMustRemainInComandos() throws IOException {
        Path catalog = JAVA.resolve(
            Path.of("inventory", "service", "ComandosInventoryCatalogContributor.java")
        );

        assertTrue(Files.isRegularFile(catalog));

        String source = Files.readString(catalog, StandardCharsets.UTF_8);

        assertTrue(source.contains("armament-types"));
        assertTrue(source.contains("firearm-specifications"));
        assertTrue(source.contains("ammunition-specifications"));
    }
    @Test
    void comandosMustDependOnFariaMiguelArtifacts() throws Exception {
        var dependencies = directDependencies();

        assertTrue(
            dependencies.contains("com.fariamiguel:faria-miguel-shared-business-services"),
            "COMANDOS must depend on FARIA MIGUEL Core"
        );

        assertTrue(
            dependencies.contains("com.fariamiguel:faria-miguel-platform"),
            "COMANDOS must consume the FARIA MIGUEL Platform Core"
        );

        assertTrue(
            dependencies.contains("com.fariamiguel:faria-miguel-commerce"),
            "COMANDOS must depend on FARIA MIGUEL Commerce"
        );
    }

    private static java.util.List<String> directDependencies()
            throws Exception {
        var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(
            "http://apache.org/xml/features/disallow-doctype-decl", true
        );
        factory.setFeature(
            "http://xml.org/sax/features/external-general-entities", false
        );
        factory.setFeature(
            "http://xml.org/sax/features/external-parameter-entities", false
        );
        factory.setFeature(
            javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true
        );

        var document = factory.newDocumentBuilder()
            .parse(Path.of("pom.xml").toFile());

        var xpath = javax.xml.xpath.XPathFactory.newInstance().newXPath();
        var expression =
            "/*[local-name()='project']" +
            "/*[local-name()='dependencies']" +
            "/*[local-name()='dependency']";

        var nodes = (org.w3c.dom.NodeList) xpath.evaluate(
            expression,
            document,
            javax.xml.xpath.XPathConstants.NODESET
        );

        var dependencies = new java.util.ArrayList<String>();

        for (int i = 0; i < nodes.getLength(); i++) {
            var node = nodes.item(i);
            var children = node.getChildNodes();
            String group = "";
            String artifact = "";

            for (int j = 0; j < children.getLength(); j++) {
                var child = children.item(j);

                if ("groupId".equals(child.getLocalName())) {
                    group = child.getTextContent().trim();
                }

                if ("artifactId".equals(child.getLocalName())) {
                    artifact = child.getTextContent().trim();
                }
            }

            dependencies.add(group + ":" + artifact);
        }

        return dependencies;
    }
}
