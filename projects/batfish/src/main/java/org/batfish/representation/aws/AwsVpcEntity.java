package org.batfish.representation.aws;

/** An interface implemented by all AWS entities */
@SuppressWarnings("JavaDoc") // all constants below create a warning
public interface AwsVpcEntity {

  String JSON_KEY_ACCEPTED_ROUTE_COUNT = "AcceptedRouteCount";
  String JSON_KEY_ACCEPTER_VPC_INFO = "AccepterVpcInfo";
  String JSON_KEY_ADDRESSES = "Addresses";
  String JSON_KEY_ALLOCATION_ID = "AllocationId";
  String JSON_KEY_AMAZON_SIDE_ASN = "AmazonSideAsn";
  String JSON_KEY_ASSOCIATION = "Association";
  String JSON_KEY_ASSOCIATION_DEFAULT_ROUTE_TABLE_ID = "AssociationDefaultRouteTableId";
  String JSON_KEY_ASSOCIATIONS = "Associations";
  String JSON_KEY_ATTACHMENT = "Attachment";
  String JSON_KEY_ATTACHMENTS = "Attachments";
  String JSON_KEY_ATTRIBUTES = "Attributes";
  String JSON_KEY_AVAILABILITY_ZONE = "AvailabilityZone";
  String JSON_KEY_AVAILABILITY_ZONES = "AvailabilityZones";
  String JSON_KEY_BGP_ASN = "BgpAsn";
  String JSON_KEY_CIDR_BLOCK = "CidrBlock";
  String JSON_KEY_CIDR_BLOCK_ASSOCIATION_SET = "CidrBlockAssociationSet";
  String JSON_KEY_CIDR_BLOCK_SET = "CidrBlockSet";
  String JSON_KEY_CIDR_IP = "CidrIp";
  String JSON_KEY_CIDRS = "Cidrs";
  String JSON_KEY_CODE = "Code";
  String JSON_KEY_CREATED = "Created";
  String JSON_KEY_CUSTOMER_GATEWAY_CONFIGURATION = "CustomerGatewayConfiguration";
  String JSON_KEY_CUSTOMER_GATEWAY_ID = "CustomerGatewayId";
  String JSON_KEY_CUSTOMER_GATEWAYS = "CustomerGateways";
  String JSON_KEY_DB_INSTANCES = "DBInstances";
  String JSON_KEY_DB_INSTANCE_IDENTIFIER = "DBInstanceIdentifier";
  String JSON_KEY_DB_INSTANCE_STATUS = "DBInstanceStatus";
  String JSON_KEY_DB_SUBNET_GROUP = "DBSubnetGroup";
  String JSON_KEY_DEFAULT_ACTIONS = "DefaultActions";
  String JSON_KEY_DEFAULT_ASSOCIATION_ROUTE_TABLE = "DefaultAssociationRouteTable";
  String JSON_KEY_DEFAULT_PROPAGATION_ROUTE_TABLE = "DefaultPropagationRouteTable";
  String JSON_KEY_DEFAULT_ROUTE_TABLE_ASSOCIATION = "DefaultRouteTableAssociation";
  String JSON_KEY_DEFAULT_ROUTE_TABLE_PROPAGATION = "DefaultRouteTablePropagation";
  String JSON_KEY_DELETED = "Deleted";
  String JSON_KEY_DESCRIPTION = "Description";
  String JSON_KEY_DESTINATION_CIDR_BLOCK = "DestinationCidrBlock";
  String JSON_KEY_DESTINATION_IPV6_CIDR_BLOCK = "DestinationIpv6CidrBlock";
  String JSON_KEY_DHCP_OPTIONS = "DhcpOptions";
  String JSON_KEY_DNS_NAME = "DNSName";
  String JSON_KEY_DOMAIN_NAME = "DomainName";
  String JSON_KEY_DOMAIN_STATUS_LIST = "DomainStatusList";
  String JSON_KEY_EGRESS = "Egress";
  String JSON_KEY_ENTRIES = "Entries";
  String JSON_KEY_FROM = "From";
  String JSON_KEY_FROM_PORT = "FromPort";
  String JSON_KEY_GATEWAY_ID = "GatewayId";
  String JSON_KEY_GROUP_ID = "GroupId";
  String JSON_KEY_GROUP_NAME = "GroupName";
  String JSON_KEY_GROUPS = "Groups";
  String JSON_KEY_ICMP_TYPE_CODE = "IcmpTypeCode";
  String JSON_KEY_ID = "Id";
  String JSON_KEY_INSTANCE_ID = "InstanceId";
  String JSON_KEY_INSTANCE_STATUSES = "InstanceStatuses";
  String JSON_KEY_INSTANCES = "Instances";
  String JSON_KEY_INTERNET_GATEWAY_ID = "InternetGatewayId";
  String JSON_KEY_INTERNET_GATEWAYS = "InternetGateways";
  String JSON_KEY_IP_ADDRESS = "IpAddress";
  String JSON_KEY_IP_PERMISSIONS = "IpPermissions";
  String JSON_KEY_IP_PERMISSIONS_EGRESS = "IpPermissionsEgress";
  String JSON_KEY_IP_PROTOCOL = "IpProtocol";
  String JSON_KEY_IP_RANGES = "IpRanges";
  String JSON_KEY_IPV6_CIDR_BLOCK = "Ipv6CidrBlock";
  String JSON_KEY_IS_DEFAULT = "IsDefault";
  String JSON_KEY_KEY = "Key";
  String JSON_KEY_LISTENER_ARN = "ListenerArn";
  String JSON_KEY_LISTENERS = "Listeners";
  String JSON_KEY_LOAD_BALANCER_ARN = "LoadBalancerArn";
  String JSON_KEY_LOAD_BALANCER_ARNS = "LoadBalancerArns";
  String JSON_KEY_LOAD_BALANCER_ATTRIBUTES = "LoadBalancerAttributes";
  String JSON_KEY_LOAD_BALANCER_LISTENERS = "LoadBalancerListeners";
  String JSON_KEY_LOAD_BALANCER_NAME = "LoadBalancerName";
  String JSON_KEY_LOAD_BALANCER_TARGET_HEALTH = "LoadBalancerTargetHealth";
  String JSON_KEY_LOAD_BALANCERS = "LoadBalancers";
  String JSON_KEY_MAIN = "Main";
  String JSON_KEY_MULTI_AZ = "MultiAZ";
  String JSON_KEY_NAME = "Name";
  String JSON_KEY_NAT_GATEWAY_ADDRESSES = "NatGatewayAddresses";
  String JSON_KEY_NAT_GATEWAY_ID = "NatGatewayId";
  String JSON_KEY_NAT_GATEWAYS = "NatGateways";
  String JSON_KEY_NETWORK_ACL_ID = "NetworkAclId";
  String JSON_KEY_NETWORK_ACLS = "NetworkAcls";
  String JSON_KEY_NETWORK_INTERFACE_ID = "NetworkInterfaceId";
  String JSON_KEY_NETWORK_INTERFACES = "NetworkInterfaces";
  String JSON_KEY_OPTIONS = "Options";
  String JSON_KEY_ORDER = "Order";
  String JSON_KEY_OUTSIDE_IP_ADDRESS = "OutsideIpAddress";
  String JSON_KEY_OWNER_ID = "OwnerId";
  String JSON_KEY_PLACEMENT = "Placement";
  String JSON_KEY_PLACEMENT_GROUPS = "PlacementGroups";
  String JSON_KEY_PORT = "Port";
  String JSON_KEY_PORT_RANGE = "PortRange";
  String JSON_KEY_PREFIX_LIST_ID = "PrefixListId";
  String JSON_KEY_PREFIX_LIST_IDS = "PrefixListIds";
  String JSON_KEY_PREFIX_LIST_NAME = "PrefixListName";
  String JSON_KEY_PREFIX_LISTS = "PrefixLists";
  String JSON_KEY_PRIMARY = "Primary";
  String JSON_KEY_PRIVATE_IP_ADDRESS = "PrivateIpAddress";
  String JSON_KEY_PRIVATE_IP_ADDRESSES = "PrivateIpAddresses";
  String JSON_KEY_PROPAGATION_DEFAULT_ROUTE_TABLE_ID = "PropagationDefaultRouteTableId";
  String JSON_KEY_PROTOCOL = "Protocol";
  String JSON_KEY_PRIVATE_IP = "PrivateIp";
  String JSON_KEY_PUBLIC_IP = "PublicIp";
  String JSON_KEY_REASON = "Reason";
  String JSON_KEY_REGIONS = "Regions";
  String JSON_KEY_RESOURCE_ID = "ResourceId";
  String JSON_KEY_RESOURCE_TYPE = "ResourceType";
  String JSON_KEY_REQUESTER_VPC_INFO = "RequesterVpcInfo";
  String JSON_KEY_RESERVATIONS = "Reservations";
  String JSON_KEY_ROUTE_TABLE_ID = "RouteTableId";
  String JSON_KEY_ROUTE_TABLES = "RouteTables";
  String JSON_KEY_ROUTES = "Routes";
  String JSON_KEY_RULE_ACTION = "RuleAction";
  String JSON_KEY_RULE_NUMBER = "RuleNumber";
  String JSON_KEY_SCHEME = "Scheme";
  String JSON_KEY_SECURITY_GROUPS = "SecurityGroups";
  String JSON_KEY_SECURITY_GROUP_IDS = "SecurityGroupIds";
  String JSON_KEY_SERVICE_NAMES = "ServiceNames";
  String JSON_KEY_STATE = "State";
  String JSON_KEY_STATIC_ROUTES_ONLY = "StaticRoutesOnly";
  String JSON_KEY_STATUS = "Status";
  String JSON_KEY_STATUS_MESSAGE = "StatusMessage";
  String JSON_KEY_SUBNET_AVAILABILITY_ZONE = "SubnetAvailabilityZone";
  String JSON_KEY_SUBNET_ID = "SubnetId";
  String JSON_KEY_SUBNET_IDS = "SubnetIds";
  String JSON_KEY_SUBNET_IDENTIFIER = "SubnetIdentifier";
  String JSON_KEY_SUBNET_STATUS = "SubnetStatus";
  String JSON_KEY_SUBNETS = "Subnets";
  String JSON_KEY_TAGS = "Tags";
  String JSON_KEY_TARGET = "Target";
  String JSON_KEY_TARGET_HEALTH = "TargetHealth";
  String JSON_KEY_TARGET_HEALTH_DESCRIPTIONS = "TargetHealthDescriptions";
  String JSON_KEY_TARGET_GROUP_ARN = "TargetGroupArn";
  String JSON_KEY_TARGET_GROUP_NAME = "TargetGroupName";
  String JSON_KEY_TARGET_GROUPS = "TargetGroups";
  String JSON_KEY_TARGET_TYPE = "TargetType";
  String JSON_KEY_TO = "To";
  String JSON_KEY_TO_PORT = "ToPort";
  String JSON_KEY_TRANSIT_GATEWAY_ATTACHMENT_ID = "TransitGatewayAttachmentId";
  String JSON_KEY_TRANSIT_GATEWAY_ATTACHMENTS = "TransitGatewayAttachments";
  String JSON_KEY_TRANSIT_GATEWAY_PROPAGATIONS = "TransitGatewayPropagations";
  String JSON_KEY_TRANSIT_GATEWAY_ID = "TransitGatewayId";
  String JSON_KEY_TRANSIT_GATEWAY_ROUTE_TABLE_ID = "TransitGatewayRouteTableId";
  String JSON_KEY_TRANSIT_GATEWAY_ROUTE_TABLE_PROPAGATIONS = "TransitGatewayRouteTablePropagations";
  String JSON_KEY_TRANSIT_GATEWAY_ROUTE_TABLES = "TransitGatewayRouteTables";
  String JSON_KEY_TRANSIT_GATEWAY_STATIC_ROUTES = "TransitGatewayStaticRoutes";
  String JSON_KEY_TRANSIT_GATEWAY_VPC_ATTACHMENTS = "TransitGatewayVpcAttachments";
  String JSON_KEY_TRANSIT_GATEWAYS = "TransitGateways";
  String JSON_KEY_TYPE = "Type";
  String JSON_KEY_USER_GROUP_ID_PAIRS = "UserIdGroupPairs";
  String JSON_KEY_VALUE = "Value";
  String JSON_KEY_VGW_TELEMETRY = "VgwTelemetry";
  String JSON_KEY_VPC_ATTACHMENTS = "VpcAttachments";
  String JSON_KEY_VPC_ID = "VpcId";
  String JSON_KEY_ES_VPC_ID = "VPCId";
  String JSON_KEY_VPC_OPTIONS = "VPCOptions";
  String JSON_KEY_VPC_PEERING_CONNECTION_ID = "VpcPeeringConnectionId";
  String JSON_KEY_VPC_PEERING_CONNECTIONS = "VpcPeeringConnections";
  String JSON_KEY_VPC_SECURITY_GROUPS = "VpcSecurityGroups";
  String JSON_KEY_VPC_SECURITY_GROUP_ID = "VpcSecurityGroupId";
  String JSON_KEY_VPCS = "Vpcs";
  String JSON_KEY_VPN_CONNECTION_ID = "VpnConnectionId";
  String JSON_KEY_VPN_CONNECTIONS = "VpnConnections";
  String JSON_KEY_VPN_ECMP_SUPPORT = "VpnEcmpSupport";
  String JSON_KEY_VPN_GATEWAY_ID = "VpnGatewayId";
  String JSON_KEY_VPN_GATEWAYS = "VpnGateways";
  String JSON_KEY_ZONE_NAME = "ZoneName";

  String STATE_AVAILABLE = "available";
  String STATE_ASSOCIATED = "associated";
  String STATE_DELETED = "deleted";

  String STATUS_ACTIVE = "active";
  String STATUS_DELETED = "deleted";

  String TAG_NAME = "Name";

  String XML_KEY_ASN = "asn";
  String XML_KEY_AUTHENTICATION_PROTOCOL = "authentication_protocol";
  String XML_KEY_BGP = "bgp";
  String XML_KEY_CUSTOMER_GATEWAY = "customer_gateway";
  String XML_KEY_ENCRYPTION_PROTOCOL = "encryption_protocol";
  String XML_KEY_IKE = "ike";
  String XML_KEY_IP_ADDRESS = "ip_address";
  String XML_KEY_IPSEC = "ipsec";
  String XML_KEY_IPSEC_TUNNEL = "ipsec_tunnel";
  String XML_KEY_LIFETIME = "lifetime";
  String XML_KEY_MODE = "mode";
  String XML_KEY_NETWORK_CIDR = "network_cidr";
  String XML_KEY_PERFECT_FORWARD_SECRECY = "perfect_forward_secrecy";
  String XML_KEY_PRE_SHARED_KEY = "pre_shared_key";
  String XML_KEY_PROTOCOL = "protocol";
  String XML_KEY_TUNNEL_INSIDE_ADDRESS = "tunnel_inside_address";
  String XML_KEY_TUNNEL_OUTSIDE_ADDRESS = "tunnel_outside_address";
  String XML_KEY_VPN_CONNECTION = "vpn_connection";
  String XML_KEY_VPN_CONNECTION_ATTRIBUTES = "vpn_connection_attributes";
  String XML_KEY_VPN_GATEWAY = "vpn_gateway";

  String getId();
}
