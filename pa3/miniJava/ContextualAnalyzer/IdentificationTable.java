package miniJava.ContextualAnalyzer;

import miniJava.AbstractSyntaxTrees.*;

import java.util.ArrayList;
import java.util.HashMap;

public class IdentificationTable implements Cloneable{

	private HashMap<String, Declaration> currlevelidt; // latest idTable
    public ArrayList<HashMap<String, Declaration>> allLevelIdts;

    // Constructors: one for first level, one for adding predetermined levels
    public IdentificationTable() {
        currlevelidt = new HashMap<String, Declaration>(); 
        allLevelIdts = new ArrayList<HashMap<String, Declaration>>();
    }

    public IdentificationTable(HashMap<String, Declaration> idt) {
        currlevelidt = idt;
        allLevelIdts = new ArrayList<HashMap<String, Declaration>>();
        allLevelIdts.add(currlevelidt);
    }

    // Getters
    public int getHighestLevel() { // highest level
        return allLevelIdts.size() - 1;
    }

    public HashMap<String, Declaration> getCurrentIdt() {
    	return this.currlevelidt;
    }
    
    // Open/Close Scopes
    public void openScope() { // level++
        currlevelidt = new HashMap<String, Declaration>();
        allLevelIdts.add(currlevelidt);
    }

    public void openScope(HashMap<String, Declaration> newidt) { // level++
        currlevelidt = newidt;
        allLevelIdts.add(currlevelidt);
    }

    public void closeScope() { // closes highest level in IdTable; level--
        allLevelIdts.remove(this.getHighestLevel());
        currlevelidt = allLevelIdts.get(this.getHighestLevel());
    }

    // Enter
    public Object enter(String id, Declaration decl) {
    	if (currlevelidt.get(id) == null) { // id doesn't exist yet
    		currlevelidt.put(id, decl);
    	} else return null;
    	return decl;
    }
    
    public Object enterDecl(Declaration decl) {
        return enter(decl.id.spelling, decl);
    }
    
    // Supporting methods
    public boolean idExistsInCurrentLevel(String id) {
        for (String currentId : currlevelidt.keySet()) {
            if (id.equals(currentId)) return true;
        }
        return false;
    }

    public void swapIdt(int idt1, int idt2) {
    	HashMap<String, Declaration> table1 = allLevelIdts.get(idt1);
        HashMap<String, Declaration> table2 = allLevelIdts.get(idt2);
        allLevelIdts.remove(idt1);
        allLevelIdts.add(idt1, table2);
        allLevelIdts.remove(idt2);
        allLevelIdts.add(idt2, table1);
    }
    
    /*public Object clone() { //need to copy
		try{
			return super.clone();
		}
		catch(Exception e){ return null; }
	}*/
    
    //Retrieve
    public Declaration retrieve(String id) {
        for (int i = getHighestLevel(); i >= 0; i--) {
        	HashMap<String, Declaration> idt = allLevelIdts.get(i);
            if (idt.get(id) != null) {
                return idt.get(id);
            }
        }
		
        return null;
    }

    public Declaration retrieveClass(String id) {
        for (int i = getHighestLevel(); i >= 0; i--) {
        	HashMap<String, Declaration> idt = allLevelIdts.get(i);
            if (idt.get(id) != null && idt.get(id) instanceof ClassDecl) {
                return idt.get(id);
            }
        }

        return null;
    }

    public Declaration retrieveMethod(String id) {
    	for (int i = getHighestLevel(); i >= 0; i--) {
    		HashMap<String, Declaration> idt = allLevelIdts.get(i);
            if (idt.get(id) != null && idt.get(id) instanceof MethodDecl) {
                return idt.get(id);
            }
        }

        return null;
    }
    
    public Declaration retrieveMember(String id) {
    	for (int i = getHighestLevel(); i >= 0; i--) {
    		HashMap<String, Declaration> idt = allLevelIdts.get(i);
            if (idt.get(id) != null && idt.get(id) instanceof MemberDecl) {
                return idt.get(id);
            }
        }

        return null;
    }
}
