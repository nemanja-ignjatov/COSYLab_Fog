package at.cosylab.fog.faca.services.policyInformationPoint;


import at.cosylab.fog.faca.commons.FACAUtilFunctions;
import at.cosylab.fog.faca.commons.exceptions.*;
import at.cosylab.fog.faca.commons.repositories.deviceType.DeviceType;
import at.cosylab.fog.faca.commons.repositories.deviceType.DeviceTypeRepository;
import at.cosylab.fog.faca.commons.repositories.subject.Subject;
import at.cosylab.fog.faca.commons.repositories.subject.SubjectRepository;
import at.cosylab.fog.faca.services.policyDecisionPoint.PolicyDecisionPointService;
import fog.faca.utils.FACAProjectConstants;
import fog.faca.utils.Profession;
import fog.faca.utils.Role;
import fog.payloads.faca.PDP.AuthorizationRequestPIP;
import fog.payloads.faca.PIP.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import payloads.acam.deviceTypes.DeviceTypeDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class PolicyInformationPointService {

    private static final Logger logger = LoggerFactory.getLogger(PolicyInformationPointService.class);

    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private DeviceTypeRepository deviceTypeRepository;
    @Autowired
    private PolicyDecisionPointService pdpService;

    @Autowired
    private SubjectJWSHelper jwsHandler;

    //Auth support

    /**
     * A function for checking login data
     *
     * @param userName the userName of the user
     * @param password the password of the user, does not get hashed anymore.
     * @return true, if the login data is valid and false if not.
     */
    public SubjectTokenPair checkLogin(String userName, String password) throws SubjectNotFoundException, WrongPasswordException, SubjectNotActiveException {

        logger.info("[PIP SRV][CHECK LOGIN] Username: " + userName + " Password: " + password);

        Subject subject = subjectRepository.findByUserName(userName);

        if (subject == null) {
            throw new SubjectNotFoundException();
        }

        if (!subject.isActive()) {
            throw new SubjectNotActiveException();
        }

        if (!subject.getPassword().equals(password)) {
            updatePasswordFails(subject, true);
            throw new WrongPasswordException();
        } else {
            updatePasswordFails(subject, false);
        }

        String token = generateSessionToken(subject);

        logger.info("[PIP SRV][CHECK LOGIN] Username: " + userName + ", successful login!");

        return new SubjectTokenPair(subject.toDTO(), token);
    }

    public void logout(String tokenString) {
        logger.info("[PIP SRV][LOGOUT] Token: " + tokenString);

        logger.info("[PIP SRV][LOGOUT] Token: " + tokenString + " deleted!");
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // User management
    public Subject getUserData(String tokenString) throws UnauthorizedAccessException, SubjectNotFoundException {
        logger.info("[PIP SRV][GET_USER_DATA] Token: " + tokenString);
        String username = jwsHandler.getUsernameFromJWS(tokenString);
        Subject retSubject = subjectRepository.findByUserName(username);
        if (retSubject == null) {
            logger.warn("[PIP SRV][GET_SUBJECT] Username: " + username + " not found!");
            throw new SubjectNotFoundException();
        }
        logger.info("[PIP SRV][GET_USER_DATA] Token: " + tokenString + " returning " + retSubject);
        return retSubject;
    }

    /**
     * A function used to get a subject. Cannot be reached with HTTP and hence does not call the PDP.
     *
     * @param userName the userName of the subject
     * @return the requested smart home or null
     */
    public Subject getSubject(String userName) throws SubjectNotFoundException {
        logger.info("[PIP SRV][GET_SUBJECT] Username: " + userName);
        //return Subject
        Subject retSubject = subjectRepository.findByUserName(userName);
        if (retSubject == null) {
            logger.warn("[PIP SRV][GET_SUBJECT] Username: " + userName + " not found!");
            throw new SubjectNotFoundException();
        }
        logger.info("[PIP SRV][GET_SUBJECT] Username: " + userName + " returning " + retSubject);
        return retSubject;
    }

    public void changePassword(ChangePasswordRequest request) throws SubjectNotFoundException, WrongPasswordException {
        logger.info("[PIP SRV][CHG_PASSWORD] : " + request);
        Subject subject = subjectRepository.findByUserName(request.getUsername());

        if (subject != null) {
            if (subject.getPassword().equals(request.getOldPassword())) {
                subject.setPassword(request.getNewPassword());
                subjectRepository.save(subject);
                logger.info("[PIP SRV][CHG_PASSWORD] : " + request + " was successful!");
            } else {
                logger.warn("[PIP SRV][CHG_PASSWORD] Old password not matched: " + request.getOldPassword());
                throw new WrongPasswordException();
            }
        } else {
            logger.warn("[PIP SRV][CHG_PASSWORD] Username not found: " + request.getUsername());
            throw new SubjectNotFoundException();
        }
    }

    /**
     * Method for registering a new subject in the subject repository
     *
     * @param addUserReq the subject to be put in to the subject repository
     * @return true, if succeeded
     * @throws BadRequestException if Username taken or empty
     */
    public Subject registerSubject(AddUserRequest addUserReq) throws BadRequestException, SubjectAlreadyExistsException {
        logger.info("[PIP SRV][REGISTER] : " + addUserReq);
        if (addUserReq.getUserName() == null || addUserReq.getUserName().equals("")) {
            throw new BadRequestException("Username must not be empty!");
        }

        Subject checkSubject = subjectRepository.findByUserName(addUserReq.getUserName());

        if (checkSubject != null) {
            logger.warn("[PIP SRV][REGISTER] Username already exists: " + addUserReq.getUserName());
            throw new SubjectAlreadyExistsException();
        }

        String userProxyId = FACAUtilFunctions.generateRandomString(FACAProjectConstants.USER_PROXY_ID_LENGTH);
        Subject addSubject = new Subject(addUserReq.getUserName(), addUserReq.getPassword(), addUserReq.getName(), addUserReq.getBirthdate(), userProxyId);

        subjectRepository.save(addSubject);
        logger.info("[PIP SRV][REGISTER] : " + addSubject + " registered!");
        return addSubject;
    }


    public Subject addProfession(Profession request, String tokenString) throws SubjectNotFoundException, UnauthorizedAccessException, UnallowedActionException, BadRequestException {
        logger.info("[PIP SRV][ADD_PROFESSION] Token : " + tokenString + " Request " + request);

        AuthorizationRequestPIP authzReq = new AuthorizationRequestPIP(FACAProjectConstants.PIP_REQUEST_TYPE.PIP_ADD_PROFESSION);

        pdpService.requestPIP(authzReq, tokenString);
        logger.info("[PIP SRV][ADD_PROFESSION] Token : " + tokenString + " authorized!");
        Subject subject = getSubject(jwsHandler.getUsernameFromJWS(tokenString));
        //Subject must exist
        if (subject == null) {
            throw new SubjectNotFoundException();
        }

        //add profession
        subject.addProfession(request.getOrganization(), request.getPosition());
        subjectRepository.save(subject);

        logger.info("[PIP SRV][ADD_PROFESSION] " + request + " successfully added!");

        return subject;
    }

    public Subject removeProfession(Profession request, String tokenString) throws SubjectNotFoundException, UnauthorizedAccessException, UnallowedActionException, BadRequestException {
        logger.info("[PIP SRV][REMOVE_PROFESSION] Token : " + tokenString + " Request " + request);

        AuthorizationRequestPIP authzReq = new AuthorizationRequestPIP(FACAProjectConstants.PIP_REQUEST_TYPE.PIP_REMOVE_PROFESSION);
        //send Request to PDP
        pdpService.requestPIP(authzReq, tokenString);
        logger.info("[PIP SRV][REMOVE_PROFESSION] Token : " + tokenString + " authorized!");
        Subject subject = getSubject(jwsHandler.getUsernameFromJWS(tokenString));
        //Subject must exist
        if (subject == null) {
            throw new SubjectNotFoundException();
        }

        //remove Profession
        subject.removeProfession(request.getOrganization(), request.getPosition());

        subjectRepository.save(subject);

        logger.info("[PIP SRV][REMOVE_PROFESSION] " + request + " successfully removed!");

        return subject;
    }

    public Subject addHandicap(Handicap request, String tokenString) throws UnauthorizedAccessException, SubjectNotFoundException, BadRequestException, UnallowedActionException {
        logger.info("[PIP SRV][ADD_HANDICAP] Token : " + tokenString + " Request " + request);

        AuthorizationRequestPIP authzReq = new AuthorizationRequestPIP(FACAProjectConstants.PIP_REQUEST_TYPE.PIP_ADD_HANDICAP);

        pdpService.requestPIP(authzReq, tokenString);
        logger.info("[PIP SRV][ADD_HANDICAP] Token : " + tokenString + " authorized!");
        Subject subject = getSubject(jwsHandler.getUsernameFromJWS(tokenString));
        //Subject must exist
        if (subject == null) {
            throw new SubjectNotFoundException();
        }

        if (subject.getHandicaps().contains(request.getHandicap())) {
            throw new BadRequestException("Duplicate Handicaps detected!");
        }
        //add handicap
        subject.addHandicap(request.getHandicap());

        subjectRepository.save(subject);
        logger.info("[PIP SRV][ADD_HANDICAP] " + request + " successfully added!");
        return subject;
    }

    public Subject removeHandicap(Handicap request, String tokenString) throws UnauthorizedAccessException, SubjectNotFoundException, UnallowedActionException, BadRequestException {
        logger.info("[PIP SRV][REMOVE_HANDICAP] Token : " + tokenString + " Request " + request);

        AuthorizationRequestPIP authzReq = new AuthorizationRequestPIP(FACAProjectConstants.PIP_REQUEST_TYPE.PIP_REMOVE_HANDICAP);

        pdpService.requestPIP(authzReq, tokenString);
        logger.info("[PIP SRV][REMOVE_HANDICAP] Token : " + tokenString + " authorized!");
        Subject subject = getSubject(jwsHandler.getUsernameFromJWS(tokenString));
        //Subject must exist
        if (subject == null) {
            throw new SubjectNotFoundException();
        }

        //remove handicap
        subject.removeHandicap(request.getHandicap());
        subjectRepository.save(subject);

        logger.info("[PIP SRV][REMOVE_HANDICAP] " + request + " successfully removed!");

        return subject;
    }

    public SubjectSecured updateRole(UpdateRoleRequest request, String tokenString) throws BadRequestException {
        logger.info("[PIP SRV][UPDATE_ROLE] subjectId: " + request.getId() + "new Role: " + request.getRole());
        try {

            Role role = new Role(FACAProjectConstants.Role.valueOf(request.getRole()));
            Subject subject = subjectRepository.findById(request.getId()).orElse(null);
            if (subject == null) {
                throw new SubjectNotFoundException();
            }
            subject.setRole(role);
            subjectRepository.save(subject);
            logger.info("[PIP SRV][UPDATE_ROLE] success! subject: " + subject.getUserName() + "new Role: " + subject.getRole().getRoleName());
            return new SubjectSecured(subject.toDTO());
        } catch (Exception e) {
            logger.error("[PIP_SRV][UPDATE_ROLE] failure! exception: " + e.getMessage());
            throw new BadRequestException();
        }
    }

    public SubjectDTO updateSubjectActiveFlag(String subjectId, boolean isActive) throws SubjectNotFoundException {
        logger.info("[PIP SERV][UPDATE_SUBJECT_ACTIVE] id: {} isActive: {}", subjectId, isActive);
        Subject subject = subjectRepository.findById(subjectId).orElse(null);

        AuthorizationRequestPIP authzReq = new AuthorizationRequestPIP(FACAProjectConstants.PIP_REQUEST_TYPE.PIP_UPDATE_ACTIVE_FLAG);

        if (subject == null) {
            throw new SubjectNotFoundException();
        }

        subject.setActive(isActive);

        if (isActive) {
            subject.setPasswordFails(0);
        }
        subjectRepository.save(subject);
        return subject.toDTO();
    }

    public List<SubjectSecured> getSubjectList(String tokenString) {

        List<Subject> subjectList = subjectRepository.findAll();
        List<SubjectSecured> retList = new ArrayList<>();
        for (Subject sub : subjectList) {
            retList.add(new SubjectSecured(sub.toDTO()));
        }
        return retList;
    }

    public SubjectManagementConfiguration getSubjectManagementConfiguration(String tokenString) {


        HashMap<String, Integer> rolesMap = new HashMap<>();
        for (FACAProjectConstants.Role role : FACAProjectConstants.Role.values()) {
            rolesMap.put(role.name(), role.ordinal());
        }

        return new SubjectManagementConfiguration(rolesMap);
    }

    public List<DeviceTypeDTO> listAllDeviceTypes() {
        logger.info("[PIP SRV][LIST_DEVICE_TYPES] Device types number");
        return deviceTypeRepository.findAll().stream().map(DeviceType::toDTO).collect(Collectors.toList());
    }

    /**
     * Private class methods
     */

    private void updatePasswordFails(Subject sub, boolean failed) {
        if (failed) {
            sub.setPasswordFails(sub.getPasswordFails() + 1);
            if (sub.getPasswordFails() > FACAProjectConstants.ALLOWED_PASSWORD_FAILS) {
                sub.setActive(false);
            }
            subjectRepository.save(sub);
        } else {
            if (sub.getPasswordFails() > 0) {
                sub.setPasswordFails(0);
                subjectRepository.save(sub);
            }
        }
    }

    private String generateSessionToken(Subject subject) {
        return jwsHandler.generateJWSToken(subject);
    }

}
