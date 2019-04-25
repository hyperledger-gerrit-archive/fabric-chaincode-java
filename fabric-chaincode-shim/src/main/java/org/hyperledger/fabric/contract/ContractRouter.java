/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.contract;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.contract.execution.ExecutionFactory;
import org.hyperledger.fabric.contract.execution.ExecutionService;
import org.hyperledger.fabric.contract.execution.InvocationRequest;
import org.hyperledger.fabric.contract.metadata.MetadataBuilder;
import org.hyperledger.fabric.contract.routing.ContractScanner;
import org.hyperledger.fabric.contract.routing.Routing;
import org.hyperledger.fabric.contract.routing.TransactionType;
import org.hyperledger.fabric.contract.routing.impl.ContractScannerImpl;
import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ResponseUtils;

/**
 * Router class routes Init/Invoke requests to contracts.
 * Implements {@link org.hyperledger.fabric.shim.Chaincode} interface.
 */
public class ContractRouter extends ChaincodeBase {
    private static Log logger = LogFactory.getLog(ContractRouter.class);
    private ContractScanner scanner;
    private ExecutionService executor;

    public ContractRouter() {
        scanner = new ContractScannerImpl();
        executor = ExecutionFactory.getInstance().createExecutionService();
    }

    void findAllContracts() {
        try {
            scanner.findAndSetContracts();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    void startRouting(String[] args) {
        start(args);
    }

    @Override
    public Response init(ChaincodeStub stub) {
        InvocationRequest request = ExecutionFactory.getInstance().createRequest(stub);
        Routing routing = getRouting(request);
        if (routing != null) {
            if (routing.getType() == TransactionType.INIT || routing.getType() == TransactionType.DEFAULT) {
                return executor.executeRequest(routing, request, stub);
            }
        }
        return ResponseUtils.newErrorResponse("Can't find @Init method " + request.getMethod() + " in namespace " + request.getNamespace() + " and no default method as well");
    }

    @Override
    public Response invoke(ChaincodeStub stub) {
        InvocationRequest request = ExecutionFactory.getInstance().createRequest(stub);
        Routing routing = getRouting(request);
        if (routing != null) {
            if (routing.getType() == TransactionType.INVOKE || routing.getType() == TransactionType.QUERY || routing.getType() == TransactionType.DEFAULT) {
                return executor.executeRequest(routing, request, stub);
            }
        }
        return ResponseUtils.newErrorResponse("Can't find @Transaction method " + request.getMethod() + " in namespace " + request.getNamespace() + " and no default method as well");
    }

    public static void main(String[] args) {
        loggingSetup();
        ContractRouter cfc = new ContractRouter();
        cfc.findAllContracts();

        // commence routing, once this has returned the chaincode and contract api is 'open for business'
        cfc.startRouting(args);

        String jsonmetadata = MetadataBuilder.getMetadata();
        logger.info("Metadata follows:");
        logger.info(MetadataBuilder.debugString());
        logger.info("----------------------------------");
    }

    private static void loggingSetup(){
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tH:%1$tM:%1$tS:%1$tL %4$-7.7s %2$s %5$s%6$s%n");
        final Logger rootLogger = Logger.getLogger("");
        for (java.util.logging.Handler handler : rootLogger.getHandlers()) {
            handler.setLevel(Level.ALL);
             handler.setFormatter(new SimpleFormatter() {
                 @Override
                 public synchronized String format(LogRecord record) {
                     return super.format(record)
                             .replaceFirst(".*SEVERE\\s*\\S*\\s*\\S*", "\u001B[1;31m$0\u001B[0m")
                             .replaceFirst(".*WARNING\\s*\\S*\\s*\\S*", "\u001B[1;33m$0\u001B[0m")
                             .replaceFirst(".*CONFIG\\s*\\S*\\s*\\S*", "\u001B[35m$0\u001B[0m")
                             .replaceFirst(".*FINE\\s*\\S*\\s*\\S*", "\u001B[36m$0\u001B[0m")
                             .replaceFirst(".*FINER\\s*\\S*\\s*\\S*", "\u001B[36m$0\u001B[0m")
                             .replaceFirst(".*FINEST\\s*\\S*\\s*\\S*", "\u001B[36m$0\u001B[0m");
                 }
             });
        }
    }

    Routing getRouting(InvocationRequest request) {
        Routing routing = scanner.getRouting(request);
        if (routing == null) {
            routing = scanner.getDefaultRouting(request);
        }
        return routing;
    }
}
