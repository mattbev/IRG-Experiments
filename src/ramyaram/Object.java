package ramyaram;

import tools.Vector2d;

/**
 * Representation of an object in the task
 * Each object is part of an object class (objects of the same class are assumed to behave similarly)
 */
public class Object {
	private int objClassId;
	private int itype;
	private Vector2d gridPos;
	
	public Object(int objClassId, int itype, Vector2d gridPos) {
		this.objClassId = objClassId;
		this.itype = itype;
		this.gridPos = gridPos.copy();
	}

	public int getItype() {
		return itype;
	}

	public int getObjClassId() {
		return objClassId;
	}

	public Vector2d getGridPos() {
		return gridPos;
	}
	
	public int hashCode(){
		return 2;
	}
	
	public boolean equals(java.lang.Object obj_param){
		Object obj = (Object)obj_param;
		return objClassId == obj.objClassId && itype == obj.itype
				&& gridPos.equals(obj.gridPos);
	}

	public String toString() {
		return gridPos.toString();
	}
}
