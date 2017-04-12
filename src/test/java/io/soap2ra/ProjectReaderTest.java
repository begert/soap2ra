package io.soap2ra;

import com.smartbear.soapui.generatedschema.Project;
import org.junit.Test;

import static org.junit.Assert.*;

public class ProjectReaderTest {

    @Test
    public void shouldReadSoapUIProjectFileWithoutError() {
        ProjectReader reader = new ProjectReader();
        Project project = reader.read(getClass().getResource("/soapui-project.xml").getFile());

        assertNotNull(project);
    }

    @Test
    public void shouldContainOneTestSuite() {
        ProjectReader reader = new ProjectReader();
        Project project = reader.read(getClass().getResource("/soapui-project.xml").getFile());

        assertEquals(1, project.getTestSuite().size());
        assertEquals("Suite1", project.getTestSuite().get(0).getName());
    }
}