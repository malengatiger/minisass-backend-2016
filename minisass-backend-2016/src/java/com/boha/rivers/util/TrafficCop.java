/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.boha.rivers.util;

import com.boha.rivers.transfer.RequestDTO;
import com.boha.rivers.transfer.ResponseDTO;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author CodeTribe1
 */
@Stateless
public class TrafficCop {

    @EJB
    DataUtil dataUtil;

    @EJB
    ListUtil listUtil;

    @EJB
    RiverDataWorker dataWorker;

    @EJB
    CloudMsgUtil cloudMsgUtil;

    @EJB
    PlatformUtil platformUtil;
    @EJB
    SiteDuplicateKiller duplicateKiller;

    public ResponseDTO processRequest(RequestDTO req,
            DataUtil dataUtil, ListUtil listUtil, CloudMsgUtil cloudMsgUtil, PlatformUtil platformUtil) {
        long start = System.currentTimeMillis();
        ResponseDTO resp = new ResponseDTO();
        try {
            switch (req.getRequestType()) {
                case RequestDTO.REGISTER_TEAM:
                    resp = dataUtil.registerTeam(req.getTeam());
                    resp.setMessage("Team registered OK");
                    break;
                case RequestDTO.REGISTER_TEAM_MEMBER:
                    resp = dataUtil.registerTeamMember(req.getTeamMember(), listUtil);
                    resp.setMessage("Member registered OK");
                    break;
                case RequestDTO.SIGN_IN_MEMBER:
                    resp = dataUtil.loginTeamMember(req.getEmail(), 
                            req.getPassword(), req.getGcmDevice(), listUtil);
                    resp.setMessage("Member signed in OK");
                    break;
                case RequestDTO.ADD_COMMENT:
                    resp = dataUtil.addComment(req.getComment());
                    break;
                case RequestDTO.ADD_EVALUATION_SITE:
                    resp = dataUtil.addEvaluationSite(req.getEvaluationSite());
                    resp.setMessage("Observation site has been added");
                    break;
                case RequestDTO.ADD_EVALUATION:
                    resp = dataUtil.addEvaluation(req.getEvaluation());
                    break;
                case RequestDTO.INVITE_MEMBER:
                    dataUtil.inviteMembers(req.getTmember());
                    resp.setMessage("Member Invited");
                    break;
                case RequestDTO.ADD_INSECT_IMAGE:
                    resp = dataUtil.addInsertImage(req.getInsectImage());
                    break;
                case RequestDTO.ADD_STREAM:
                    resp = dataUtil.addStream(req.getStream());
                    break;
                case RequestDTO.LIST_EVALUATION_SITES:
                    resp = listUtil.getEvaluationList();
                    break;
                case RequestDTO.GET_MEMBER:
                    resp = listUtil.getTeamMemberProfileData(req.getTeamMemberID(), listUtil, dataUtil);
                    break;
                case RequestDTO.ADD_EVALUATION_INSECT:
                    resp = dataUtil.addEvaluationInsect(req.getEvaluationInsect());
                    break;
                case RequestDTO.UPDATE_TEAM:
                    resp = dataUtil.updateTeam(req.getTeam());
                    break;
                case RequestDTO.UPDATE_EVALUATION_SITE:
                    resp = dataUtil.updateEvaluationSite(req.getEvaluationSite());
                    break;
                case RequestDTO.UPDATE_PROFILE:
                    resp = dataUtil.updateTeamMember(req.getTeamMember());
                    break;
                case RequestDTO.UPDATE_COMMENT:
                    resp = dataUtil.updateComment(req.getComment());
                    break;
                case RequestDTO.UPDATE_EVALUATION:
                    resp = dataUtil.updateEvaluation(req.getEvaluation());
                    break;
                case RequestDTO.UPDATE_CONDITIONS:
                    resp = dataUtil.updateConditions(req.getConditions());
                    break;
                case RequestDTO.SEND_INVITE_TO_TEAM_MEMBER:
                    resp = cloudMsgUtil.sendInviteToTeam(req.getTeamMemberID(), req.getTeamID(), platformUtil, dataUtil);
                    break;
                case RequestDTO.UPDATE_STREAM:
                    resp = dataUtil.updateStream(req.getStream());
                    break;
                case RequestDTO.LIST_EVALUATION_BY_TEAM_MEMBER:
                    resp = listUtil.getEvaluationByTeamMember(req.getTeamMemberID());
                    break;
                case RequestDTO.LIST_EVALUATION_BY_CONDITIONS:
                    resp = listUtil.getEvaluationByCondtions(req.getConditionsID());
                    break;
                case RequestDTO.LIST_EVALUATION_SITE_BY_CATEGORY:
                    resp = listUtil.getEvaluationSiteByCategory(req.getCategoryID());
                    break;
                case RequestDTO.LIST_EVALUATION_INSECT_BY_EVALUATION:
                    resp = listUtil.getEvaluationInsectByEvaluation(req.getEvaluationID());
                    break;
                case RequestDTO.LIST_TEAMS_BY_TOWN:
                    resp = listUtil.getTeamByTown(req.getTownID());
                    break;
                case RequestDTO.LIST_TEAM_MEMBERS:
                    resp = listUtil.getTeamMemberList();
                    break;
                case RequestDTO.LIST_REGISTER_DATA:
                    resp = listUtil.registrationData();
                    break;
                case RequestDTO.LIST_CATEGORY:
                    resp = listUtil.getCategoryList();
                    break;
                case RequestDTO.LIST_COMMENTS:
                    resp = listUtil.getCommentList();
                    break;

                case RequestDTO.LIST_EVALUATIONS:
                    resp = listUtil.getEvaluationList();
                    break;
                case RequestDTO.LIST_RIVERS:
                    resp = listUtil.getRiverList();
                    break;
                case RequestDTO.GET_DATA:
                    resp = listUtil.getData();
                    break;
                case RequestDTO.LIST_DATA_WITH_RADIUS_RIVERS:
                    ResponseDTO respRivers = listUtil.getRiverData(req.getLatitude(), req.getLongitude(), 
                            req.getRadius(), req.getType(), 2);
                    resp = listUtil.getLookups();
                    resp.setRiverList(respRivers.getRiverList());
                    resp.setMessage("Rivers found within radius of " 
                            + req.getRadius() + " km: " + resp.getRiverList().size());
                    break;

                case RequestDTO.GET_RIVERS_BY_RADIUS:
                    resp = listUtil.getLookups();
                    resp.setRiverList(dataWorker.getRiversWithinRadius(req.getLatitude(),
                            req.getLongitude(), req.getRadius(), req.getType(), 2));
                    resp.setMessage("Rivers found within radius of " 
                            + req.getRadius() + " km: " + resp.getRiverList().size());
                    break;
                case RequestDTO.LIST_EVALUATION_BY_RIVER_ID:
                    resp = listUtil.getEvaluationSiteByRiver(req.getRiverID());
                    break;

                case RequestDTO.LIST_STREAM:
                    resp = listUtil.getStream();
                    break;

                case RequestDTO.LIST_BY_STREAM_NAME:
                    resp = listUtil.getStreamByStreamName(req.getStream().getStreamName());
                    break;

                case RequestDTO.ADD_TEAM:
                    dataUtil.addTeam(req.getTeam(), req.getTeamMemberID());
                    resp.setMessage("Member registered team");
                    break;
                case RequestDTO.SEARCH_MEMBERS:
                    resp = listUtil.searchForMembers(req.getSearch(), req.getEmail());
                    break;

                
                case RequestDTO.DELETE_EVALUATION_SITE:
                    resp = dataUtil.deleteEvaluationSite(req.getEvaluationSiteID());
                    resp.setMessage("Observation site removed, id: " + req.getEvaluationSiteID());
                    break;
                case RequestDTO.GET_RIVER_DETAILS:
                    resp = dataWorker.getRiverDetails(req.getRiverID());
                    resp.setMessage("River points returned for riverID: " + req.getRiverID());
                    break;
                    
                case RequestDTO.KILL_SITE_DUPLICATES:
                    resp = duplicateKiller.killDuplicates();
                    break;

                case RequestDTO.FIX_RIVER_POINTS:
                    dataWorker.fixRiverPoints();
                    break;
                case RequestDTO.FIX_CROCODILE:
                    dataWorker.fixCrocodileRiver();
                    break;
                default:
                    resp.setStatusCode(444);
                    resp.setMessage("#### Unknown Request");
                    logger.log(Level.SEVERE, "Couldn't find request,please try again");
                    break;

            }
        } catch (DataException e) {
            resp.setStatusCode(101);
            resp.setMessage("Data service failed to process your request");
            logger.log(Level.SEVERE, "Database related failure", e);

        } catch (Exception e) {
            resp.setStatusCode(102);
            resp.setMessage("Server process failed to process your request");
            logger.log(Level.SEVERE, "Generic server related failure", e);

        }
        if (resp.getStatusCode() == null) {
            resp.setStatusCode(0);
        }
        long end = System.currentTimeMillis();
        double elapsed = Elapsed.getElapsed(start, end);
        resp.setElapsedRequestTimeInSeconds(elapsed);
        logger.log(Level.WARNING, "*********** request elapsed time: {0} seconds", elapsed);
        return resp;
    }
    @PersistenceContext
    EntityManager em;
    static final Logger logger = Logger.getLogger(TrafficCop.class.getSimpleName());
}
