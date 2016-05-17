/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.boha.rivers.dto;

import com.boha.rivers.data.River;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author CodeTribe1
 */
public class RiverDTO implements Serializable, Comparable<RiverDTO> {

    private static final long serialVersionUID = 1L;
    private Integer riverID;
    private String riverName;
    private Double distance;
    private List<RiverPartDTO> riverpartList = new ArrayList<>();
    private List<EvaluationSiteDTO> evaluationsiteList = new ArrayList<>();
    private List<StreamDTO> streamList = new ArrayList<>();

    public RiverDTO() {
    }

    public RiverDTO(River c) {
        riverID = c.getRiverID();
        riverName = c.getRiverName().trim();
    }

    public List<StreamDTO> getStreamList() {
        if (streamList == null) {
            streamList = new ArrayList<>();
        }
        return streamList;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public void setStreamList(List<StreamDTO> streamList) {
        this.streamList = streamList;
    }

    public Integer getRiverID() {
        return riverID;
    }

    public void setRiverID(Integer riverID) {
        this.riverID = riverID;
    }

    public String getRiverName() {
        return riverName;
    }

    public void setRiverName(String riverName) {
        this.riverName = riverName;
    }

    public List<RiverPartDTO> getRiverpartList() {
        if (riverpartList == null) {
            riverpartList = new ArrayList<>();
        }
        return riverpartList;
    }

    public void setRiverpartList(List<RiverPartDTO> riverpartList) {
        this.riverpartList = riverpartList;
    }

    public List<EvaluationSiteDTO> getEvaluationsiteList() {
        if (evaluationsiteList == null) {
            evaluationsiteList = new ArrayList<>();
        }
        return evaluationsiteList;
    }

    public void setEvaluationsiteList(List<EvaluationSiteDTO> evaluationsiteList) {
        this.evaluationsiteList = evaluationsiteList;
    }

    @Override
    public int compareTo(RiverDTO o) {
        if (this.distance == null || o.distance == null) {
            return 1;
        }
        if (this.distance < o.distance) {
            return -1;
        }
        if (this.distance > o.distance) {
            return 1;
        }
        
        return 0;
    }

}
