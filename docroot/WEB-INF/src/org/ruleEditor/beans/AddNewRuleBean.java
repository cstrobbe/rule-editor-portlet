package org.ruleEditor.beans;

import java.awt.MenuItem;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.AjaxBehaviorEvent;

import org.apache.commons.io.FilenameUtils;
import org.primefaces.component.commandbutton.CommandButton;
import org.primefaces.component.contextmenu.ContextMenu;
import org.primefaces.component.panel.Panel;
import org.primefaces.context.RequestContext;
import org.primefaces.event.CloseEvent;
import org.primefaces.event.DragDropEvent;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TreeDragDropEvent;
import org.primefaces.event.diagram.ConnectEvent;
import org.primefaces.event.diagram.ConnectionChangeEvent;
import org.primefaces.event.diagram.DisconnectEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.primefaces.model.diagram.Connection;
import org.primefaces.model.diagram.DefaultDiagramModel;
import org.primefaces.model.diagram.DiagramModel;
import org.primefaces.model.diagram.Element;
import org.primefaces.model.diagram.connector.Connector;
import org.primefaces.model.diagram.connector.StraightConnector;
import org.primefaces.model.diagram.endpoint.DotEndPoint;
import org.primefaces.model.diagram.endpoint.EndPoint;
import org.primefaces.model.diagram.endpoint.EndPointAnchor;
import org.primefaces.model.diagram.endpoint.RectangleEndPoint;
import org.primefaces.model.diagram.overlay.ArrowOverlay;
import org.primefaces.model.mindmap.DefaultMindmapNode;
import org.primefaces.model.mindmap.MindmapNode;
import org.ruleEditor.ontology.BuiltinMethod;
import org.ruleEditor.ontology.Main;
import org.ruleEditor.ontology.Message;
import org.ruleEditor.ontology.Ontology;
import org.ruleEditor.ontology.OntologyClass;
import org.ruleEditor.ontology.OntologyProperty;
import org.ruleEditor.ontology.OntologyProperty.DataProperty;
import org.ruleEditor.ontology.OntologyProperty.ObjectProperty;
import org.ruleEditor.ontology.PointElement;
import org.ruleEditor.ontology.PointElement.Type;
import org.ruleEditor.utils.FileDownloadController;
import org.ruleEditor.utils.FileUploadController;
import org.ruleEditor.utils.Utils;

import sun.rmi.runtime.NewThreadAction;

import com.sun.faces.component.visit.FullVisitContext;

@ManagedBean(name = "addNewRuleBean")
@SessionScoped
public class AddNewRuleBean {

	private Main main;
	private DefaultTreeNode root;
	private TreeNode selectedNode = null;
	private String ruleName = "", newFileName = "", oldFileName = "", feedbackFile="";
	private InputStream fileStream;
	private DefaultDiagramModel conditionsModel;
	private DefaultDiagramModel conclusionsModel;
	private List<DataProperty> datatypes = null;
	private List<ObjectProperty> objects = null;
	private List<String> instances = null;
	private OntologyProperty selectedDataProperty = new OntologyProperty("","");
	private OntologyProperty selectedObjectProperty = new OntologyProperty("","");
	private ArrayList<PointElement> conditions;
	private ArrayList<PointElement> conclusions;
	private int counter;
	private int initialX =3;
	private int initialY = 3;
	private int objectCounter = 0;
	private String nodeForRemove;
	private PointElement clonedTargetElement = null;
	private PointElement targetElement = null;
	private PointElement sourceElement= null;
	private PointElement originalTargetElement = null;
	private PointElement clonedOriginalTargetElement = null;
	private PointElement cloneSelectedNode = null;
	private BuiltinMethod selectedMethod = null;
	private List<Message> messages = null;
	private Message messageForRemove = new Message();
	private boolean flag = false;// false : for simple rule
	                             // true : for feedback rule
	private String jsonString = "";
	private String selectedInstance = "";


	public AddNewRuleBean() {
		super();

		FacesContext context = FacesContext.getCurrentInstance();
		main = (Main) context.getApplication().evaluateExpressionGet(context,
				"#{main}", Main.class);
		createOntologyTree(main.getOntology());

	}

	public void init(boolean tempFlag) {

		flag = tempFlag;
		ruleName = "";
		newFileName = "";
		oldFileName = "";
		feedbackFile="";
		datatypes = new ArrayList<DataProperty>();
		objects = new ArrayList<ObjectProperty>();
		instances = new ArrayList<String>();
		conditions = new ArrayList<PointElement>();
		conclusions = new ArrayList<PointElement>();
		counter = 0;
		initialX =3;
		initialY = 3;
		objectCounter = 0;
		selectedNode = null;
		selectedDataProperty = new OntologyProperty("","");
		selectedObjectProperty = new OntologyProperty("","");
		nodeForRemove = "";
		clonedTargetElement = new PointElement();
		targetElement = new PointElement();
		sourceElement= new PointElement();
		originalTargetElement = new PointElement();
		clonedOriginalTargetElement = new PointElement();
		selectedInstance = "";
		
		Message emptyMessage = new Message();
		emptyMessage.setLanguage("English");
		messages = new ArrayList<Message>();
		messages.add(emptyMessage);

		// Initialization of conditions model
		conditionsModel = new DefaultDiagramModel();
		conditionsModel.setMaxConnections(-1);

		conditionsModel.getDefaultConnectionOverlays().add(
				new ArrowOverlay(20, 20, 1, 1));

		// create a connector
		StraightConnector connector = new StraightConnector();
		connector.setPaintStyle("{strokeStyle:'#98AFC7', lineWidth:2}");
		connector.setHoverPaintStyle("{strokeStyle:'#5C738B'}");

		conditionsModel.setDefaultConnector(connector);

		// Initialization of conclusions model
		conclusionsModel = new DefaultDiagramModel();
		conclusionsModel.setMaxConnections(-1);
		conclusionsModel.getDefaultConnectionOverlays().add(
				new ArrowOverlay(20, 20, 1, 1));
		conclusionsModel.setDefaultConnector(connector);
		

	}
	
	
	public void editNode(){
		Map<String, String> params = FacesContext.getCurrentInstance()
				.getExternalContext().getRequestParameterMap();
		String nodeForRemoveId = params.get("id");
		String panel = params.get("panel");
		
		if (panel.equalsIgnoreCase("conditions")) {
			for (PointElement el : conditions) {
				if (el.getVarName().equals(nodeForRemoveId)) {
					cloneSelectedNode = el.clone();
					break;
				}
			}
		} else {

			for (PointElement el : conclusions) {
				if (el.getVarName().equals(nodeForRemoveId)) {
					cloneSelectedNode = el.clone();
					break;
				}
			}

		}

	}
	
	public void saveEditOfNode() {
		
		if (cloneSelectedNode.getType() == Type.DATA_PROPERTY) {

			DataProperty property = (DataProperty) cloneSelectedNode
					.getProperty();
			if (property.getValue().trim().equals("")) {
				FacesContext.getCurrentInstance().addMessage(
						"msgs",
						new FacesMessage(FacesMessage.SEVERITY_ERROR,
								"Please provide a value for the property", ""));

				return;
			}

			if (property.getDataRange().equalsIgnoreCase("boolean"))
				if (!property.getValue().trim().equals("true")
						|| !property.getValue().trim().equals("false")) {
					FacesContext
							.getCurrentInstance()
							.addMessage(
									"msgs",
									new FacesMessage(
											FacesMessage.SEVERITY_ERROR,
											"Please provide a boolean value for the property",
											""));

					return;
				}
		}

		ArrayList<PointElement> clonedList = new ArrayList<PointElement>();

		// find the panel which the selected node belong to
		// and clone the list (in order to work in one list)
		if (cloneSelectedNode.getPanel().equals("conditions"))
			clonedList = (ArrayList<PointElement>) conditions.clone();
		else
			clonedList = (ArrayList<PointElement>) conclusions.clone();

		// find the index of the old node in the list
		int index = -1;
		index = clonedList.indexOf(cloneSelectedNode);

		// remove the old node, add the cloned node (changes added)
		if (index != -1) {
			clonedList.remove(index);
			clonedList.add(index, cloneSelectedNode);
		}

		// update the corresponding list
		if (cloneSelectedNode.getPanel().equals("conditions"))
			conditions = (ArrayList<PointElement>) clonedList.clone();
		else
			conclusions = (ArrayList<PointElement>) clonedList.clone();

		// remove old Node from diagramModels
		if (cloneSelectedNode.getPanel().equals("conditions")) {
			Element el = Utils.getElementFromID(conditionsModel,
					cloneSelectedNode.getId());
			index = conditionsModel.getElements().indexOf(el);
			conditionsModel.getElements().remove(index);
			el.setData(cloneSelectedNode);
			conditionsModel.getElements().add(index, el);
		} else {
			Element el = Utils.getElementFromID(conclusionsModel,
					cloneSelectedNode.getId());
			index = conclusionsModel.getElements().indexOf(el);
			conclusionsModel.getElements().remove(index);
			el.setData(cloneSelectedNode);
			conclusionsModel.getElements().add(index, el);
		}

	}
	
	

	public DefaultDiagramModel getConclusionsModel() {
		return conclusionsModel;
	}

	public void setConclusionsModel(DefaultDiagramModel conclusionsModel) {
		this.conclusionsModel = conclusionsModel;
	}

	public DefaultDiagramModel getConditionsModel() {
		return conditionsModel;
	}

	public void setConditionsModel(DefaultDiagramModel conditionsModel) {
		this.conditionsModel = conditionsModel;
	}
	
	public PointElement getCloneSelectedNode() {
		return cloneSelectedNode;
	}

	public void setCloneSelectedNode(PointElement cloneSelectedNode) {
		this.cloneSelectedNode = cloneSelectedNode;
	}

	public PointElement setPosition(PointElement el) {
		int x = 0;
		int y = 0;
		if (objectCounter == 0) {
			x = initialX;
			y = initialY;
			objectCounter++;
		} else if (objectCounter == 1) {
			x = initialX + 15;
			y = initialY;
			objectCounter++;
		} else if (objectCounter == 2) {
			x = initialX + 30;
			y = initialY;
			objectCounter = 0;
			initialY = initialY + 8;
		}

		el.setX(x);
		el.setY(y);

		return el;
	}
	
	public void removeNode(){
		
		String nodeForRemoveId = cloneSelectedNode.getVarName();
		String panel = cloneSelectedNode.getPanel();

		int index = -1;
		Element elementForRemove = null;

		if (panel.equals("conditions")) {

			for (PointElement el : conditions) {
				if (el.getId().equals(nodeForRemoveId)) {
					index = conditions.indexOf(el);
					break;
				}
			}

			conditions.remove(index);

			elementForRemove = Utils.getElementFromID(conditionsModel,
					nodeForRemoveId);
			conditionsModel.removeElement(elementForRemove);

		} else {

			for (PointElement el : conclusions) {
				if (el.getVarName().equals(nodeForRemoveId)) {
					index = conclusions.indexOf(el);
					break;
				}
			}

			conclusions.remove(index);

			elementForRemove = Utils.getElementFromID(conclusionsModel,
					nodeForRemoveId);
			conclusionsModel.removeElement(elementForRemove);

		}
	}
	
	public int connectClassWithProperty(PointElement targetElement,
			PointElement sourceElement) {
		// update the list with the connections for the target element
		for (PointElement el : conditions) {
			if (el.getId().equalsIgnoreCase(targetElement.getId())) {
				targetElement.setConnections(el.getConnections());
				break;
			}
		}

		for (PointElement el : conclusions) {
			if (el.getId().equalsIgnoreCase(targetElement.getId())) {
				targetElement.setConnections(el.getConnections());
				break;
			}
		}
		// clone the old target element in order to make the changes
		clonedTargetElement = targetElement.clone();
		int result = 0;
		if (clonedTargetElement.getProperty() instanceof DataProperty) {

			// add element to data property connections
			result = addElementForDataProperty(sourceElement);

		} else {

			// add element to object property connections
			result = addElementForObjectProperty(sourceElement);
		}

		if (result == 1) {
			int index = -1;
			if (sourceElement.getPanel().equals("conditions")) {
				// remove old object from the list
				// add the updated one

				index = this.conditions.indexOf(targetElement);
				if (index != -1) {
					this.conditions.remove(index);
					this.conditions.add(index, clonedTargetElement);
				}

			} else {
				index = this.conclusions.indexOf(targetElement);
				if (index != -1) {
					this.conclusions.remove(index);
					this.conclusions.add(index, clonedTargetElement);
				}
			}
			return 1;
		} else {
			System.out.println("connection failed");
		}
		return 0;
	}
	
	public int addElementForDataProperty(PointElement sourceElement){
		DataProperty property = (DataProperty) clonedTargetElement
				.getProperty();
		String className = property.getClassName();
		int maxConns = 1;

		if (clonedTargetElement.getConnections().size() < maxConns
				&& className.equalsIgnoreCase(sourceElement.getElementName())) {
			clonedTargetElement.getConnections().add(sourceElement);
			return 1;
		}
		
		return 0;
	}
	
	public int addElementForObjectProperty(PointElement sourceElement){
		ObjectProperty property = (ObjectProperty) clonedTargetElement
				.getProperty();
		String className = property.getClassName();
		String rangeClass = property.getRangeOfClasses().get(0);
		int maxConns = 2;
		String name = sourceElement.getElementName();
		
		if (clonedTargetElement.getConnections().size() < maxConns
				&& (className.equalsIgnoreCase(name)||
						rangeClass.equalsIgnoreCase(name))) {
			clonedTargetElement.getConnections().add(sourceElement);
			return 1;
		}
		
		return 0;
	}

	public void connectBuiltinMethodWithProperty(PointElement sourceElement,
			PointElement targetElement) {
		// update the connections of the source element (built in method)
		for (PointElement el : conditions) {
			if (el.getId().equalsIgnoreCase(sourceElement.getId())) {
				sourceElement.setConnections(el.getConnections());
				break;
			}
		}

		for (PointElement el : conclusions) {
			if (el.getId().equalsIgnoreCase(sourceElement.getId())) {
				sourceElement.setConnections(el.getConnections());
				break;
			}
		}

		PointElement clonedSourceElement = sourceElement.clone();
		int i = clonedSourceElement.getMethod().getNumberOfParams();
		if (clonedSourceElement.getConnections().size() < i) {
			clonedSourceElement.getConnections().add(targetElement.clone());
		}

		int index = -1;
		if (sourceElement.getPanel().equals("conditions")) {
			// remove old object from the list
			// add the updated one

			index = this.conditions.indexOf(sourceElement);
			if (index != -1) {
				this.conditions.remove(index);
				this.conditions.add(index, clonedSourceElement);
			}

		} else {
			index = this.conclusions.indexOf(sourceElement);
			if (index != -1) {
				this.conclusions.remove(index);
				this.conclusions.add(index, clonedSourceElement);
			}
		}
	}

	public void onConnect(ConnectEvent event) {

		targetElement = (PointElement) event.getTargetElement()
				.getData();
		sourceElement = (PointElement) event.getSourceElement()
				.getData();
		int result = 0;
		// 1st case, connect a class with a property
		if (sourceElement.getType() == Type.CLASS)
			result = connectClassWithProperty(targetElement, sourceElement);
		// 2nd case, connect a built in method with a property
		else if (sourceElement.getType() == Type.BUILTIN_METHOD)
			connectBuiltinMethodWithProperty(sourceElement, targetElement);
		
		if (result == 0) {
			if (conditionsModel.getConnections().size() > 0)
				conditionsModel.getConnections().remove(
						conditionsModel.getConnections().size() - 1);
			System.out.println("incorrect connection");
		}
			
		
		
	}

	public void onDisconnect(DisconnectEvent event) {
		boolean flag = false;
		
		targetElement = (PointElement) event.getTargetElement()
				.getData();
		sourceElement = (PointElement) event.getSourceElement()
				.getData();
		
		//update the list with the connections for the target element
		for(PointElement el: conditions){
			if(el.getId().equalsIgnoreCase(targetElement.getId())){
				targetElement.setConnections(el.getConnections());
				flag = true;
				break;
			}
		}
		
		if(!flag)
		for(PointElement el: conclusions){
			if(el.getId().equalsIgnoreCase(targetElement.getId())){
				targetElement.setConnections(el.getConnections());
				break;
			}
		}
		
		//remove the disconnected node from the connections of the property
        int indexOfNodeToRemove = -1;
        indexOfNodeToRemove = targetElement.getConnections().indexOf(sourceElement);
        if(indexOfNodeToRemove!=-1)
        	targetElement.getConnections().remove(indexOfNodeToRemove);
        
        //clone the old property
        clonedTargetElement = targetElement.clone();

		//add the new property node in the corresponding list
		int index = -1;
		if (flag) {
			index = this.conditions.indexOf(targetElement);
			if (index != -1) {
				this.conditions.remove(index);
				this.conditions.add(index, clonedTargetElement);
			}

		} else {
			index = this.conclusions.indexOf(targetElement);
			if (index != -1) {
				this.conclusions.remove(index);
				this.conclusions.add(index, clonedTargetElement);
			}
		}
	}

	public void onConnectionChange(ConnectionChangeEvent event) {
		
		ArrayList<PointElement> cloneList = new ArrayList<PointElement>();
		sourceElement = (PointElement) event.getNewSourceElement().getData();
		targetElement = (PointElement) event.getNewTargetElement().getData();
		originalTargetElement = (PointElement) event.getOriginalTargetElement()
				.getData();
		
		boolean flag = Utils.findPanelOfElement(sourceElement.getVarName(),
				conditions, conclusions);
		if (flag)
			cloneList = (ArrayList<PointElement>) conditions.clone();
		else
			cloneList = (ArrayList<PointElement>) conclusions.clone();

		for (PointElement el : cloneList) {

			if (el.getId().equalsIgnoreCase(
					originalTargetElement.getId()))
				originalTargetElement.setConnections(el.getConnections());

			if (el.getVarName().equalsIgnoreCase(targetElement.getVarName()))
				targetElement.setConnections(el.getConnections());

		}

		// remove sourceElement from the connections of the old target
		int index = -1;
		index = originalTargetElement.getConnections().indexOf(sourceElement);
		if (index != -1)
			originalTargetElement.getConnections().remove(index);

		clonedOriginalTargetElement = originalTargetElement.clone();
		
//		int result = 0;
//		if (sourceElement.getType() == Type.CLASS)
//			result = connectClassWithProperty(targetElement, sourceElement);
//		// 2nd case, connect a built in method with a property
//		else if (sourceElement.getType() == Type.BUILTIN_METHOD)
//			connectBuiltinMethodWithProperty(sourceElement, targetElement);

		clonedTargetElement = targetElement.clone();

		// remove old targets and add the updated targets
		index = cloneList.indexOf(originalTargetElement);
		cloneList.remove(index);
		cloneList.add(index, clonedOriginalTargetElement);

		index = cloneList.indexOf(targetElement);
		cloneList.remove(index);
		cloneList.add(index, clonedTargetElement);

		//update the lists
		if (flag)
			conditions = (ArrayList<PointElement>) cloneList.clone();
		else
			conclusions = (ArrayList<PointElement>) cloneList.clone();
		
//		if (result == 0) {
//			if (flag && conditionsModel.getConnections().size() > 0)
//				conditionsModel.getConnections().remove(
//						conditionsModel.getConnections().size() - 1);
//			else if (!flag && conclusionsModel.getConnections().size() > 0)
//				conclusionsModel.getConnections().remove(
//						conclusionsModel.getConnections().size() - 1);
//
//			System.out.println("incorrect connection");
//
//		}
		
	}

	private void createOntologyTree(Ontology ontology) {

		List<ArrayList<OntologyClass>> list = main.getAllClasses();
		OntologyClass cl = null;
		root = new DefaultTreeNode("Solutions Ontology", null);

		for (ArrayList<OntologyClass> temp : list) {
			cl = temp.get(0);
			getTreeNodeOfConcept(cl, root);
		}

	}

	private DefaultTreeNode getTreeNodeOfConcept(OntologyClass cl, TreeNode root) {

		DefaultTreeNode node = new DefaultTreeNode(cl.getClassName(), root);
		if (cl.getChildren().size() > 0)
			for (int i = 0; i < cl.getChildren().size(); i++) {
				getTreeNodeOfConcept(cl.getChildren().get(i), node);
			}
		return node;

	}
	
	public void saveRule() throws IOException{
		
		
		// feedback rule
		if(flag){
			//write messages in jsonld file
			
			
		}
		
		if (ruleName.trim().equals("")) {
			FacesContext.getCurrentInstance().addMessage(
					"msgs",
					new FacesMessage(FacesMessage.SEVERITY_ERROR,
							"Please provide a rule name", ""));

			return;
		}

		String finalFileName = newFileName.trim();
		if (newFileName.isEmpty() && !oldFileName.trim().isEmpty())
			finalFileName = oldFileName;

		if (finalFileName.trim().equals("")) {
			FacesContext.getCurrentInstance().addMessage(
					"msgs",
					new FacesMessage(FacesMessage.SEVERITY_ERROR,
							"Please create a new file or select an existing",
							""));
			return;
		}

		
		
		//create the rule
		String rule = Utils.createRule(conditions, conclusions, ruleName);

		RequestContext rc = RequestContext.getCurrentInstance();
		rc.execute("PF('newRuleDialog').hide()");
		
		//export the rule
		if (!rule.isEmpty())
			FileDownloadController.writeGsonAndExportFile(newFileName, rule);
		
	}
	
	public void onFileUpload(FileUploadEvent event) throws IOException {

		feedbackFile = event.getFile().getFileName();

		fileStream = event.getFile().getInputstream();
		
		System.out.println(feedbackFile);

	}
	
	public void exportJsonLdFile() throws IOException{
		if (!feedbackFile.isEmpty())
			jsonString = Utils.writeMessagesInJsonLdFile(fileStream, messages);

		FileDownloadController.writeGsonAndExportFile(feedbackFile, jsonString);
	}

	public void onNodeSelect() {

		for (ArrayList<OntologyClass> temp : main.getAllClasses()) {
			searchChildrenByName(temp.get(0));

		}

	}

	public boolean searchChildrenByName(OntologyClass tempClass) {
		boolean flag = false;
		if (tempClass.getClassName().equals(this.selectedNode.getData())) {

			datatypes = tempClass.getDataProperties();
			objects = tempClass.getObjectProperties();
			instances = tempClass.getInstances();
			return true;

		} else {
			for (OntologyClass tempChild : tempClass.getChildren()) {
				flag = searchChildrenByName(tempChild);
				if (flag)
					break;
			}
		}

		return false;
	}

	public void createMethodElement(String panelID){
		PointElement networkElement = new PointElement();
		networkElement.setElementName(this.selectedMethod
				.getUsingName().toString());
		networkElement.setType(Type.BUILTIN_METHOD);
		networkElement.setRenderEditText(false);
		networkElement.setVarName(setVariableName());
		networkElement.setId(networkElement.getVarName());
		networkElement = setPosition(networkElement);
		networkElement.setMethod(selectedMethod.clone());
		Element element = new Element(networkElement,
				String.valueOf(networkElement.getX() + "em"),
				String.valueOf(networkElement.getY() + "em"));
		EndPoint endPointCA = Utils.createRectangleEndPoint(EndPointAnchor.BOTTOM);
		endPointCA.setSource(true);
		element.addEndPoint(endPointCA);
		
		moveToPanel(panelID, networkElement, element);
	}
	
	public void createClassElement(String panelID){
		PointElement networkElement = new PointElement();
		networkElement.setElementName(this.selectedNode.getData().toString());
		networkElement.setType(Type.CLASS);
		networkElement.setRenderEditText(false);
		networkElement.setVarName(setVariableName());
		networkElement.setId(networkElement.getVarName());
		networkElement = setPosition(networkElement);
		Element element = new Element(networkElement,
				String.valueOf(networkElement.getX() + "em"),
				String.valueOf(networkElement.getY() + "em"));
		EndPoint endPointCA = Utils.createRectangleEndPoint(EndPointAnchor.BOTTOM);
		endPointCA.setSource(true);
		element.addEndPoint(endPointCA);
		moveToPanel(panelID, networkElement, element);
		
	}
	
	public void createInstanceElement(String panelID){
		PointElement networkElement = new PointElement();
		networkElement.setElementName(this.selectedInstance.toString());
		networkElement.setType(Type.INSTANCE);
		networkElement.setRenderEditText(false);
		networkElement.setVarName(setVariableName());
		networkElement.setId(networkElement.getVarName());
		networkElement = setPosition(networkElement);
		Element element = new Element(networkElement,
				String.valueOf(networkElement.getX() + "em"),
				String.valueOf(networkElement.getY() + "em"));
		EndPoint endPointCA = Utils.createRectangleEndPoint(EndPointAnchor.BOTTOM);
		endPointCA.setSource(true);
		element.addEndPoint(endPointCA);
		moveToPanel(panelID, networkElement, element);
		
	}
	
	public void createPropertyElement(String panelID){
		OntologyProperty property = new OntologyProperty("", "");
		Type type = Type.DATA_PROPERTY;
		
		if (this.getSelectedDataProperty()!=null) {
			property = this.getSelectedDataProperty().clone();
		} else {
			property = this.getSelectedObjectProperty().clone();
			type = Type.OBJECT_PROPERTY;
		}

		//create the nodeElement for conditions list
		PointElement propElement = new PointElement();
		propElement.setElementName(property.getPropertyName());
		propElement.setVarName(setVariableName());
		propElement.setId(propElement.getVarName());
		propElement = setPosition(propElement);
		propElement.setType(type);
		if (type == Type.DATA_PROPERTY)
			propElement.setRenderEditText(true);
		
		propElement.setProperty(property);
		
		//create element for the model diagram
		Element element = new Element(propElement,
				String.valueOf(propElement.getX() + "em"),
				String.valueOf(propElement.getY() + "em"));
		EndPoint endPointCA = Utils.createDotEndPoint(EndPointAnchor.AUTO_DEFAULT);
		endPointCA.setTarget(true);
		element.addEndPoint(endPointCA);
		 
		moveToPanel(panelID, propElement, element);
		
		this.selectedDataProperty = null;
		this.selectedObjectProperty = null;
		
	}


	public void moveToPanel(String panelID, PointElement networkElement,
			Element element) {

		if (panelID.contains("pan1")) {
			conditionsModel.addElement(element);
			networkElement.setPanel("conditions");
			conditions.add(networkElement);
		} else {
			conclusionsModel.addElement(element);
			networkElement.setPanel("conclusions");
			conclusions.add(networkElement);
		}

	}

	public void clearPanel(String panelID) {

		if (panelID.contains("pan1")) {
			conditionsModel = new DefaultDiagramModel();
			conditions = new ArrayList<PointElement>();
		} else {
			conclusionsModel = new DefaultDiagramModel();
			conclusions = new ArrayList<PointElement>();
		}
	}
	
	public void removeMessageFromList() {
		messages.remove(messageForRemove);
	}

	public void addMessageToList() {
		Message newMessage = new Message();
		messages.add(newMessage);
	}
	

	public String setVariableName(){
		return "X_"+counter++;
	}
	
	

	public Main getMain() {
		return main;
	}

	public void setMain(Main main) {
		this.main = main;
	}

	public DefaultTreeNode getRoot() {
		return root;
	}

	public void setRoot(DefaultTreeNode root) {
		this.root = root;
	}

	public TreeNode getSelectedNode() {
		return selectedNode;
	}

	public void setSelectedNode(TreeNode selectedNode) {
		this.selectedNode = selectedNode;
	}

	public String getRuleName() {
		return ruleName;
	}

	public void setRuleName(String ruleName) {
		this.ruleName = ruleName;
	}

	public String getNewFileName() {
		return newFileName;
	}

	public void setNewFileName(String newFileName) {
		this.newFileName = newFileName;
	}

	public String getOldFileName() {
		return oldFileName;
	}

	public void setOldFileName(String oldFileName) {
		this.oldFileName = oldFileName;
	}

	public List<DataProperty> getDatatypes() {
		return datatypes;
	}

	public void setDatatypes(List<DataProperty> datatypes) {
		this.datatypes = datatypes;
	}

	public List<ObjectProperty> getObjects() {
		return objects;
	}

	public void setObjects(List<ObjectProperty> objects) {
		this.objects = objects;
	}

	public List<String> getInstances() {
		return instances;
	}

	public void setInstances(List<String> instances) {
		this.instances = instances;
	}

	
	public ArrayList<PointElement> getConditions() {
		return conditions;
	}

	public void setConditions(ArrayList<PointElement> conditions) {
		this.conditions = conditions;
	}

	public String getNodeForRemove() {
		return nodeForRemove;
	}

	public void setNodeForRemove(String nodeForRemove) {
		this.nodeForRemove = nodeForRemove;
	}

	public OntologyProperty getSelectedDataProperty() {
		return selectedDataProperty;
	}

	public void setSelectedDataProperty(OntologyProperty selectedDataProperty) {
		this.selectedDataProperty = selectedDataProperty;
	}

	public OntologyProperty getSelectedObjectProperty() {
		return selectedObjectProperty;
	}

	public void setSelectedObjectProperty(OntologyProperty selectedObjectProperty) {
		this.selectedObjectProperty = selectedObjectProperty;
	}

	public BuiltinMethod getSelectedMethod() {
		return selectedMethod;
	}

	public void setSelectedMethod(BuiltinMethod selectedMethod) {
		this.selectedMethod = selectedMethod;
	}

	public List<Message> getMessages() {
		return messages;
	}

	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}

	public Message getMessageForRemove() {
		return messageForRemove;
	}

	public void setMessageForRemove(Message messageForRemove) {
		this.messageForRemove = messageForRemove;
	}

	public String getFeedbackFile() {
		return feedbackFile;
	}

	public void setFeedbackFile(String feedbackFile) {
		this.feedbackFile = feedbackFile;
	}

	public String getSelectedInstance() {
		return selectedInstance;
	}

	public void setSelectedInstance(String selectedInstance) {
		this.selectedInstance = selectedInstance;
	}

}