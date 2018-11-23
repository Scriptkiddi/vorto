/**
 * Copyright (c) 2015-2018 Bosch Software Innovations GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * The Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 * Bosch Software Innovations GmbH - Please refer to git log
 */
package org.eclipse.vorto.model.runtime;

import java.util.*;

import org.eclipse.vorto.model.FunctionblockModel;
import org.eclipse.vorto.model.ModelProperty;
import org.eclipse.vorto.model.PrimitiveType;

public class FunctionblockValue implements IValidatable {
	
	private FunctionblockModel meta;
	
	private List<PropertyValue> status = new ArrayList<PropertyValue>();
	private List<PropertyValue> configuration = new ArrayList<PropertyValue>();
	private List<FBEventValue> events = new ArrayList<FBEventValue>();
	
	public FunctionblockValue(FunctionblockModel meta) {
		this.meta = meta;
	}

	public FunctionblockModel getMeta() {
		return this.meta;
	}

	public List<PropertyValue> getStatus() {
		return Collections.unmodifiableList(this.status);
	}
	
	public PropertyValue getStatusProperty(String propertyName) {
		ListIterator<PropertyValue> iterator = this.status.listIterator();
		while (iterator.hasNext()){
			PropertyValue propertyValue = iterator.next();
			if(propertyValue.getMeta().getName().equals(propertyName)){
				return propertyValue;
			}
		}
		return null;
	}

	public List<PropertyValue> getConfiguration() {
		return Collections.unmodifiableList(this.status);
	}
	
	public PropertyValue getConfigurationProperty(String propertyName) {
		ListIterator<PropertyValue> iterator = this.configuration.listIterator();
		while (iterator.hasNext()){
			PropertyValue propertyValue = iterator.next();
			if(propertyValue.getMeta().getName().equals(propertyName)){
				return propertyValue;
			}
		}
		return null;
	}

    public FunctionblockValue withStatusProperty(String name, Object value) {
    	Optional<ModelProperty> mp = meta.getStatusProperty(name);
    	if (mp == null) {
    		throw new IllegalArgumentException("Status property with given name is not defined in Function Block");
    	}
    	PropertyValue pv = this.getStatusProperty(name);
    	if (pv != null) {
    		pv.setValue(value);
    	} else {
			this.status.add(new PropertyValue(mp, value));
    	}
    			
		return this;
	}
	
    public FunctionblockValue withConfigurationProperty(String name, Object value) {
    	ModelProperty mp = meta.getConfigurationProperty(name);
    	if (mp == null) {
    		throw new IllegalArgumentException("Configuration property with given name is not defined in Function Block");
    	}
		
    	Optional<PropertyValue> pv = this.getConfigurationProperty(name);
    	if (pv != null) {
    		pv.setValue(value);
    	} else {
			this.configuration.add(new PropertyValue(mp, value));
    	}
		
		return this;
	}
    
    public FunctionblockValue withEvent(FBEventValue event) {
    	this.events.add(event);
    	return this;
    }

	@Override
	public String toString() {
		return "FunctionblockData [status=" + status + ", configuration=" + configuration + "]";
	}

	@Override
	public ValidationReport validate() {
		ValidationReport report = new ValidationReport();
		
		for (ModelProperty statusProperty : meta.getStatusProperties()) {
			checkProperty(getStatus(), statusProperty, meta.getId().getName().toLowerCase() + "/status",report);
		}

		for (ModelProperty configProperty : meta.getConfigurationProperties()) {
			checkProperty(getConfiguration(), configProperty,
					meta.getId().getName().toLowerCase() + "/configuration",report);
		}
		return report;
	}
	
	private void checkProperty(List<PropertyValue> properties, ModelProperty property, String path, ValidationReport report) {
		PropertyValue mpd = null;
		ListIterator<PropertyValue> iterator = properties.listIterator();
		while (iterator.hasNext()){
			PropertyValue propertyValue = iterator.next();
			if(propertyValue.getMeta().getName().equals(property)){
				mpd = propertyValue;
				break;
			}
		}
		if (property.isMandatory()
				&& mpd == null) {
			report.addItem(property, "Mandatory field " + path + "/" + property.getName() + " is missing");
		} else {
			if (mpd != null && property.getType() instanceof PrimitiveType) {
				checkPrimitiveTypeValue(path, mpd.getValue(), property,report);
			}
		}
	}

	private static void checkPrimitiveTypeValue(String path, Object propertyValue, ModelProperty property,ValidationReport report) {
		PrimitiveType type = (PrimitiveType) property.getType();
		if (type == PrimitiveType.STRING && !(propertyValue instanceof String)) {
			report.addItem(property,"Field " + path + "/" + property.getName() + " must be of type 'String'");
		} else if (type == PrimitiveType.BOOLEAN && !(propertyValue instanceof Boolean)) {
			report.addItem(property,"Field " + path + "/" + property.getName() + " must be of type 'Boolean'");
		} else if (type == PrimitiveType.DOUBLE && !(propertyValue instanceof Double)) {
			report.addItem(property,"Field " + path + "/" + property.getName() + " must be of type 'Double'");
		} else if (type == PrimitiveType.FLOAT && !isFloat(propertyValue)) {
			report.addItem(property,"Field " + path + "/" + property.getName() + " must be of type 'Float'");
		} else if (type == PrimitiveType.INT && !isInteger(propertyValue)) {
			report.addItem(property,"Field " + path + "/" + property.getName() + " must be of type 'Integer'");
		} else if (type == PrimitiveType.LONG && !isLong(propertyValue)) {
			report.addItem(property,"Field " + path + "/" + property.getName() + " must be of type 'Long'");
		} else if (type == PrimitiveType.BASE64_BINARY && !(propertyValue instanceof String)) {
			report.addItem(property,
					"Field " + path + "/" + property.getName() + " must be a Base64-encoded 'String'");
		}
	}

	private static boolean isInteger(Object value) {
		return value instanceof Integer;
	}
	
	private static boolean isFloat(Object value) {
		return value instanceof Float || value instanceof Double;
	}

	private static boolean isLong(Object value) {
		return value instanceof Integer || value instanceof Long;
	}

	
	public Map<String, Object> serialize() {
		Map<String, Object> result = new HashMap<String, Object>();
		
		Map<String,Object> statusProperties = new HashMap<String, Object>();
		for (PropertyValue statusProperty : status) {
			statusProperties.put(statusProperty.getMeta().getName(), statusProperty.getValue());
		}
		result.put("status", statusProperties);
		
		Map<String,Object> configuration = new HashMap<String, Object>();
		for (PropertyValue configProperty : this.configuration) {
			configuration.put(configProperty.getMeta().getName(), configProperty.getValue());
		}
		if (!configuration.isEmpty()) {
			result.put("configuration", configuration);
		}
				
		return result;
	}
}
