package com.jh.carvideo;

import java.util.ArrayList;
import java.util.List;

public class TreeNode {
    private String id;
    private String name;
    private String path;
    private String usbSource; // "USB 1" o "USB 2"
    private boolean isUsbRoot;
    private int totalTrackCount;
    private int directTrackCount;
    private boolean isExpanded;
    private int level;
    private TreeNode parent;
    private List<TreeNode> children = new ArrayList<>();
    // Canciones que están directamente en esta carpeta (las que cuenta el botón [N ▶])
    private final List<Track> directTracks = new ArrayList<>();

    public TreeNode(String id, String name, String path, String usbSource, boolean isUsbRoot, int directTrackCount) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.usbSource = usbSource;
        this.isUsbRoot = isUsbRoot;
        this.directTrackCount = directTrackCount;
        this.totalTrackCount = directTrackCount;
        this.isExpanded = false;
        this.level = 0;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getPath() { return path; }
    public String getUsbSource() { return usbSource; }
    public boolean isUsbRoot() { return isUsbRoot; }
    
    public int getTotalTrackCount() { return totalTrackCount; }
    public void setTotalTrackCount(int totalTrackCount) { this.totalTrackCount = totalTrackCount; }
    
    public int getDirectTrackCount() { return directTrackCount; }
    public void setDirectTrackCount(int directTrackCount) { this.directTrackCount = directTrackCount; }
    
    public void incrementDirectCount() { 
        this.directTrackCount++; 
        this.totalTrackCount++; 
    }
    
    public void incrementTotalCount() {
        this.totalTrackCount++;
    }

    public List<Track> getDirectTracks() { return directTracks; }

    public void addDirectTrack(Track track) {
        directTracks.add(track);
        incrementDirectCount();
    }

    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { this.isExpanded = expanded; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public TreeNode getParent() { return parent; }
    public void setParent(TreeNode parent) { this.parent = parent; }

    public List<TreeNode> getChildren() { return children; }
    
    public void addChild(TreeNode child) {
        child.setParent(this);
        children.add(child);
    }

    public boolean hasChildren() { 
        return children != null && !children.isEmpty(); 
    }

    public TreeNode findChildByName(String childName) {
        if (children == null) return null;
        for (TreeNode child : children) {
            if (child.getName() != null && child.getName().equalsIgnoreCase(childName)) {
                return child;
            }
        }
        return null;
    }
}
