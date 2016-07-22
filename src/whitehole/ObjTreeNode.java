/*
    Copyright 2012 The Whitehole team

    This file is part of Whitehole.

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
    FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/

package whitehole;

import java.util.Enumeration;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import whitehole.smg.LevelObject;
import whitehole.smg.object.PathPointObject;

public class ObjTreeNode implements MutableTreeNode
{
    public ObjTreeNode()
    {
        this.parent = null;
        this.object = null;
        this.userObject = null;
        this.uniqueID = -1;
    }
    
    public ObjTreeNode(LevelObject obj)
    {
        this.parent = null;
        this.object = obj;
        this.userObject = null;
        this.uniqueID = obj.uniqueID;
    }
    
    public ObjTreeNode(PathPointObject obj)
    {
        this.parent = null;
        this.object = obj;
        this.userObject = null;
        this.uniqueID = obj.uniqueID;
    }
    

    @Override
    public void insert(MutableTreeNode child, int index) {}
    @Override
    public void remove(int index) {}
    @Override
    public void remove(MutableTreeNode node) {}

    @Override
    public void setUserObject(Object object)
    {
        userObject = object;
    }

    @Override
    public void removeFromParent()
    {
        parent = null;
        System.out.println("[ObjTreeNode] REMOVE FROM PARENT");
    }

    @Override
    public void setParent(MutableTreeNode newParent)
    {
        parent = newParent;
    }

    @Override
    public TreeNode getChildAt(int childIndex)
    {
        return null;
    }

    @Override
    public int getChildCount()
    {
        return 0;
    }

    @Override
    public TreeNode getParent()
    {
        return parent;
    }

    @Override
    public int getIndex(TreeNode node)
    {
        return -1;
    }

    @Override
    public boolean getAllowsChildren()
    {
        return false;
    }

    @Override
    public boolean isLeaf()
    {
        return true;
    }

    @Override
    public Enumeration children()
    {
        return null;
    }
    
    
    @Override
    public String toString()
    {
        if (userObject != null)
            return userObject.toString();
        else if (object != null)
            return object.toString();
        else
            return "unknown node";
    }
    
    
    public TreeNode parent;
    public Object object;
    public Object userObject;
    public int uniqueID;
}
