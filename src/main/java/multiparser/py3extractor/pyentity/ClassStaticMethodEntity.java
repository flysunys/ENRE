package multiparser.py3extractor.pyentity;

public class ClassStaticMethodEntity extends PyFunctionEntity{

    public ClassStaticMethodEntity(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        String str = "";
        str += "\n(ClassStaticmethod:";
        str += ("id:" + Integer.toString(id) + ",");
        str += ("name:" + name + ",");
        str += ("parentId:" + parentId + ",");
        str += ("childrenIds:" + childrenIds + ",");
        str += ("relations:" + relations);
        str += ")\n";
        return str;

    }
}
