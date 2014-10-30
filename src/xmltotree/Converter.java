package xmltotree;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.xerces.impl.dv.xs.XSSimpleTypeDecl;
import org.apache.xerces.impl.xs.XSComplexTypeDecl;
import org.apache.xerces.impl.xs.XSElementDecl;
import org.apache.xerces.impl.xs.XSModelGroupImpl;
import org.apache.xerces.impl.xs.XSParticleDecl;
import org.apache.xerces.impl.xs.XSWildcardDecl;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSImplementation;
import org.apache.xerces.xs.XSLoader;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSNamedMap;
import org.apache.xerces.xs.XSObject;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSTerm;
import org.apache.xerces.xs.XSTypeDefinition;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

public class Converter {
	
	private static StringBuilder sb = new StringBuilder();

	public static void main(String[] args) throws Exception {
        DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
        XSImplementation impl = (XSImplementation) registry.getDOMImplementation("XS-Loader");
		XSLoader schemaLoader = impl.createXSLoader(null);
        DOMConfiguration config = schemaLoader.getConfig();
        // set validation feature
        config.setParameter("validate", Boolean.TRUE);		
		XSModel model = schemaLoader.loadURI("sample/sampleHomeQuoteMessage.xsd");
		
        if (model != null) {
            XSNamedMap map = model.getComponents(XSConstants.ELEMENT_DECLARATION);
            if (map.getLength() != 0) {
                for (int i = 0; i < map.getLength(); i++) {
                    XSObject item = map.item(i);
                    traversItem(item, null);
                }
            }
        }
        
        generateOutputFile("html\\template.html", "html\\out.html");
	}
	
	private static void traversItem(XSObject item, List<Item> parents) {
        if (item instanceof XSElementDecl) {
        	XSElementDecl element = (XSElementDecl) item;  
            XSTypeDefinition fType = element.getTypeDefinition();
            if (fType instanceof XSComplexTypeDecl) {
            	XSComplexTypeDecl fTypeDecl = (XSComplexTypeDecl) fType;

            	List<Item> parentsList = new ArrayList<Item>();
            	
            	if (parents != null) {
            		parentsList.addAll(parents);	
            	}
            	
            	Item newItem = new Item();
            	newItem.setElement(element);
            	parentsList.add(newItem);

                XSParticle fParticle = fTypeDecl.getParticle();

                int repeat = 0;
                for (Item i : parentsList) {
                	if (newItem.equals(i)) {
                		repeat++;
                	}
                }
                
                if (repeat < 3) {
                	sb.append("<li><span>" + item.getName() + " - " + getPath(parents, item.getName()) + "</span>\n<ul>\n");
                	traversItem(fParticle, parentsList);
                	sb.append("</ul>\n</li>\n");
                }

            } else if (fType instanceof XSSimpleTypeDecl) {
            	//XSSimpleTypeDecl fTypeDecl = (XSSimpleTypeDecl) fType;
            	//System.out.println("{" + item.getNamespace() + "}" + "[" + item.getName() + "]" + fTypeDecl.getTypeName() + " - " + fTypeDecl.getBaseType().getName());            	
            	
            	sb.append("<li>" + item.getName() + " - " + getPath(parents, item.getName()) + "</li>\n");

            }

        } else if (item instanceof XSParticleDecl) {
            XSParticleDecl fParticleDecl = (XSParticleDecl) item;
            XSTerm fValue = fParticleDecl.fValue;

            if (fValue instanceof XSModelGroupImpl) {
                XSModelGroupImpl modelGroup = (XSModelGroupImpl) fValue;            
                XSParticleDecl[] fParticles = modelGroup.fParticles;
                if (fParticles != null) {
	                for (XSParticleDecl fp : fParticles) {
	                	traversItem(fp, parents);
	                }      
                }
                
            } else if (fValue instanceof XSElementDecl) {
            	traversItem((XSElementDecl) fValue, parents);
            	
            } else if (fValue instanceof XSWildcardDecl) {
            	//System.out.println(">> XSWildcardDecl");
            	sb.append("<li> XSWildcardDecl" + item.getName() + "</li>\n");
            }

        }
	}
	
	private static void generateOutputFile(String in, String out) throws IOException {
	    BufferedReader reader = new BufferedReader(new FileReader(in));
	    PrintWriter writer = new PrintWriter(new FileWriter(out));
	    String line = null;

	    while ((line = reader.readLine()) != null) {
	        writer.println(line.replaceAll("_LIST_", sb.toString()));
	    }

	    reader.close();
	    writer.close();		
	}
	
	private static String getPath(List<Item> parents, String last) {
    	String path = "";
    	if (parents != null) {
	    	for (Item i : parents) {
	    		path += "&#92;" + i.getElementName();
	    	}
	    	path += "&#92;" + last;
    	}
    	
    	return path; 
	}

}
