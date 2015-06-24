package org.ruleEditor.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.ruleEditor.ontology.OntologyProperty;
import org.ruleEditor.ontology.OntologyProperty.ObjectProperty;
import org.ruleEditor.ontology.PointElement;
import org.ruleEditor.ontology.OntologyProperty.DataProperty;
import org.ruleEditor.ontology.PointElement.Type;

public class RuleCreationUtilities {

	public static String prefix_c4a = "@prefix c4a: <http://rbmm.org/schemas/cloud4all/0.1/>.";
	public static String prefix_rdfs = "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.";

	public static void saveRule(String ruleName, String fileName,
			ArrayList<PointElement> conditions,
			ArrayList<PointElement> conclusions, boolean flag,
			boolean createNewFile, String feedbackClass, String feedbackScope,
			String feedbackId, ArrayList<Rule> existingRules,
			InputStream fileStream) throws IOException {

		// create the rule string
		String rule = "";
		if (feedbackClass.isEmpty())
			rule = createRule(conditions, conclusions, ruleName);
		else
			rule = createFeedBackRule(feedbackClass, feedbackScope, feedbackId,
					conditions, conclusions, ruleName);

		// export the rule
		if (!rule.isEmpty()) {
			if (!createNewFile) {
				existingRules = Utils.getRulesFromFile(fileStream);
				String allRuleString = Utils.prefix_c4a + "\n"
						+ Utils.prefix_rdfs + "\n";
				for (Rule temp : existingRules) {
					allRuleString = allRuleString.concat(temp.getBody()) + "\n";
				}
				allRuleString = allRuleString.concat(rule
						.replace(Utils.prefix_c4a, "")
						.replace(Utils.prefix_rdfs, "").trim());
				rule = allRuleString;
			}

			FileDownloadController.writeGsonAndExportFile(fileName, rule);
		}

	}

	public static String createRule(ArrayList<PointElement> conditions,
			ArrayList<PointElement> conclusions, String ruleName) {

		String rule = prefix_c4a + "\n" + prefix_rdfs + "\n\n" + "[" + ruleName
				+ "\n";

		rule = rule + convertListToRule(conditions);

		rule = rule + "->\n";

		rule = rule + convertListToRule(conclusions);

		rule = rule + "]";

		return rule;
	}

	public static String createFeedBackRule(String className, String scope,
			String id, ArrayList<PointElement> conditions, ArrayList<PointElement> conclusions,String ruleName) {

		String rule = prefix_c4a + "\n" + prefix_rdfs + "\n\n" + "[" + ruleName
				+ "\n";

		rule = rule + convertListToRule(conditions);

		rule = rule + "makeSkolem(?newMetaData, " + className + ", " + scope
				+ ")\n";

		rule = rule + "->\n";

		if (!conclusions.isEmpty())
			rule = rule + convertListToRule(conclusions);

		rule = rule + "(?newMetaData rdf:type c4a:Metadata)\n"
				+ "(?newMetaData c4a:scope " + scope + ")\n"
				+ "(?newMetaData c4a:messageType \"helpMessage\")\n"
				+ "(?newMetaData c4a:refersTo c4a:" + id + ")\n" + "("
				+ className + " c4a:hasMetadata ?newMetaData)\n]";

		return rule;
	}

	public static String convertListToRule(ArrayList<PointElement> list) {
		String rule = "";

		// work on conditions
		for (PointElement el : list) {
			if (el.getType() == PointElement.Type.CLASS) {

				rule = rule + "(" + el.getVarName() + " rdf:type " + "c4a:"
						+ el.getElementName() + ")\n";

			} else if (el.getType() == PointElement.Type.DATA_PROPERTY) {

				OntologyProperty property = (DataProperty) el.getProperty();
				String value = ((DataProperty) property).getValue();
				if(!value.contains("?"))
					value = "\""+value+"\"";
				
				if (el.getConnections().size() > 0)
					rule = rule + "(" + el.getConnections().get(0).getVarName()
							+ " c4a:" + el.getElementName() + " "
							+ value + ")\n";

			} else if (el.getType() == PointElement.Type.OBJECT_PROPERTY) {

				// TODO: check that there are two connections
				// and define the correct order
				// in case that something is missing, throw away
				// the property node
				OntologyProperty property = (ObjectProperty) el.getProperty();
				String className = property.getClassName();
				String classRange = ((ObjectProperty) property)
						.getRangeOfClasses().get(0);
				String source = "";
				String target = "";
				if (el.getConnections().size() > 1) {
					for (PointElement temp : el.getConnections()) {
						if (temp.getType() == Type.CLASS
								&& temp.getElementName().equalsIgnoreCase(
										className))
							source = temp.getVarName();
						else if (temp.getType() == Type.CLASS
								&& temp.getElementName().equalsIgnoreCase(
										classRange))
							target = temp.getVarName();
						else if (temp.getType() == Type.INSTANCE)
							target = temp.getInstance().getId();
					}

					rule = rule + "(" + source + " c4a:" + el.getElementName()
							+ " " + target + ")\n";

				}

			} else if (el.getType() == PointElement.Type.BUILTIN_METHOD) {

				rule = rule + el.getMethod().getOriginalName() + "(";
				for (PointElement temp : el.getConnections()) {
					DataProperty property = (DataProperty) temp.getProperty();
					rule = rule + property.getValue() + ",";
				}

				rule = rule.substring(0, rule.length() - 1);
				rule = rule + ")\n";
			}

		}

		return rule;
	}

}
