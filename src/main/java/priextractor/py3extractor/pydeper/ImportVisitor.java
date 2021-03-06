package priextractor.py3extractor.pydeper;

import uerr.AbsEntity;
import uerr.AbsFLDEntity;
import entitybuilder.pybuilder.pyentity.ImportStmt;
import entitybuilder.pybuilder.pyentity.ModuleEntity;
import entitybuilder.pybuilder.pyentity.PyFunctionEntity;
import util.Configure;
import util.StringUtil;


import java.util.ArrayList;
import java.util.HashMap;

public class ImportVisitor extends DepVisitor {

    private HashMap<String, Integer> pkg2IdMap = new HashMap<String, Integer>();
    private HashMap<String, Integer> mod2IdMap = new HashMap<String, Integer>();

    public ImportVisitor() {
        buildPkgMap(); //fullpathname->id
        //buildModuleMap(); //simplename->id
    }

    @Override
    public void setDep() {
        bindPkg2Pkg();
        bindMod2Pkg();

        setImportDep(); //import
    }

    /**
     * build parent-child relation between pkgs
      */
    private void bindPkg2Pkg() {
        for(AbsEntity entity :  singleCollect.getEntities()) {
            if(entity instanceof AbsFLDEntity) {
                String dirName = StringUtil.deleteLastStrByPathDelimiter(((AbsFLDEntity) entity).getFullPath());
                int parentId = -1;
                if(pkg2IdMap.containsKey(dirName)) {
                    parentId = pkg2IdMap.get(dirName);
                }

                singleCollect.getEntities().get(entity.getId()).setParentId(parentId);
                if(parentId != -1
                        && singleCollect.getEntities().get(parentId) instanceof AbsFLDEntity) {
                    singleCollect.getEntities().get(parentId).addChildId(entity.getId());
                }
            }
        }
    }

    /**
     * module's parent is package.
     * is it possible that a module's parent is also a module.?????????????/
     *
     *
     * build parent-child relation between module and pkgs
     */
    private void bindMod2Pkg() {
        for (AbsEntity entity : singleCollect.getEntities()) {
            if(entity instanceof ModuleEntity) {
                String dirName = StringUtil.deleteLastStrByPathDelimiter(entity.getName());
                int parentId = -1;
                if(pkg2IdMap.containsKey(dirName)) {
                    parentId = pkg2IdMap.get(dirName);
                }
                singleCollect.getEntities().get(entity.getId()).setParentId(parentId);
                if(parentId != -1
                        && singleCollect.getEntities().get(parentId) instanceof AbsFLDEntity) {
                    singleCollect.getEntities().get(parentId).addChildId(entity.getId());
                }
            }
        }
    }

    /**
     * map["packagename"] = packageId
     * @return
     */
    private void buildPkgMap() {
        for(AbsEntity entity : singleCollect.getEntities()) {
            if(entity instanceof AbsFLDEntity) {
                this.pkg2IdMap.put(((AbsFLDEntity) entity).getFullPath(), entity.getId());
            }
        }
    }

    private void buildModuleMap() {
        for(AbsEntity entity : singleCollect.getEntities()) {
            if(entity instanceof ModuleEntity) {
                this.mod2IdMap.put(((ModuleEntity) entity).getModuleSimpleName(), entity.getId());
            }
        }
    }


    /**
     * if find the import uerr, then save to uerr relation
     */
    private void setImportDep() {
        for(AbsEntity entity : singleCollect.getEntities()) {
            ArrayList<ImportStmt> importStmts = null;
            if(entity instanceof ModuleEntity) {
                importStmts = ((ModuleEntity) entity).getImportStmts();
            }
            else if(entity instanceof PyFunctionEntity) {
                importStmts = ((PyFunctionEntity) entity).getImportStmts();
            }
            if(importStmts == null) {
                continue;
            }

            for (int index = 0; index < importStmts.size(); index++) {
                ImportStmt importStmt = importStmts.get(index);
                String impstr = importStmt.getImpor();
                if(!importStmt.getFrom().equals(Configure.NULL_STRING)) {
                    impstr = (importStmt.getFrom() + Configure.DOT + impstr);
                }
                //System.out.println("looking for " + impstr);
                int scope = -1; //should get it based on from.
                int id = findImportedEntity(impstr, scope);

                if(id != -1) {
                    //save (importedID, importsList_index) into uerr
                    saveId2Id(entity.getId(), id, index);
                    saveRelation(entity.getId(), id, Configure.RELATION_IMPORT, Configure.RELATION_IMPORTED_BY);
                    //System.out.println("setImportDep: find " + singleCollect.getEntities().get(id).getName());
                }
                else  {
                    //System.out.println("setImportDep: cannot find " + impstr);
                }
            }
        }
    }



    /**
     *
     * @param impstr
     * @param scope
     * @return
     */
    private int findImportedEntity(String impstr, int scope) {
        while(impstr.contains(Configure.DOT)) {
            String [] arr = impstr.split("\\.");
            String pre = arr[0];
            String post = impstr.substring(pre.length() + 1, impstr.length());
            scope = findPkgOrMod(pre, scope);
            impstr = post;
            //System.out.println("scope=" + scope + "; impstr=" + impstr);
            if(scope == -1) {
                return -1;
            }
        }
        return findObject(impstr, scope);
    }

    /**
     * in parent scope, find a str's id
     * @param str
     * @param parentId
     * @return
     */
    private int findObject(String str, int parentId) {
        if(str.equals(Configure.STAR)) {
            return parentId;
        }
        if(parentId == -1) { //import q (a is module or package)
            return findPkgOrMod(str, parentId);
        }

        for (int childId : singleCollect.getEntities().get(parentId).getChildrenIds()) {
            String name = singleCollect.getEntities().get(childId).getName();
            if(singleCollect.getEntities().get(childId) instanceof ModuleEntity) {
                name = ((ModuleEntity) singleCollect.getEntities().get(childId)).getModuleSimpleName();
                //System.out.println(name);
            }
            if(name.equals(str)) {
                return childId;
            }
        }
        return -1;
    }

    /**
     * in scope , find id of package ir module
     * @param str
     * @param scopeId
     * @return
     */
    private int findPkgOrMod(String str, int scopeId) {
        if(scopeId == -1) {
            for (AbsEntity entity : singleCollect.getEntities()) {
                if(isStrAModOrPkg(entity, str)) {
                    return entity.getId();
                }
            }
        }
        else { //scope != -1, it's parent id
            for(int childId : singleCollect.getEntities().get(scopeId).getChildrenIds()) {
                AbsEntity entity = singleCollect.getEntities().get(childId);
                if(isStrAModOrPkg(entity, str)) {
                    return entity.getId();
                }
            }
        }
        return -1;
    }


    /**
     * judge the str is module or package uerr's name or not.
     * because module's name is a full path , so the code is here
     */
    private boolean isStrAModOrPkg(AbsEntity entity, String str) {
        String name = "";
        if(entity instanceof AbsFLDEntity) {
            name = entity.getName();
        }
        else if (entity instanceof ModuleEntity) {
            name = ((ModuleEntity) entity).getModuleSimpleName();
        }
        if(name.equals(str)) {
            return true;
        }
        return false;
    }




    /**
     * save (importedID, importsList_index) into uerr
     * @param entityId
     * @param importedId
     * @param index
     */
    private void saveId2Id(int entityId, int importedId, int index) {
        if(singleCollect.getEntities().get(entityId) instanceof ModuleEntity) {
            ((ModuleEntity) singleCollect.getEntities().get(entityId)).updateImportedId2Indexs(importedId, index);
        }
        else if (singleCollect.getEntities().get(entityId) instanceof PyFunctionEntity) {
            ((PyFunctionEntity) singleCollect.getEntities().get(entityId)).updateImportedId2Indexs(importedId, index);
        }
    }








}
