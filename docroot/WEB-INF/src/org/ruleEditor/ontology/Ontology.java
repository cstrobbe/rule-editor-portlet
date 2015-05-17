package org.ruleEditor.ontology;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import org.ruleEditor.ontology.OntologyProperty.DataProperty;
import org.ruleEditor.ontology.OntologyProperty.ObjectProperty;
import org.ruleEditor.utils.Utils;

import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.OntTools;
import com.hp.hpl.jena.ontology.impl.IndividualImpl;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.OWL;

public class Ontology implements Serializable {

	private OntModel ontologyModel;
	String ontologyFileName = "semanticFrameworkOfContentAndSolutions.owl";
	String ontologyPath = "";
	String SOURCE = "http://www.cloud4all.eu/SemanticFrameworkForContentAndSolutions.owl";
	String NS = SOURCE + "#";
	private List<String> classes = Arrays.asList("Solutions",
			"InstalledSolution", "Preference", "Metadata", "Setting",
			"InferredConfiguration", "Configuration", "Conflict",
			"ConflictResolution", "PreferenceSet", "OperatingSystem",
			"MultipleATConflict", "MultipleSolutionConflict", "Environment",
			"Devices", "Platforms");

	// "AssistiveTechnology","AccessibilitySolutions"

	public void loadOntology() {

		FacesContext context = FacesContext.getCurrentInstance();

		String path = ((ServletContext) context.getExternalContext()
				.getContext()).getRealPath(ontologyFileName);
		System.out.println(path);
		try {
			ontologyPath = path;
			ontologyModel = ModelFactory.createOntologyModel();
			InputStream in = FileManager.get().open(ontologyPath);
			if (in == null) {
				throw new IllegalArgumentException("File: " + ontologyPath
						+ " not found");
			}

			ontologyModel.read(in, "");

		} catch (Exception e) {
			System.out.println("myexception");
			e.printStackTrace();
		}

	}

	public List<ArrayList<OntologyClass>> getClassesStructured() {

		List<ArrayList<OntologyClass>> list = new ArrayList<ArrayList<OntologyClass>>();
		ArrayList<OntologyClass> tempList = new ArrayList<OntologyClass>();

		for (String s : classes) {

			OntClass essaClasse = ontologyModel.getOntClass(NS + s);

			String vClasse = essaClasse.getLocalName().toString();
			List<OntologyClass> l = new ArrayList<OntologyClass>();

			// get children classes
			if (essaClasse.hasSubClass()) {
				for (Iterator i = essaClasse.listSubClasses(true); i.hasNext();) {
					OntClass c = (OntClass) i.next();
					l.add(getConceptChildrenStructured(c,vClasse));
				}
			}

			// create a new class
			OntologyClass on = new OntologyClass();
			on.setClassName(vClasse);
			on.setChildren(l);

			// TODO
			fillClassData(on,vClasse);

			tempList.add(on);
			list.add(tempList);
			tempList = new ArrayList<OntologyClass>();
		}

		return list;
	}

	public void fillClassData(OntologyClass myClass,String motherClassName) {

		ArrayList<DataProperty> dataProperties = new ArrayList<DataProperty>();
		ArrayList<ObjectProperty> objectProperties = new ArrayList<ObjectProperty>();
		ArrayList<String> instances = new ArrayList<String>();

		String className = myClass.getClassName();

		List<String> dataPropertiesNames = setDataPropertiesToClass(motherClassName);
		List<String> objectPropertiesNames = setObjectPropertiesToClass(motherClassName);

		OntClass cl = ontologyModel.getOntClass(NS + myClass.getClassName());
		// get instances
		// TODO load instances and their properties
		List<IndividualImpl> myinstances = new ArrayList<IndividualImpl>();
		if (cl != null) {
			Iterator it = cl.listInstances();
			while (it.hasNext()) {
				IndividualImpl in = (IndividualImpl) it.next();
				String categoryName = in.getOntClass().toString().split("#")[1];
				
				//format the name of the instance and add it to the correct
				//class 
				if (categoryName.equalsIgnoreCase(className)) {
					String instanceName = in.getURI().replace(NS, "");
					if (instanceName.contains("_"))
						instanceName = instanceName.split("_")[0];
					instances.add(Utils.splitCamelCase(instanceName));
					myinstances.add(in);
				}
			}
		}

		// prepare properties
		// TODO load properties
		// data properties
		OntologyProperty prop = new OntologyProperty("", "");
		OntologyProperty.DataProperty dataProp = null;
		for (String s : dataPropertiesNames) {
			dataProp = prop.new DataProperty(s, className);
			dataProp.setOntologyURI(NS+className+"_"+s);
			dataProp.setDataRange("string");
			dataProp.setValue("empty");
			dataProperties.add(dataProp);
		}

		// object properties
		OntologyProperty.ObjectProperty objectProp = null;
		for (String s : objectPropertiesNames) {

			String objName = s.split("_")[0];
			String range = s.split("_")[1];
			objectProp = prop.new ObjectProperty(objName, className);
			objectProp.setOntologyURI(NS + className + "_" + objName);
			objectProp.setRangeOfClasses(new ArrayList<String>());
			objectProp.getRangeOfClasses().add(range);
			objectProperties.add(objectProp);
		}
		
		myClass.setDataProperties(dataProperties);
		myClass.setObjectProperties(objectProperties);
		myClass.setInstances(instances);
	}

	public OntologyClass getConceptChildrenStructured(OntClass c,String motherClassName) {
		List<OntologyClass> l = new ArrayList<OntologyClass>();
		if (c.hasSubClass()) {
			for (Iterator i = c.listSubClasses(true); i.hasNext();) {
				OntClass cc = (OntClass) i.next();
				l.add(getConceptChildrenStructured(cc,motherClassName));
			}
		}
		OntologyClass on = new OntologyClass();
		on.setClassName(c.getLocalName());
		on.setChildren(l);
		
		fillClassData(on, motherClassName);

		return on;
	}

	public List<String> setDataPropertiesToClass(String name) {
		List<String> solutions = Arrays.asList("hasSolutionName", "id", "hasSolutionDescription","hasSolutionVersion",
				"hasStartCommand","hasStopCommand","hasCapabilities","hasCapabilitiesTransformations","hasContraints");
		List<String> installedSolution = Arrays.asList("name", "id");
		List<String> preference = Arrays.asList("id", "name", "type", "value");
		List<String> metadata = Arrays.asList("value", "type");
		List<String> setting = Arrays.asList("name", "id", "description",
				"refersTo", "value");
		List<String> inferredConfiguration = Arrays.asList("id", "name");
		List<String> configuration = Arrays.asList("id", "name", "isActive",
				"solutionIsPreferred");
		List<String> conflict = Arrays.asList("id", "name");
		List<String> conflictResolution = Arrays.asList("id", "name");
		List<String> preferenceSet = new ArrayList<String>();

		List<String> operatingSystem = Arrays.asList("name", "version");
		List<String> multipleATConflict = new ArrayList<String>();
		List<String> multipleSolutionConflict = new ArrayList<String>();
		List<String> environment = Arrays.asList("hasEnvironmentName");
		List<String> devices = Arrays.asList("hasDeviceName", "hasDeviceDescription");
		List<String> platforms = Arrays.asList("hasPlatformName", "hasPlatformDescription",
				"hasPlatformVersion", "hasPlatformSubType", "hasPlatfomType");

		if (name.equalsIgnoreCase("Solutions"))
			return solutions;
		else if (name.equalsIgnoreCase("InstalledSolution"))
			return installedSolution;
		else if (name.equalsIgnoreCase("Preference"))
			return preference;
		else if (name.equalsIgnoreCase("Metadata"))
			return metadata;
		else if (name.equalsIgnoreCase("Setting"))
			return setting;
		else if (name.equalsIgnoreCase("InferredConfiguration"))
			return inferredConfiguration;
		else if (name.equalsIgnoreCase("Configuration"))
			return configuration;
		else if (name.equalsIgnoreCase("Conflict") || name.equalsIgnoreCase("ConflictResolution"))
			return conflict;
		else if (name.equalsIgnoreCase("OperatingSystem"))
			return operatingSystem;
		else if (name.equalsIgnoreCase("Devices"))
			return devices;
		else if (name.equalsIgnoreCase("platforms"))
			return platforms;
		else if (name.equalsIgnoreCase("environment"))
			return platforms;
		else
			return new ArrayList<String>();

	}

	public List<String> setObjectPropertiesToClass(String name) {
		List<String> solutions = Arrays.asList("hasSolutionSpecificSettings_Settings",
				"runsOnDevice_Devices", "runsOnPlatform_Platforms");
		List<String> setting = new ArrayList<String>();
		List<String> metadata = Arrays.asList("scope_?");
		List<String> conflict = Arrays.asList("hasResolution_?");
		List<String> preferenceSet = Arrays.asList("hasMetadata_Metadata",
				"hasPreference_Preference");
		List<String> inferredConfiguration = Arrays.asList("hasMetadata_Metadata",
				"hasPreference_Preference", "refersTo_?");
		List<String> devices = Arrays.asList("hasDeviceVendor_Vendor","hasDeviceSpecificSetting_Settings",
				"isSupportingDeviceOf_Solutions");
		List<String> platforms = Arrays.asList("hasPlatformVendor_Vendor","hasPlatformSpecificSetting_Settings",
				"platformSupports_Solutions");
		List<String> installedSolution = Arrays.asList("settings_Settings");

		if (name.equals("Solutions"))
			return solutions;
		else if (name.equals("Setting"))
			return setting;
		else if (name.equals("Conflict"))
			return conflict;
		else if (name.equals("PreferenceSet"))
			return preferenceSet;
		else if (name.equals("InferredConfiguration"))
			return inferredConfiguration;
		else if (name.equals("Devices"))
			return devices;
		else if (name.equals("Platforms"))
			return platforms;
		else if (name.equals("Metadata"))
			return metadata;
		else if (name.equals("InstalledSolution"))
			return installedSolution;
		else
			return new ArrayList<String>();

	}

}
