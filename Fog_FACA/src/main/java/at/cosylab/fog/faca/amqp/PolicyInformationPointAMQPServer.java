package at.cosylab.fog.faca.amqp;

import at.cosylab.fog.faca.commons.FACAUtilFunctions;
import at.cosylab.fog.faca.commons.repositories.subject.Subject;
import at.cosylab.fog.faca.services.policyInformationPoint.PolicyInformationPointService;
import fog.amqp_utils.payloads.AMQPResponseEntity;
import fog.amqp_utils.routes.FACARoutesConstants;
import fog.faca.utils.Profession;
import fog.payloads.faca.PIP.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.Payload;
import payloads.acam.deviceTypes.ListLatestDeviceTypeResponse;

import java.util.List;
import java.util.Map;

public class PolicyInformationPointAMQPServer {

    private static final Logger logger = LoggerFactory.getLogger(PolicyInformationPointAMQPServer.class);

    @Autowired
    private PolicyInformationPointService pipService;

    @RabbitListener(queues = "abac.rpc.pip.register")
    public AMQPResponseEntity<?> register(@Payload AddUserRequest addUserReq) {
        try {
            logger.info("[AMQP_PIP][REGISTER_USER] :" + addUserReq);
            Subject sub = pipService.registerSubject(addUserReq);
            return new AMQPResponseEntity<Subject>(sub, HttpStatus.OK.value());
        } catch (Exception e) {
            return FACAUtilFunctions.convertExceptionToAMQP(e);
        }
    }

    @RabbitListener(queues = "abac.rpc.pip.change_password")
    public AMQPResponseEntity<?> changePassword(@Payload ChangePasswordRequest request) {
        try {
            logger.info("[AMQP_PIP][CHANGE_PASS] :" + request);
            pipService.changePassword(request);
            return new AMQPResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return FACAUtilFunctions.convertExceptionToAMQP(e);
        }
    }

    @RabbitListener(queues = "abac.rpc.pip.add_profession")
    public AMQPResponseEntity<?> addProfession(@Payload Profession request, Message message) {
        try {
            String tokenString = FACAUtilFunctions.retrieveSessionTokenFromAMQPMessage(message);
            logger.info("[AMQP_PIP][ADD_PROFESSION] :" + request + " Token : " + tokenString);
            return new AMQPResponseEntity<Subject>(pipService.addProfession(request, tokenString), HttpStatus.OK.value());
        } catch (Exception e) {
            return FACAUtilFunctions.convertExceptionToAMQP(e);
        }
    }

    @RabbitListener(queues = "abac.rpc.pip.remove_profession")
    public AMQPResponseEntity<?> removeProfession(@Payload Profession request, Message message) {
        try {
            String tokenString = FACAUtilFunctions.retrieveSessionTokenFromAMQPMessage(message);
            logger.info("[AMQP_PIP][REMOVE_PROFESSION] :" + request + " Token : " + tokenString);
            return new AMQPResponseEntity<Subject>(pipService.removeProfession(request, tokenString), HttpStatus.OK.value());
        } catch (Exception e) {
            return FACAUtilFunctions.convertExceptionToAMQP(e);
        }
    }

    @RabbitListener(queues = "abac.rpc.pip.add_handicap")
    public AMQPResponseEntity<?> addHandicap(@Payload Handicap request, Message message) {
        try {
            String tokenString = FACAUtilFunctions.retrieveSessionTokenFromAMQPMessage(message);
            logger.info("[AMQP_PIP][ADD_HANDICAP] :" + request + " Token : " + tokenString);
            return new AMQPResponseEntity<Subject>(pipService.addHandicap(request, tokenString), HttpStatus.OK.value());
        } catch (Exception e) {
            return FACAUtilFunctions.convertExceptionToAMQP(e);
        }
    }

    @RabbitListener(queues = "abac.rpc.pip.remove_handicap")
    public AMQPResponseEntity<?> removeHandicap(@Payload Handicap request, Message message) {
        try {
            String tokenString = FACAUtilFunctions.retrieveSessionTokenFromAMQPMessage(message);
            logger.info("[AMQP_PIP][REMOVE_HANDICAP] :" + request + " Token : " + tokenString);
            return new AMQPResponseEntity<Subject>(pipService.removeHandicap(request, tokenString), HttpStatus.OK.value());

        } catch (Exception e) {
            return FACAUtilFunctions.convertExceptionToAMQP(e);
        }
    }

    //Session management
    @RabbitListener(queues = "abac.rpc.pip.login_extended")
    public AMQPResponseEntity<?> loginExtended(@Payload LoginRequest request) {
        try {
            logger.info("[AMQP_PIP][LOGIN] :" + request);
            SubjectTokenPair subjecttoken = pipService.checkLogin(request.getUsername(), request.getPassword());
            SubjectTokenAndViews subjectTokenAndViews = new SubjectTokenAndViews(new SubjectSecured(subjecttoken.getSubject()), FACAUtilFunctions.generateViewList(subjecttoken.getSubject().getRole().getRoleName()), subjecttoken.getToken());
            logger.warn("TOKEN: " + subjecttoken.getToken());
            return new AMQPResponseEntity<SubjectTokenAndViews>(subjectTokenAndViews, HttpStatus.OK.value());
        } catch (Exception e) {
            return FACAUtilFunctions.convertExceptionToAMQP(e);
        }
    }

    @RabbitListener(queues = "abac.rpc.pip.logout")
    public AMQPResponseEntity<?> logout(Message message) {
        try {
            String tokenString = FACAUtilFunctions.retrieveSessionTokenFromAMQPMessage(message);
            logger.info("[AMQP_PIP][LOGOUT] Token : " + tokenString);
            pipService.logout(tokenString);
            return new AMQPResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return FACAUtilFunctions.convertExceptionToAMQP(e);
        }
    }

    // Subject management
    @RabbitListener(queues = "abac.rpc.pip.update_user_active_flag")
    public AMQPResponseEntity<?> updateUserActiveFlag(@Payload SubjectUpdateFlagRequest activeFlagReq, Message message) {
        try {
            String tokenString = FACAUtilFunctions.retrieveSessionTokenFromAMQPMessage(message);
            logger.info("[AMQP_PIP][UPDATE_USER_ACTIVE_FLAG] " + activeFlagReq + " Token : " + tokenString);
            SubjectDTO subject = pipService.updateSubjectActiveFlag(activeFlagReq.getId(), activeFlagReq.isActive());
            return new AMQPResponseEntity<SubjectDTO>(subject, HttpStatus.OK.value());
        } catch (Exception e) {
            return FACAUtilFunctions.convertExceptionToAMQP(e);
        }
    }

    @RabbitListener(queues = "abac.rpc.pip.get_user_data")
    public AMQPResponseEntity<?> getUserData(Message message) {
        try {
            String tokenString = FACAUtilFunctions.retrieveSessionTokenFromAMQPMessage(message);
            logger.info("[AMQP_PIP][GET_USER_DATA] Token : " + tokenString);
            return new AMQPResponseEntity<Subject>(pipService.getUserData(tokenString), HttpStatus.OK.value());
        } catch (Exception e) {
            return FACAUtilFunctions.convertExceptionToAMQP(e);
        }
    }

    @RabbitListener(queues = "abac.rpc.pip.update_account_role")
    public AMQPResponseEntity<?> updateAccountRole(@Payload UpdateRoleRequest request, Message message) {
        try {
            String tokenString = FACAUtilFunctions.retrieveSessionTokenFromAMQPMessage(message);

            logger.info("[AMQP_PIP][UPDATE_ROLE] :" + request.toString());
            return new AMQPResponseEntity<SubjectSecured>(pipService.updateRole(request, tokenString), HttpStatus.OK.value());
        } catch (Exception e) {
            return FACAUtilFunctions.convertExceptionToAMQP(e);
        }
    }

    @RabbitListener(queues = "abac.rpc.pip.get_subject_list")
    public AMQPResponseEntity<?> getSubjectList(Message message) {
        try {
            String tokenString = FACAUtilFunctions.retrieveSessionTokenFromAMQPMessage(message);

            logger.info("[AMQP_PIP][GET_SUBJECT_LIST] :" + tokenString);
            return new AMQPResponseEntity<List<SubjectSecured>>(pipService.getSubjectList(tokenString), HttpStatus.OK.value());
        } catch (Exception e) {
            return FACAUtilFunctions.convertExceptionToAMQP(e);
        }
    }

    @RabbitListener(queues = "abac.rpc.pip.get_subject_management_configuration")
    public AMQPResponseEntity<?> getSubjectManagementConfiguration(Message message) {
        try {
            String tokenString = FACAUtilFunctions.retrieveSessionTokenFromAMQPMessage(message);

            logger.info("[AMQP_PIP][GET_SUBJECT_MNGMT_CONFIG] :" + tokenString);
            return new AMQPResponseEntity<SubjectManagementConfiguration>(pipService.getSubjectManagementConfiguration(tokenString), HttpStatus.OK.value());
        } catch (Exception e) {
            return FACAUtilFunctions.convertExceptionToAMQP(e);
        }
    }

    //Device types
    @RabbitListener(queues = FACARoutesConstants.PIP_LIST_DEVICE_TYPES)
    public AMQPResponseEntity<?> listAllDeviceTypes() {
        try {
            logger.info("[AMQP_PIP][LIST_ALL_DEVICE_TYPES]");
            return new AMQPResponseEntity<>(
                    new ListLatestDeviceTypeResponse(pipService.listAllDeviceTypes()), HttpStatus.OK.value());
        } catch (Exception e) {
            return FACAUtilFunctions.convertExceptionToAMQP(e);
        }
    }

}
