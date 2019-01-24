/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.contact.execution;

import java.util.List;

public interface InvocationRequest {
    String DEFAULT_NAMESPACE = "default";

    String getNamespace();
    String getMethod();
    List<byte[]> getArgs();
    String getRequestName();
}
