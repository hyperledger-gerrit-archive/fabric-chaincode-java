/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.shim.ext.sbe.impl;

import org.hyperledger.fabric.protos.common.MspPrincipal;
import org.hyperledger.fabric.protos.common.Policies;

import java.util.Arrays;
import java.util.List;

public class StateBasedEndorsementUtils {
    static Policies.SignaturePolicy signedBy(int index){
        return Policies.SignaturePolicy.newBuilder()
                .setSignedBy(index).build();
    }

    static Policies.SignaturePolicy nOutOf(int n, List<Policies.SignaturePolicy> policies) {
        return Policies.SignaturePolicy
                .newBuilder()
                .setNOutOf(Policies.SignaturePolicy.NOutOf
                        .newBuilder()
                        .setN(n)
                        .addAllRules(policies)
                        .build())
                .build();
    }

    static Policies.SignaturePolicyEnvelope signedByFabricEntity(String mspId, MspPrincipal.MSPRole.MSPRoleType role) {
        // specify the principal: it's a member of the msp we just found
        MspPrincipal.MSPPrincipal principal = MspPrincipal.MSPPrincipal
                .newBuilder()
                .setPrincipalClassification(MspPrincipal.MSPPrincipal.Classification.ROLE)
                .setPrincipal(MspPrincipal.MSPRole
                        .newBuilder()
                        .setMspIdentifier(mspId)
                        .setRole(role)
                        .build().toByteString())
                .build();

        // create the policy: it requires exactly 1 signature from the first (and only) principal
        return Policies.SignaturePolicyEnvelope
                .newBuilder()
                .setVersion(0)
                .setRule(nOutOf(1, Arrays.asList(signedBy(0))))
                .addIdentities(principal)
                .build();

    }


}
