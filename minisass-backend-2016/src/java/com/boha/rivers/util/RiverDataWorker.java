/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.boha.rivers.util;

/**
 *
 * @author aubreyM
 */
import com.boha.rivers.data.Evaluation;
import com.boha.rivers.data.Evaluationinsect;
import com.boha.rivers.data.Evaluationsite;
import com.boha.rivers.data.River;
import com.boha.rivers.data.Riverpart;
import com.boha.rivers.data.Riverpoint;
import com.boha.rivers.data.Stream;
import com.boha.rivers.dto.EvaluationDTO;
import com.boha.rivers.dto.EvaluationInsectDTO;
import com.boha.rivers.dto.EvaluationSiteDTO;
import com.boha.rivers.dto.RiverDTO;
import com.boha.rivers.dto.RiverPartDTO;
import com.boha.rivers.dto.RiverPointDTO;
import com.boha.rivers.dto.StreamDTO;
import com.boha.rivers.transfer.ResponseDTO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
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

@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
public class RiverDataWorker {

    @PersistenceContext
    EntityManager em;

    private PreparedStatement preparedStatement;
    private static final String SQL_STATEMENT = "select a.riverID, a.riverPointID, a.riverPartID, a.latitude, a.longitude,  "
            + "( ? * acos( cos( radians(?) ) * cos( radians( a.latitude) ) "
            + "* cos( radians( a.longitude ) - radians(?) ) + sin( radians(?) ) "
            + "* sin( radians( a.latitude ) ) ) ) "
            + "AS distance FROM riverpoint a HAVING distance < ? order by distance";

    private static final String SQL_STATEMENT2 = "select a.evaluationSiteID, a.latitude, a.longitude,  "
            + "( ? * acos( cos( radians(?) ) * cos( radians( a.latitude) ) "
            + "* cos( radians( a.longitude ) - radians(?) ) + sin( radians(?) ) "
            + "* sin( radians( a.latitude ) ) ) ) "
            + "AS distance FROM evaluationSite a  HAVING distance < ? order by distance";

    public static final int KILOMETRES = 1, MILES = 2, PARM_KM = 6371, PARM_MILES = 3959;

    private Connection conn;

    public List<RiverDTO> getRiversWithinRadius(Double latitude, Double longitude,
            int radius, int type, int flag)
            throws Exception {
        log.log(Level.INFO, "#### getRiversWithinRadius, lat: {0} lng: {1} rad: {2}",
                new Object[]{latitude, longitude, radius});
        long start = System.currentTimeMillis();
        if (conn == null || conn.isClosed()) {
            conn = em.unwrap(Connection.class);
            log.log(Level.INFO, "..........SQL Connection unwrapped from EntityManager");
        }
        if (preparedStatement == null || preparedStatement.isClosed()) {
            preparedStatement = conn.prepareStatement(SQL_STATEMENT);
            log.log(Level.INFO, "..........SQL Statement prepared from Connection: {0}", preparedStatement.toString());
        }
        switch (type) {
            case KILOMETRES:
                preparedStatement.setInt(1, PARM_KM);
                break;
            case MILES:
                preparedStatement.setInt(1, PARM_MILES);
                break;
            case 0:
                preparedStatement.setInt(1, PARM_KM);
                break;
        }
        preparedStatement.setDouble(2, latitude);
        preparedStatement.setDouble(3, longitude);
        preparedStatement.setDouble(4, latitude);
        preparedStatement.setInt(5, radius);
        ResultSet resultSet = preparedStatement.executeQuery();
        long end = System.currentTimeMillis();

        log.log(Level.INFO, "RiverDataWorkerBee -  rivers by radius elapsed: {0} fetchSize: {1}",
                new Object[]{Elapsed.getElapsed(start, end), resultSet.getFetchSize()});
        List<RiverDTO> rivers;
        if (flag == 1) {
            rivers = buildRiverListWithSites(resultSet);
        } else {
            rivers = buildRiverList(resultSet);
        }
        log.log(Level.INFO, "############# Rivers found: {0}", rivers.size());
        return rivers;
    }
    public static final int MAX_RIVERS_TO_SHOW = 10;

    private List<RiverDTO> buildRiverListWithSites(ResultSet resultSet) throws SQLException {
        log.log(Level.SEVERE, "----- buildRiverListWithSites started");
        long start = System.currentTimeMillis();
        List<RiverDTO> rivers = new ArrayList<>();
        List<Integer> riverPartIDList = new ArrayList<>();
        HashMap<Integer, Integer> map = new HashMap<>();

        while (resultSet.next()) {
            Integer id = resultSet.getInt("riverPartID");
            Integer distance = resultSet.getInt("distance");
            if (!map.containsKey(id)) {
                map.put(id, distance);
            }
        }
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            Integer key = entry.getKey();
            riverPartIDList.add(key);
        }
        if (riverPartIDList.isEmpty()) {
            return rivers;
        }
        Query qw = em.createNamedQuery("Riverpart.findRiversByRiverPartList", River.class);
        qw.setParameter("list", riverPartIDList);
        List<River> riverList = qw.getResultList();
        System.out.print("### ########### rivers found: " + riverList.size());
        for (River river : riverList) {
            RiverDTO riverDTO = new RiverDTO(river);
            riverDTO.setRiverpartList(new ArrayList<>());
            List<Riverpart> rpList = river.getRiverpartList();
            System.out.print("\t### " + river.getRiverName() + " riverParts found: " + rpList.size());
            riverDTO.setEvaluationsiteList(buildSites(river.getRiverID()));
            rivers.add(riverDTO);
        }

        long end = System.currentTimeMillis();
        System.out.print("\n\n################# buildRiverList elapsed: " + Elapsed.getElapsed(start, end));

        resultSet.close();
        log.log(Level.INFO, "Rivers found: {0}", rivers.size());
        return rivers;
    }

    private List<EvaluationDTO> buildEvaluation(int evaluationSiteID) {
        List<EvaluationDTO> list = new ArrayList<>();
        Query qw = em.createNamedQuery("Evaluation.findByevaluationSiteID", Evaluation.class);
        qw.setParameter("evaluationSiteID", evaluationSiteID);
        List<Evaluation> l = qw.getResultList();
        for (Evaluation e : l) {
            EvaluationDTO dTO = new EvaluationDTO(e);
            for (Evaluationinsect ea : e.getEvaluationinsectList()) {
                EvaluationInsectDTO eid = new EvaluationInsectDTO(ea);
                dTO.getEvaluationinsectList().add(eid);
            }
            list.add(dTO);
        }

        return list;
    }

    private List<EvaluationSiteDTO> buildSites(int riverID) {
        List<EvaluationSiteDTO> list = new ArrayList<>();
        Query qw = em.createNamedQuery("Evaluationsite.findByRiverID", Evaluationsite.class);
        qw.setParameter("riverID", riverID);
        List<Evaluationsite> l = qw.getResultList();
        for (Evaluationsite e : l) {
            EvaluationSiteDTO siteDTO = new EvaluationSiteDTO(e);
            siteDTO.setEvaluationList(buildEvaluation(e.getEvaluationSiteID()));
            list.add(siteDTO);
        }

        return list;
    }

    private List<RiverDTO> buildRiverList(ResultSet resultSet) throws SQLException {
        log.log(Level.SEVERE, "----- buildRiverList started");
        long start = System.currentTimeMillis();
        List<RiverDTO> rivers = new ArrayList<>();
        List<Integer> riverPartIDList = new ArrayList<>();
        HashMap<Integer, Double> map = new HashMap<>();
        while (resultSet.next()) {
            Integer id = resultSet.getInt("riverPartID");
            Double distance = resultSet.getDouble("distance");
            Integer riverID = resultSet.getInt("riverID");
            if (!map.containsKey(riverID)) {
                map.put(riverID, distance);
            }
            riverPartIDList.add(id);

        }
        if (riverPartIDList.isEmpty()) {
            return rivers;
        }
        Query qw = em.createNamedQuery("Riverpart.findRiversByRiverPartList", River.class);
        qw.setParameter("list", riverPartIDList);
        List<River> riverList = qw.getResultList();
        System.out.print("### rivers found: " + riverList.size());
        for (River river : riverList) {
            RiverDTO riverDTO = new RiverDTO(river);
            riverDTO.setDistance(map.get(river.getRiverID()));
            for (Stream steam : river.getStreamList()) {
                riverDTO.getStreamList().add(new StreamDTO(steam));
            }
            riverDTO.setEvaluationsiteList(buildSites(river.getRiverID()));
            log.log(Level.OFF, " --- {0} sites: {1}", new Object[]{riverDTO.getRiverName(),
                riverDTO.getEvaluationsiteList().size()});
            rivers.add(riverDTO);
        }

        Collections.sort(rivers);

        long end = System.currentTimeMillis();
        System.out.print("\n\n################# buildRiverList elapsed: " + Elapsed.getElapsed(start, end));
        resultSet.close();
        log.log(Level.INFO, "Rivers found: {0}", rivers.size());
        return rivers;
    }

    public EntityManager getEntityManager() {
        return em;
    }

    public ResponseDTO getRiverDetails(Integer riverID) {
        ResponseDTO resp = new ResponseDTO();
        River river = em.find(River.class, riverID);
        RiverDTO dto = new RiverDTO(river);
        int count = 0;
        for (Riverpart rp : river.getRiverpartList()) {
            RiverPartDTO rpDTO = new RiverPartDTO(rp);
            for (Riverpoint riverpoint : rp.getRiverpointList()) {
                rpDTO.getRiverpointList().add(new RiverPointDTO(riverpoint));
                count += rpDTO.getRiverpointList().size();
            }
            dto.getRiverpartList().add(rpDTO);
        }
        for (Stream s : river.getStreamList()) {
            dto.getStreamList().add(new StreamDTO(s));
        }
        dto.setEvaluationsiteList(buildSites(riverID));

        resp.getRiverList().add(dto);
        log.log(Level.INFO, "River points retrieved:{0} riverID: {1}", new Object[]{count, riverID});
        return resp;
    }

    public void fixRiverPoints() {
        long start = System.currentTimeMillis();
        Query q = em.createNamedQuery("River.findAll", River.class);
        List<River> rivers = q.getResultList();
        Query up = em.createQuery("update Riverpoint r set r.river = :river where r.riverPart = :riverpart");
        int count = 0;
        for (River river : rivers) {
            for (Riverpart riverpart : river.getRiverpartList()) {
                up.setParameter("river", river);
                up.setParameter("riverpart", riverpart);
                count += up.executeUpdate();
            }
            river.setRiverName(river.getRiverName().trim());
            em.merge(river);
            log.log(Level.INFO, "River: {0} ===> river name and river points updated  ", river.getRiverName());
        }
        long end = System.currentTimeMillis();
        log.log(Level.INFO, "River points have riverID updated, elapsed seconds: {0} records updated: {1}",
                new Object[]{(end - start) / 1000, count});
    }

    public void fixCrocodileRiver() throws DataException {
        log.log(Level.INFO, ".... starting fixCrocodileRiver ...");
        try {
            River river = em.find(River.class, 2538);
            List<Riverpoint> rpEast = new ArrayList<>();
            List<Riverpoint> rpWest = new ArrayList<>();

            for (Riverpart riverpart : river.getRiverpartList()) {
                for (Riverpoint rp : riverpart.getRiverpointList()) {
                    if (rp.getLongitude() > 29) {
                        rpEast.add(rp);
                    } else {
                        rpWest.add(rp);
                    }
                }
            }

            writeNewCrocodile(rpWest, "West");
            writeNewCrocodile(rpEast, "East");

            //remove old Croc River
            em.remove(river);
            em.flush();
            log.log(Level.SEVERE, "Old crocodile 2538 has been shot and killed!");
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed fix", e);
            throw new DataException("Failed");
        }

    }

    private void writeNewCrocodile(List<Riverpoint> list, String heading) {
        River x1 = new River();
        x1.setRiverName("Crocodile " + heading);
        em.persist(x1);
        em.flush();
        Riverpart z = new Riverpart();
        z.setRiver(x1);
        z.setDistanceToMouth(0);
        z.setRiverName(x1.getRiverName());
        em.persist(z);
        em.flush();
        int count = 0;
        for (Riverpoint riverpoint : list) {
            Riverpoint a = new Riverpoint();
            a.setRiverPart(z);
            a.setLatitude(riverpoint.getLatitude());
            a.setLongitude(riverpoint.getLongitude());
            a.setRiver(x1);
            em.persist(a);
            count++;
        }
        em.flush();
        list.clear();
        log.log(Level.SEVERE, "Crocodile {0} has been added, points: {1}", new Object[]{heading, count});
    }

    public static final int ROWS_PER_PAGE = 100;
    static final Logger log = Logger.getLogger(RiverDataWorker.class.getName());
}
