/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.shim.ext.sbe.impl;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.protos.common.MspPrincipal;
import org.hyperledger.fabric.protos.common.Policies;
import org.hyperledger.fabric.shim.ext.sbe.StateBasedEndorsement;

import java.util.*;

public class StateBasedEndorsementImpl implements StateBasedEndorsement {
    private static Log logger = LogFactory.getLog(StateBasedEndorsementImpl.class);

    private Map<String, MspPrincipal.MSPRole.MSPRoleType> orgs = new HashMap<>();

    StateBasedEndorsementImpl(byte[] ep) {
        if (ep == null) {
            ep = new byte[]{};
        }
        try {
            Policies.SignaturePolicyEnvelope spe = Policies.SignaturePolicyEnvelope.parseFrom(ep);
            setMSPIDsFromSP(spe);
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalArgumentException("error unmarshaling endorsement policy bytes", e);
        }

    }

    @Override
    public byte[] policy() {
        Policies.SignaturePolicyEnvelope spe = policyFromMSPIDs();
        ByteString speBytes = spe.toByteString();
        return speBytes.toByteArray();
    }

    @Override
    public void addOrgs(RoleType role, String... organizations) {
        MspPrincipal.MSPRole.MSPRoleType mspRole;
        if (RoleType.RoleTypeMember.equals(role)) {
            mspRole = MspPrincipal.MSPRole.MSPRoleType.MEMBER;
        } else {
            mspRole = MspPrincipal.MSPRole.MSPRoleType.PEER;
        }
        for (String neworg : organizations) {
            orgs.put(neworg, mspRole);
        }
    }

    @Override
    public void delOrgs(String... organizations) {
        for (String delorg : organizations) {
            orgs.remove(delorg);
        }
    }

    @Override
    public List<String> listOrgs() {
        List<String> res = new ArrayList<>();
        orgs.forEach((k, v) -> {
            res.add(k);

        });
        return res;
    }

    private void setMSPIDsFromSP(Policies.SignaturePolicyEnvelope spe) {
        spe.getIdentitiesList().forEach(identity -> {
            if (MspPrincipal.MSPPrincipal.Classification.ROLE.equals(identity.getPrincipalClassification())) {
                try {
                    MspPrincipal.MSPRole mspRole = MspPrincipal.MSPRole.parseFrom(identity.getPrincipal());
                    orgs.put(mspRole.getMspIdentifier(), mspRole.getRole());
                } catch (InvalidProtocolBufferException e) {
                    logger.warn("error unmarshaling msp principal");
                    throw new IllegalArgumentException("error unmarshaling msp principal", e);
                }
            }
        });
    }


    private Policies.SignaturePolicyEnvelope policyFromMSPIDs() {
        List<String> mspids = listOrgs();

        mspids.sort(Comparator.naturalOrder());
        List<MspPrincipal.MSPPrincipal> principals = new ArrayList<>();
        List<Policies.SignaturePolicy> sigpolicy = new ArrayList<>();
        for (int i = 0; i < mspids.size(); i++) {
            String mspid = mspids.get(i);
            principals.add(MspPrincipal.MSPPrincipal
                    .newBuilder()
                    .setPrincipalClassification(MspPrincipal.MSPPrincipal.Classification.ROLE)
                    .setPrincipal(MspPrincipal.MSPRole
                            .newBuilder()
                            .setMspIdentifier(mspid)
                            .setRole(orgs.get(mspid))
                            .build().toByteString())
                    .build());

            sigpolicy.add(StateBasedEndorsementUtils.signedBy(i));
        }

        // create the policy: it requires exactly 1 signature from all of the principals
        return Policies.SignaturePolicyEnvelope
                .newBuilder()
                .setVersion(0)
                .setRule(StateBasedEndorsementUtils.nOutOf(mspids.size(), sigpolicy))
                .addAllIdentities(principals)
                .build();
    }



}
