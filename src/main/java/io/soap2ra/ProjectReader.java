package io.soap2ra;

import com.smartbear.soapui.generatedschema.Project;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.File;

public class ProjectReader {

    public Project read(String filename) {

        try {
            File file = new File(filename);
            JAXBContext context = JAXBContext.newInstance(Project.class.getPackage().getName());

            JAXBElement<Project> project = (JAXBElement<Project>) context.createUnmarshaller().unmarshal(file);

            return project.getValue();

        } catch (JAXBException e) {
            e.printStackTrace();
            return null;
        }
    }
}
