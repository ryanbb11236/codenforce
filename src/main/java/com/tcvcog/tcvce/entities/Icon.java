/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tcvcog.tcvce.entities;

/**
 *
 * @author sylvia
 */
public class Icon extends EntityUtils {
    private int iconid;
    private String name;
    private String styleClass;
    private String fontAwesome;
    private String materialIcon;

    /**
     * @return the iconid
     */
    public int getIconid() {
        return iconid;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the styleClass
     */
    public String getStyleClass() {
        return styleClass;
    }

    /**
     * @return the fontAwesome
     */
    public String getFontAwesome() {
        return fontAwesome;
    }

    /**
     * @return the materialIcon
     */
    public String getMaterialIcon() {
        return materialIcon;
    }

    /**
     * @param iconid the iconid to set
     */
    public void setIconid(int iconid) {
        this.iconid = iconid;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param styleClass the styleClass to set
     */
    public void setStyleClass(String styleClass) {
        this.styleClass = styleClass;
    }

    /**
     * @param fontAwesome the fontAwesome to set
     */
    public void setFontAwesome(String fontAwesome) {
        this.fontAwesome = fontAwesome;
    }

    /**
     * @param materialIcon the materialIcon to set
     */
    public void setMaterialIcon(String materialIcon) {
        this.materialIcon = materialIcon;
    }
    
}
