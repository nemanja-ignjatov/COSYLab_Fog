package at.cosylab.fog.faca.amqp;

import context.amqp.ContextAMQPRoutingConstants;
import fog.amqp_utils.routes.FACARoutesConstants;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AMQPConfiguration {

    private final String exchangeName = "abac.rpc";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public DirectExchange exchangeRpc() {
        return new DirectExchange(exchangeName);
    }

    /**
     * PAP QUEUES AND CONFIG
     */

    @Bean
    public Queue papGetPolicySet() {
        return new Queue("abac.rpc.pap.get_policy_set");
    }

    @Bean
    public Queue papAddRule() {
        return new Queue("abac.rpc.pap.add_rule");
    }

    @Bean
    public Queue papDeleteRule() {
        return new Queue("abac.rpc.pap.delete_rule");
    }

    @Bean
    public Queue papGetAttributesConfiguration() {
        return new Queue("abac.rpc.pap.get_attributes_configuration");
    }

    @Bean
    public Queue papUpdateDeviceTypesConfiguration() {
        return new Queue("abac.rpc.pap.update_device_types_configuration");
    }

    @Bean
    public Queue papRegisterCAAttributes() {
        return new Queue(ContextAMQPRoutingConstants.FACA_REGISTER_ATTRIBUTES);
    }

    @Bean
    public Queue papUpdateCAAttributes() {
        return new Queue(ContextAMQPRoutingConstants.FACA_UPDATE_ATTRIBUTES);
    }

    @Bean
    public Queue papNotifyCAAttributeValueChange() {
        return new Queue(ContextAMQPRoutingConstants.FACA_NOTIFY_ATTRIBUTE_VALUE_CHANGE);
    }

    @Bean
    public PolicyAdministrationPointAMQPServer papServer() {
        return new PolicyAdministrationPointAMQPServer();
    }


    /**
     * PEP QUEUES AND CONFIG
     */

    @Bean
    public Queue pepQueueAuthorize() {
        return new Queue("abac.rpc.pep.authorize");
    }

    @Bean
    public Queue pepQueueAuthorizeDeviceManagement() {
        return new Queue("abac.rpc.pep.authorize_device_management");
    }

    @Bean
    public Queue pepQueueAuthorizeDevice() {
        return new Queue("abac.rpc.pep.authorize_devices_visibility");
    }

    @Bean
    public Queue pepQueueAuthorizeDevicesAccess() {
        return new Queue("abac.rpc.pep.authorize_devices_access");
    }

    @Bean
    public PolicyEnforcementPointAMQPServer pepServer() {
        return new PolicyEnforcementPointAMQPServer();
    }

    /**
     * PIP QUEUES AND CONFIG
     */
    @Bean
    public Queue pipQueueRegister() {
        return new Queue("abac.rpc.pip.register");
    }

    @Bean
    public Queue pipQueueChangePassword() {
        return new Queue("abac.rpc.pip.change_password");
    }

    @Bean
    public Queue pipQueueAddProfession() {
        return new Queue("abac.rpc.pip.add_profession");
    }

    @Bean
    public Queue pipQueueRemoveProfession() {
        return new Queue("abac.rpc.pip.remove_profession");
    }

    @Bean
    public Queue pipQueueAddHandicap() {
        return new Queue("abac.rpc.pip.add_handicap");
    }

    @Bean
    public Queue pipQueueRemoveHandicap() {
        return new Queue("abac.rpc.pip.remove_handicap");
    }

    @Bean
    public Queue pipQueueLoginExtended() {
        return new Queue("abac.rpc.pip.login_extended");
    }

    @Bean
    public Queue pipQueueLogout() {
        return new Queue("abac.rpc.pip.logout");
    }

    @Bean
    public Queue pipQueueUpdateUserActiveFlag() {
        return new Queue("abac.rpc.pip.update_user_active_flag");
    }

    @Bean
    public Queue pipQueueGetUserData() {
        return new Queue("abac.rpc.pip.get_user_data");
    }

    @Bean
    public Queue pipQueueListAllDeviceTypes() {
        return new Queue(FACARoutesConstants.PIP_LIST_DEVICE_TYPES);
    }

    @Bean
    public Queue pipQueueUpdateAccountRole() {
        return new Queue("abac.rpc.pip.update_account_role");
    }

    @Bean
    public Queue pipQueueGetSubjectList() {
        return new Queue("abac.rpc.pip.get_subject_list");
    }

    @Bean
    public Queue pipQueueGetSubjectManagementConfiguration() {
        return new Queue("abac.rpc.pip.get_subject_management_configuration");
    }

    @Bean
    public Queue pipQueueGetUserProxyMap() {
        return new Queue("abac.rpc.pip.get_user_proxy_map");
    }

    @Bean
    public PolicyInformationPointAMQPServer pipServer() {
        return new PolicyInformationPointAMQPServer();
    }
}
