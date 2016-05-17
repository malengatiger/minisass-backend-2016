/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.boha.rivers.util;

import com.boha.rivers.data.Evaluation;
import com.boha.rivers.data.Evaluationsite;
import com.boha.rivers.data.River;
import com.boha.rivers.transfer.ResponseDTO;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author aubreymalabie
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
public class SiteDuplicateKiller {
    
    @PersistenceContext
    EntityManager em;
    Gson gson = new Gson();
    
    public ResponseDTO killDuplicates() {
        ResponseDTO w = new ResponseDTO();
        HashMap<Integer, Evaluationsite> deleteMap = new HashMap<>();
        HashMap<Integer, Evaluationsite> keepMap = new HashMap<>();
        try {
            Query q = em.createNamedQuery("River.findAll", River.class);
            List<River> list = q.getResultList();
            for (River river : list) {
                if (river.getEvaluationsiteList().isEmpty()) {
                    continue;
                }
                q = em.createNamedQuery("Evaluationsite.findDuplicates", Evaluationsite.class);
                for (Evaluationsite site : river.getEvaluationsiteList()) {
                    q.setParameter("riverID", site.getRiver().getRiverID());
                    q.setParameter("latitude", site.getLatitude());
                    q.setParameter("longitude", site.getLongitude());
                    List<Evaluationsite> sites = q.getResultList();
                    if (sites.size() > 1) {
                        log.log(Level.OFF, "**************************************** DUPLICATES FOUND: {0}", sites.size());
                        int index = 0;
                        for (Evaluationsite site1 : sites) {
                            if (index == 0) {
                                log.log(Level.INFO, "####### site to keep has obs: {0}", site1.getEvaluationList().size());
                                
                                keepMap.put(site1.getEvaluationSiteID(), site1);
                                StringBuilder sb = new StringBuilder();
                                sb.append("+++++++ EVALUATIONS ++++++++++++\n");
                                for (Evaluation eval : site1.getEvaluationList()) {
                                    //String json = gson.toJson(eval);
                                    sb.append(eval.getEvaluationDate().toString()).append("\n");
                                }
                                sb.append("+++++++ end of EVALUATIONS ++++++++++++\n\n");
                                //log.log(Level.INFO, sb.toString());
                                index++;
                                continue;
                            }
                            
                            if (site1.getEvaluationList().isEmpty()) {
                                log.log(Level.OFF, "site slated for deletion");
                                deleteMap.put(site1.getEvaluationSiteID(), site1);
                            } else {
                                log.log(Level.OFF, "!!!!!!! site slated for deletion has: {0} evaluations", site1.getEvaluationList().size());
                                StringBuilder sb = new StringBuilder();
                                sb.append("------- EVALUATIONS ----------------\n");
                                for (Evaluation eval : site1.getEvaluationList()) {
                                    //String json = gson.toJson(eval);
                                    sb.append(eval.getEvaluationDate().toString()).append("\n");
                                }
                                sb.append("----------- end of EVALUATIONS ------------\n\n");
                                log.log(Level.OFF, sb.toString());
                                deleteMap.put(site1.getEvaluationSiteID(), site1);
                            }
                            
                        }
                    } else if (!sites.isEmpty()) {
                        log.log(Level.INFO, "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ NO duplicates: site has obs: {0}", sites.get(0).getEvaluationList().size());
                        keepMap.put(sites.get(0).getEvaluationSiteID(), sites.get(0));
                        StringBuilder sb = new StringBuilder();
                        sb.append("@@@@@@@ EVALUATIONS @@@@@@@@@@@@@@@\n");
                        for (Evaluation eval : sites.get(0).getEvaluationList()) {
                            sb.append(eval.getEvaluationDate().toString()).append("\n");
                        }
                        sb.append("@@@@@@@ end of EVALUATIONS @@@@@@@@@@@@@@@\n");
                        //log.log(Level.INFO, sb.toString());
                    }
                }
            }
            log.log(Level.INFO, "Total sites kept: {0} duplicates: {1}",
                    new Object[]{keepMap.keySet().size(), deleteMap.keySet().size()});
            
            for (Map.Entry<Integer, Evaluationsite> entry : deleteMap.entrySet()) {
                Evaluationsite site = entry.getValue();
                em.remove(site);
            }
            em.flush();
            log.log(Level.OFF, "Duplicate sites deleted: {0}", deleteMap.keySet().size());
//
            int count = 0;
            for (Map.Entry<Integer, Evaluationsite> entry : keepMap.entrySet()) {
                Evaluationsite es = entry.getValue();
                log.log(Level.INFO, "EvaluationSite to confirm: " + " riverID: {0} - {1} \tlat: {2} lng: {3}",
                        new Object[]{es.getRiver().getRiverID(), es.getRiver().getRiverName(),
                            es.getLatitude(), es.getLongitude()});
                es.setConfirmed(Boolean.TRUE);
                em.merge(es);
                count += es.getEvaluationList().size();
            }
            
            em.flush();
            log.log(Level.OFF, "Proper, unduplicated sites kept and confirmed:{0} total observations: {1}", 
                    new Object[]{keepMap.keySet().size(), count});
            w.setMessage("Evaluation sites cleaned up");
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to clean up duplicate sites", e);
            w.setMessage("Failed to clean up duplicate sites\n" + e.getMessage());
            
        }
        return w;
    }
    
    static final Logger log = Logger.getLogger(SiteDuplicateKiller.class.getSimpleName());
}
