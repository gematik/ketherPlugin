// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract ScumaContract {

    address Owner;

    address[] protectionAuthorizationIds;
    mapping(address => uint) protectionAuthorizationIdIndices;
    Policy[] policies;
    Rule[][] ruleLists;
    mapping(uint256 => uint) policyIndices; // protectedResourceId to policy index

    struct Policy {
        uint256 what;  // protected resource id
        Rule[] ruleList;
    }

    struct Rule {
        address who;  // userId
        AccessMethod how;
    }

    enum AccessMethod {Create, Read, Update, Delete}

    struct Permission {
        uint256 protectedResourceId;
        AccessMethod grantedMethod;
    }

    struct PermissionRequest {
        uint256 protectedResourceId;
        AccessMethod requestedMethod;
    }

    constructor () {
        Owner = msg.sender;
        protectionAuthorizationIds.push();
        policies.push();
    }

    modifier onlyOwner(){
        require(msg.sender == Owner, 'Not owner');
        _;
    }

    modifier onlyAuthorizedProviders(){
        require(protectionAuthorizationIdIndices[msg.sender] > 0, 'Provider not authorized');
        _;
    }

    function registerProvider(address protectionAuthorizationId) public onlyOwner {
        protectionAuthorizationIds.push(protectionAuthorizationId);
        protectionAuthorizationIdIndices[protectionAuthorizationId] = protectionAuthorizationIds.length - 1;
    }

    function unregisterProvider(address protectionAuthorizationId) public onlyOwner {
        uint index = protectionAuthorizationIdIndices[protectionAuthorizationId];
        if (index > 0) {
            protectionAuthorizationIds[index] = protectionAuthorizationIds[protectionAuthorizationIds.length - 1];
            protectionAuthorizationIds.pop();
        }
    }

    function unregisterAllProviders() public onlyOwner {
        for (uint i = 0; i < protectionAuthorizationIds.length - 1; i++) {
            delete protectionAuthorizationIdIndices[protectionAuthorizationIds[i]];
            protectionAuthorizationIds.pop();
        }
    }

    function registerResource(uint256 protectedResourceId) public onlyAuthorizedProviders {
        ruleLists.push();
        policies.push();
        policies[policies.length - 1].what = protectedResourceId;
        policies[policies.length - 1].ruleList = ruleLists[ruleLists.length - 1];
        policyIndices[protectedResourceId] = policies.length - 1;
    }

    function unregisterResource(uint256 protectedResourceId) public {
        uint policyIndex = policyIndices[protectedResourceId];
        if (policyIndex > 0) {// policy exists
            policies[policyIndex] = policies[policies.length - 1];
            policies.pop();
            delete policyIndices[protectedResourceId];
        }
    }

    function unregisterAllResources() public onlyOwner {
        for (uint i = policies.length - 1; i < 0; i--) {
            delete policyIndices[policies[i].what];
            policies.pop();
        }
    }


    function setRule(uint256 protectedResourceId, address userId, AccessMethod method) public onlyOwner {
        uint index = policyIndices[protectedResourceId];
        if (index > 0) {// protected resource id exists
            policies[index].ruleList.push(Rule(userId, method));
        }
    }

    function deleteRule(uint256 protectedResourceId, uint index) public {
        uint policyIndex = policyIndices[protectedResourceId];
        require(policyIndex > 0, 'protected resource does not exist');
        Policy storage policy = policies[policyIndex];
        require(index < policy.ruleList.length, 'rule does not exist');
        policy.ruleList[index] = policy.ruleList[policy.ruleList.length - 1];
        policy.ruleList.pop();
    }

    function requestPermissions(address userId, PermissionRequest[] calldata permissionRequests) view external returns (Permission[] memory){
        // first count permissions
        uint count = 0;
        for (uint i = 0; i < permissionRequests.length; i++) {
            PermissionRequest memory permissionRequest = permissionRequests[i];
            uint policyIndex = policyIndices[permissionRequest.protectedResourceId];
            if (policyIndex > 0) {
                Policy storage policy = policies[policyIndex];
                for (uint j = 0; j < policy.ruleList.length; j++) {
                    Rule memory rule = policy.ruleList[j];
                    if (rule.who == userId && rule.how == permissionRequest.requestedMethod) {
                        count++;
                    }
                }
            }
        }
        // allocate array and fill
        Permission[] memory permissions = new Permission[](count);
        count = 0;
        for (uint i = 0; i < permissionRequests.length; i++) {
            PermissionRequest memory permissionRequest = permissionRequests[i];
            uint policyIndex = policyIndices[permissionRequest.protectedResourceId];
            if (policyIndex > 0) {
                Policy storage policy = policies[policyIndex];
                for (uint j = 0; j < policy.ruleList.length; j++) {
                    Rule memory rule = policy.ruleList[j];
                    if (rule.who == userId && rule.how == permissionRequest.requestedMethod) {
                        permissions[count++] = Permission(permissionRequest.protectedResourceId, rule.how);
                    }
                }
            }
        }
        return permissions;
    }
}